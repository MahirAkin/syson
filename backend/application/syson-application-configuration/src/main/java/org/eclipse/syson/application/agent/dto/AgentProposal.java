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
package org.eclipse.syson.application.agent.dto;

import java.util.List;
import java.util.Objects;

/**
 * A reviewable proposal returned by the assistant before a model mutation.
 *
 * @author Codex
 */
public record AgentProposal(String proposalId, String toolId, String actionType, String targetObjectId, String targetLabel, String elementType, String summary, String argumentsJson,
        List<AgentProposalField> fields) {

    public AgentProposal {
        Objects.requireNonNull(proposalId);
        Objects.requireNonNull(toolId);
        Objects.requireNonNull(actionType);
        Objects.requireNonNull(summary);
        Objects.requireNonNull(argumentsJson);
        fields = List.copyOf(fields);
    }
}
