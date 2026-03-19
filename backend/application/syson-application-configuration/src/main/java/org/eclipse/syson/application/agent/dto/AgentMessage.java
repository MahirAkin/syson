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
 * A single assistant message rendered in the workbench chat.
 *
 * @author Codex
 */
public record AgentMessage(String id, String role, String content, String variant) {

    public AgentMessage {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
        Objects.requireNonNull(content);
        Objects.requireNonNull(variant);
    }
}
