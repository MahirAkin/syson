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

import java.util.Objects;

/**
 * Minimal selection metadata used by the Agent workbench view.
 *
 * @author Codex
 */
public record AgentSelectionObject(String id, String label, String type) {

    public AgentSelectionObject {
        Objects.requireNonNull(id);
        Objects.requireNonNull(label);
        Objects.requireNonNull(type);
    }
}
