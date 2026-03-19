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

import java.util.UUID;

import org.eclipse.sirius.components.core.api.IInput;

/**
 * Input of the confirm proposal mutation.
 *
 * @author Codex
 */
public record ConfirmAgentProposalInput(UUID id, String editingContextId, AgentProposalInput proposal) implements IInput {

}
