/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.application.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentProposalInput;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.springframework.stereotype.Service;

/**
 * Main orchestration service behind the single Agent panel.
 *
 * @author Codex
 */
@Service
public class AgentConversationService {

    private static final TypeReference<Map<String, String>> MAP_OF_STRING_TYPE = new TypeReference<>() {
    };

    private final AgentToolRegistry agentToolRegistry;

    private final AgentLLMProvider agentLLMProvider;

    private final ObjectMapper objectMapper;

    public AgentConversationService(AgentToolRegistry agentToolRegistry, AgentLLMProvider agentLLMProvider, ObjectMapper objectMapper) {
        this.agentToolRegistry = Objects.requireNonNull(agentToolRegistry);
        this.agentLLMProvider = Objects.requireNonNull(agentLLMProvider);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public AgentReply handleMessage(IEditingContext editingContext, String userMessage, List<String> selectedObjectIds) {
        String sanitizedMessage = "";
        if (userMessage != null) {
            sanitizedMessage = userMessage.trim();
        }
        AgentReply reply = this.messageOnlyReply("Please describe what you want to do. For example: create a part named Battery.");
        if (!this.agentLLMProvider.isConfigured()) {
            reply = this.messageOnlyReply(
                    "The SysON agent is unavailable because no OpenAI API key is configured. "
                            + "Set 'syson.agent.llm.openai.api-key' before using the agent.");
        } else if (!sanitizedMessage.isBlank()) {
            Optional<AgentRoutingDecision> routingDecisionOptional = this.route(editingContext, sanitizedMessage, selectedObjectIds);
            if (routingDecisionOptional.isPresent()) {
                AgentRoutingDecision routingDecision = routingDecisionOptional.get();
                Optional<AgentTool> toolOptional = this.agentToolRegistry.findById(routingDecision.toolId());
                if (toolOptional.isPresent()) {
                    AgentToolRequest toolRequest = new AgentToolRequest(
                            editingContext,
                            sanitizedMessage,
                            selectedObjectIds,
                            routingDecision.arguments());
                    AgentReply toolReply = toolOptional.get().prepare(toolRequest);
                    if (routingDecision.assistantMessageOptional().isPresent() && toolReply.pendingConfirmation()) {
                        List<AgentMessage> prefixedMessages = new ArrayList<>();
                        prefixedMessages.add(this.agentMessage(
                                "assistant",
                                routingDecision.assistantMessageOptional().orElse(""),
                                "status"));
                        prefixedMessages.addAll(toolReply.messages());
                        reply = new AgentReply(prefixedMessages, toolReply.proposal(), toolReply.pendingConfirmation());
                    } else {
                        reply = toolReply;
                    }
                } else {
                    reply = this.messageOnlyReply("I understood the request, but no matching capability is registered yet.");
                }
            } else {
                reply = this.messageOnlyReply(
                        "I could not obtain a valid routing response from OpenAI. Check the API key, model access, "
                                + "and network connection, then try again.");
            }
        }
        return reply;
    }

    public AgentReply confirmProposal(IEditingContext editingContext, AgentProposalInput proposalInput) {
        Optional<AgentTool> toolOptional = this.agentToolRegistry.findById(proposalInput.toolId());
        if (toolOptional.isEmpty()) {
            return this.messageOnlyReply("This proposal references a capability that is no longer available.");
        }

        Map<String, String> arguments = this.parseArguments(proposalInput.argumentsJson());
        AgentToolRequest toolRequest = new AgentToolRequest(editingContext, proposalInput.summary(), List.of(), arguments);
        return toolOptional.get().execute(toolRequest, proposalInput);
    }

    public AgentReply cancelProposal(AgentProposalInput proposalInput) {
        return this.messageOnlyReply("Proposal cancelled. The model was not changed.");
    }

    private Optional<AgentRoutingDecision> route(IEditingContext editingContext, String userMessage, List<String> selectedObjectIds) {
        Optional<AgentRoutingDecision> routingDecisionOptional = Optional.empty();
        Optional<String> llmCompletion = this.agentLLMProvider.complete(
                this.buildSystemPrompt(),
                this.buildUserPrompt(editingContext, userMessage, selectedObjectIds));
        if (llmCompletion.isPresent()) {
            routingDecisionOptional = this.parseDecision(llmCompletion.get());
        }
        return routingDecisionOptional;
    }

    private String buildSystemPrompt() {
        String toolDescriptions = this.agentToolRegistry.definitions().stream()
                .map(definition -> definition.id() + " | " + definition.name() + " | " + definition.description()
                        + " | mutating=" + definition.mutating())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return """
                You are the internal routing brain for the SysON agent.
                Choose exactly one best tool for the user request and reply with compact JSON only.
                Never write markdown.
                Never write explanations outside the JSON object.
                JSON shape:
                {"toolId":"...","assistantMessage":"...","arguments":{"key":"value"}}
                Use only registered tool ids.
                Prefer modeling.create_part_usage when the user asks to create, define, add, or model a part.
                Prefer review.guidelines when the user asks to review, check, assess, validate,
                or inspect the current selection, its naming, or its readiness.
                When the request is about creating a part:
                - put the intended part name in arguments.declaredName
                - if a selected object id is available, use the first selected id as arguments.targetObjectId
                  unless the prompt clearly indicates another target
                - if the name is not explicit, infer a short reasonable declaredName from the request
                - do not ask the user for additional domain details such as voltage, capacity, or type
                  unless the tool specifically requires them
                - do not promise that an action will be executed immediately; proposals must be confirmed first
                When the request is about review or guidelines:
                - use review.guidelines
                - do not create a proposal
                - rely on the current selection unless the prompt explicitly mentions another target
                Use assistant.explain only for requests that are not actionable modeling operations.
                Registered tools:
                """ + "\n" + toolDescriptions;
    }

    private String buildUserPrompt(IEditingContext editingContext, String userMessage, List<String> selectedObjectIds) {
        String selectedContext = "none";
        if (!selectedObjectIds.isEmpty()) {
            selectedContext = String.join(",", selectedObjectIds);
        }
        return """
                Editing context: %s
                Selected object ids: %s
                User request: %s
                """.formatted(editingContext.getId(), selectedContext, userMessage);
    }

    private Optional<AgentRoutingDecision> parseDecision(String completion) {
        Optional<AgentRoutingDecision> decisionOptional = Optional.empty();
        try {
            Map<String, Object> raw = this.objectMapper.readValue(completion, new TypeReference<>() {
            });
            Object toolIdValue = raw.get("toolId");
            if (toolIdValue instanceof String toolId && !toolId.isBlank()) {
                Map<String, String> arguments = Map.of();
                Object argumentsObject = raw.get("arguments");
                if (argumentsObject != null) {
                    arguments = this.objectMapper.convertValue(argumentsObject, MAP_OF_STRING_TYPE);
                }
                String assistantMessage = null;
                if (raw.get("assistantMessage") instanceof String message) {
                    assistantMessage = message;
                }
                decisionOptional = Optional.of(new AgentRoutingDecision(toolId, arguments, assistantMessage));
            }
        } catch (IllegalArgumentException | IOException exception) {
        }
        return decisionOptional;
    }

    private Map<String, String> parseArguments(String argumentsJson) {
        Map<String, String> arguments = Map.of();
        if (argumentsJson == null || argumentsJson.isBlank()) {
            arguments = Map.of();
        } else {
            try {
                arguments = this.objectMapper.readValue(argumentsJson, MAP_OF_STRING_TYPE);
            } catch (IOException exception) {
                arguments = Map.of();
            }
        }
        return arguments;
    }

    private AgentReply messageOnlyReply(String content) {
        return new AgentReply(List.of(this.agentMessage("assistant", content, "text")), null, false);
    }

    private AgentMessage agentMessage(String role, String content, String variant) {
        return new AgentMessage(UUID.randomUUID().toString(), role, content, variant);
    }
}
