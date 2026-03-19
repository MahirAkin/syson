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
 * The assistant reply returned to the frontend.
 *
 * @author Codex
 */
public record AgentReply(List<AgentMessage> messages, AgentProposal proposal, boolean pendingConfirmation) {

    public AgentReply {
        Objects.requireNonNull(messages);
        messages = List.copyOf(messages);
    }
}
