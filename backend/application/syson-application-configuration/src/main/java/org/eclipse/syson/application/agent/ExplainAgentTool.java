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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentProposalInput;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.springframework.stereotype.Service;

/**
 * General fallback capability used when no specialized tool fits.
 *
 * @author Codex
 */
@Service
public class ExplainAgentTool implements AgentTool {

    private static final AgentToolDefinition TOOL_DEFINITION = new AgentToolDefinition(
            "assistant.explain",
            "Explain and Guide",
            "Fallback assistant that explains what the registered capabilities can do.",
            false);

    @Override
    public AgentToolDefinition definition() {
        return TOOL_DEFINITION;
    }

    @Override
    public AgentReply prepare(AgentToolRequest request) {
        String originalRequest = Optional.ofNullable(request.arguments().get("request")).orElse(request.userMessage());
        String normalizedRequest = originalRequest.toLowerCase();
        String response = String.join("\n\n",
                "I cannot safely execute that yet. Right now I can propose creating a PartUsage from chat "
                        + "and I can run a lightweight guideline review on the current selection.",
                "Request received: " + originalRequest);
        if (this.isConfirmationLikeRequest(normalizedRequest)) {
            response = "There is no pending proposal to confirm yet. First select a package, part, or definition in SysON, "
                    + "then ask me something like: create a battery part.";
        }
        return new AgentReply(List.of(this.agentMessage(
                "assistant",
                response,
                "text")), null, false);
    }

    private boolean isConfirmationLikeRequest(String normalizedRequest) {
        return this.containsImmediateExecutionRequest(normalizedRequest)
                || this.containsConfirmationKeyword(normalizedRequest);
    }

    private boolean containsImmediateExecutionRequest(String normalizedRequest) {
        return normalizedRequest.contains("ok do it now")
                || normalizedRequest.contains("do it now")
                || normalizedRequest.contains("go ahead");
    }

    private boolean containsConfirmationKeyword(String normalizedRequest) {
        return normalizedRequest.contains("confirm")
                || normalizedRequest.contains("proceed");
    }

    @Override
    public AgentReply execute(AgentToolRequest request, AgentProposalInput proposalInput) {
        return this.prepare(request);
    }

    private AgentMessage agentMessage(String role, String content, String variant) {
        return new AgentMessage(UUID.randomUUID().toString(), role, content, variant);
    }
}
