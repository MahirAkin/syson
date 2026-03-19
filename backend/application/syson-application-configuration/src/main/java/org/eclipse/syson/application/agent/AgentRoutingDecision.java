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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Tool routing result produced by the orchestrator.
 *
 * @author Codex
 */
public record AgentRoutingDecision(String toolId, Map<String, String> arguments, String assistantMessage) {

    public AgentRoutingDecision {
        Objects.requireNonNull(toolId);
        arguments = Map.copyOf(arguments);
    }

    public Optional<String> assistantMessageOptional() {
        return Optional.ofNullable(this.assistantMessage);
    }
}
