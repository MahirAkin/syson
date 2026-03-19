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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentProposal;
import org.eclipse.syson.application.agent.dto.AgentProposalField;
import org.eclipse.syson.application.agent.dto.AgentProposalInput;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.eclipse.syson.model.services.ModelMutationElementService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.PartUsage;
import org.springframework.stereotype.Service;

/**
 * First mutating modeling tool: create a {@link PartUsage}.
 *
 * @author Codex
 */
@Service
public class CreatePartUsageAgentTool implements AgentTool {

    private static final Pattern NAMED_PART_PATTERN = Pattern.compile("(?i)\\b(?:named|called)\\s+([a-z0-9][a-z0-9 _-]*)$");

    private static final Pattern CREATE_PREFIX_PATTERN = Pattern.compile("(?i)^\\s*(?:please\\s+)?(?:create|add|define|model)\\s+");

    private static final String SPACE = " ";

    private static final AgentToolDefinition TOOL_DEFINITION = new AgentToolDefinition(
            "modeling.create_part_usage",
            "Create PartUsage",
            "Creates a PartUsage in the selected SysML container after explicit confirmation.",
            true);

    private final ObjectMapper objectMapper;

    private final IObjectSearchService objectSearchService;

    private final ModelMutationElementService modelMutationElementService;

    public CreatePartUsageAgentTool(ObjectMapper objectMapper, IObjectSearchService objectSearchService, ModelMutationElementService modelMutationElementService) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.modelMutationElementService = Objects.requireNonNull(modelMutationElementService);
    }

    @Override
    public AgentToolDefinition definition() {
        return TOOL_DEFINITION;
    }

    @Override
    public AgentReply prepare(AgentToolRequest request) {
        AgentReply reply = this.messageOnlyReply("Select the package, part, or definition that should contain the new element, then ask again.");
        Optional<String> targetObjectIdOptional = Optional.ofNullable(request.arguments().get("targetObjectId"))
                .or(() -> request.selectedObjectIds().stream().findFirst());
        if (targetObjectIdOptional.isPresent()) {
            String declaredName = this.resolveDeclaredName(request);
            if (declaredName.isBlank()) {
                reply = this.messageOnlyReply("I need a name for the new PartUsage. For example: create a part named Battery.");
            } else {
                String targetObjectId = targetObjectIdOptional.get();
                String targetLabel = this.objectSearchService.getObject(request.editingContext(), targetObjectId)
                        .filter(Element.class::isInstance)
                        .map(Element.class::cast)
                        .map(element -> Optional.ofNullable(element.getDeclaredName()).filter(name -> !name.isBlank()).orElse(targetObjectId))
                        .orElse(targetObjectId);

                Map<String, String> proposalArguments = Map.of(
                        "declaredName", declaredName,
                        "targetObjectId", targetObjectId);

                String argumentsJson = this.toJson(proposalArguments);
                AgentProposal proposal = new AgentProposal(
                        UUID.randomUUID().toString(),
                        TOOL_DEFINITION.id(),
                        "CREATE_ELEMENT",
                        targetObjectId,
                        targetLabel,
                        "PartUsage",
                        "Create PartUsage '" + declaredName + "' in " + targetLabel + ".",
                        argumentsJson,
                        List.of(
                                new AgentProposalField("Element Type", "PartUsage"),
                                new AgentProposalField("Declared Name", declaredName),
                                new AgentProposalField("Target", targetLabel)));

                reply = new AgentReply(List.of(
                        this.agentMessage("assistant", "I prepared a proposal for review. Confirm it to apply the model change.", "text")),
                        proposal,
                        true);
            }
        }
        return reply;
    }

    @Override
    public AgentReply execute(AgentToolRequest request, AgentProposalInput proposalInput) {
        AgentReply reply = this.messageOnlyReply("The proposal is incomplete, so I could not apply it.");
        Map<String, String> arguments = request.arguments();
        String targetObjectId = arguments.get("targetObjectId");
        String declaredName = arguments.get("declaredName");
        if (targetObjectId != null && declaredName != null && !declaredName.isBlank()) {
            Optional<Element> targetElementOptional = this.objectSearchService.getObject(request.editingContext(), targetObjectId)
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast);
            if (targetElementOptional.isPresent()) {
                Optional<PartUsage> partUsageOptional = this.modelMutationElementService.createPartUsage(targetElementOptional.get(), declaredName);
                if (partUsageOptional.isPresent()) {
                    String createdName = Optional.ofNullable(partUsageOptional.get().getDeclaredName()).filter(name -> !name.isBlank()).orElse(declaredName);
                    reply = new AgentReply(List.of(
                            this.agentMessage("assistant", "Created PartUsage '" + createdName + "'.", "result")),
                            null,
                            false);
                } else {
                    reply = this.messageOnlyReply("The selected container does not support creating a PartUsage.");
                }
            } else {
                reply = this.messageOnlyReply("I could not find the selected SysML container anymore. Please create a new proposal.");
            }
        }
        return reply;
    }

    private String toJson(Map<String, String> value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String resolveDeclaredName(AgentToolRequest request) {
        return Optional.ofNullable(request.arguments().get("declaredName"))
                .map(this::normalizeDeclaredName)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> this.extractDeclaredNameFromRequest(request.userMessage()));
    }

    private String extractDeclaredNameFromRequest(String userMessage) {
        String extractedName = "";
        if (userMessage != null) {
            Matcher namedPartMatcher = NAMED_PART_PATTERN.matcher(userMessage.trim());
            if (namedPartMatcher.find()) {
                extractedName = this.normalizeDeclaredName(namedPartMatcher.group(1));
            }
            if (extractedName.isBlank()) {
                String simplifiedRequest = CREATE_PREFIX_PATTERN.matcher(userMessage).replaceFirst("")
                        .replaceAll("(?i)\\b(?:a|an|the)\\b", SPACE)
                        .replaceAll("(?i)\\b(?:new|sysml|element|usage)\\b", SPACE)
                        .replaceAll("(?i)\\bpart\\b", SPACE)
                        .replaceAll("[^a-zA-Z0-9 ]", SPACE)
                        .replaceAll("\\s+", SPACE)
                        .trim();
                extractedName = this.normalizeDeclaredName(simplifiedRequest);
            }
        }
        return extractedName;
    }

    private String normalizeDeclaredName(String rawValue) {
        String normalizedName = "";
        if (rawValue != null) {
            normalizedName = rawValue.trim()
                    .replaceAll("(?i)\\b(?:named|called)\\b", SPACE)
                    .replaceAll("[^a-zA-Z0-9 ]", SPACE)
                    .replaceAll("\\s+", SPACE)
                    .trim();
            if (!normalizedName.isBlank()) {
                normalizedName = normalizedName.substring(0, 1).toUpperCase() + normalizedName.substring(1);
            }
        }
        return normalizedName;
    }

    private AgentReply messageOnlyReply(String content) {
        return new AgentReply(List.of(this.agentMessage("assistant", content, "text")), null, false);
    }

    private AgentMessage agentMessage(String role, String content, String variant) {
        return new AgentMessage(UUID.randomUUID().toString(), role, content, variant);
    }
}
