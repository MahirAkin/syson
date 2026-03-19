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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Registry of internal agent tools.
 *
 * @author Codex
 */
@Service
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolsById;

    public AgentToolRegistry(List<AgentTool> tools) {
        Objects.requireNonNull(tools);
        this.toolsById = tools.stream().collect(Collectors.toUnmodifiableMap(tool -> tool.definition().id(), Function.identity()));
    }

    public List<AgentToolDefinition> definitions() {
        return this.toolsById.values().stream()
                .map(AgentTool::definition)
                .sorted((left, right) -> left.id().compareToIgnoreCase(right.id()))
                .toList();
    }

    public Optional<AgentTool> findById(String toolId) {
        return Optional.ofNullable(this.toolsById.get(toolId));
    }
}
