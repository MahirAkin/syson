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

/**
 * Serialized proposal data sent back by the frontend for confirmation and cancellation.
 *
 * @author Codex
 */
public record AgentProposalInput(String proposalId, String toolId, String actionType, String targetObjectId, String targetLabel, String elementType, String summary, String argumentsJson) {

}
