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

import org.eclipse.sirius.components.core.api.IEditingContext;

/**
 * Common tool request context.
 *
 * @author Codex
 */
public record AgentToolRequest(IEditingContext editingContext, String userMessage, List<String> selectedObjectIds, Map<String, String> arguments) {

    public AgentToolRequest {
        Objects.requireNonNull(editingContext);
        Objects.requireNonNull(userMessage);
        selectedObjectIds = List.copyOf(selectedObjectIds);
        arguments = Map.copyOf(arguments);
    }
}
