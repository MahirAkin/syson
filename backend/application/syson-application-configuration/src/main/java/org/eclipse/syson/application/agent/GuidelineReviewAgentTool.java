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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentProposalInput;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.eclipse.syson.sysml.Element;
import org.springframework.stereotype.Service;

/**
 * Read-only review capability used to demonstrate how additional tools plug into the shared agent platform.
 *
 * @author Codex
 */
@Service
public class GuidelineReviewAgentTool implements AgentTool {

    private static final Pattern GENERATED_NAME_PATTERN = Pattern.compile(
            "(?i)^(?:package|part(?:definition|usage)?|item(?:definition|usage)?|feature|element)\\d+$");

    private static final AgentToolDefinition TOOL_DEFINITION = new AgentToolDefinition(
            "review.guidelines",
            "Guideline Review",
            "Runs a lightweight read-only review on the current SysML selection.",
            false);

    private final IObjectSearchService objectSearchService;

    public GuidelineReviewAgentTool(IObjectSearchService objectSearchService) {
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
    }

    @Override
    public AgentToolDefinition definition() {
        return TOOL_DEFINITION;
    }

    @Override
    public AgentReply prepare(AgentToolRequest request) {
        AgentReply reply = new AgentReply(List.of(this.agentMessage(
                "assistant",
                "Select one SysML element first, then ask for a review. I can currently run a lightweight naming "
                        + "and target-readiness check on the current selection.",
                "text")), null, false);
        if (!request.selectedObjectIds().isEmpty()) {
            String selectedObjectId = request.selectedObjectIds().get(0);
            Optional<Element> selectedElementOptional = this.objectSearchService.getObject(request.editingContext(), selectedObjectId)
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast);
            if (selectedElementOptional.isPresent()) {
                Element selectedElement = selectedElementOptional.get();
                String elementType = selectedElement.eClass().getName();
                String declaredName = Optional.ofNullable(selectedElement.getDeclaredName()).orElse("").trim();
                String elementLabel = selectedObjectId;
                if (!declaredName.isBlank()) {
                    elementLabel = declaredName;
                }

                List<String> findings = new ArrayList<>();
                List<String> recommendations = new ArrayList<>();

                if (declaredName.isBlank()) {
                    findings.add("The selected " + elementType + " has no declared name.");
                    recommendations.add("Add a meaningful declared name before using it as a stable agent target.");
                } else if (GENERATED_NAME_PATTERN.matcher(declaredName).matches()) {
                    findings.add("The declared name '" + declaredName + "' looks like a generated default name.");
                    recommendations.add(
                            "Rename it to a domain-specific concept before building more model content on top of it.");
                } else {
                    findings.add("The declared name '" + declaredName + "' is explicit enough for agent targeting.");
                }

                if (this.supportsPartUsageCreation(selectedElement)) {
                    findings.add(
                            "This selection can act as a valid container for the current PartUsage creation capability.");
                } else {
                    recommendations.add(
                            "Use a package, part usage, or part definition when you want the agent to create a PartUsage.");
                }

                StringBuilder response = new StringBuilder();
                response.append("Review summary for ").append(elementType).append(" '").append(elementLabel).append("'.");
                response.append("\n\nFindings:");
                findings.forEach(finding -> response.append("\n- ").append(finding));
                if (!recommendations.isEmpty()) {
                    response.append("\n\nRecommendations:");
                    recommendations.forEach(recommendation -> response.append("\n- ").append(recommendation));
                }

                reply = new AgentReply(List.of(this.agentMessage(
                        "assistant",
                        response.toString(),
                        "result")), null, false);
            } else {
                reply = new AgentReply(List.of(this.agentMessage(
                        "assistant",
                        "I could not resolve the selected element anymore. Select it again and rerun the review.",
                        "error")), null, false);
            }
        } else {
            reply = new AgentReply(List.of(this.agentMessage(
                    "assistant",
                    "Select one SysML element first, then ask for a review. I can currently run a lightweight naming "
                            + "and target-readiness check on the current selection.",
                    "text")), null, false);
        }
        return reply;
    }

    @Override
    public AgentReply execute(AgentToolRequest request, AgentProposalInput proposalInput) {
        return this.prepare(request);
    }

    private AgentMessage agentMessage(String role, String content, String variant) {
        return new AgentMessage(UUID.randomUUID().toString(), role, content, variant);
    }

    private boolean supportsPartUsageCreation(Element element) {
        String typeName = element.eClass().getName();
        return "Package".equals(typeName) || "PartUsage".equals(typeName) || "PartDefinition".equals(typeName);
    }
}
