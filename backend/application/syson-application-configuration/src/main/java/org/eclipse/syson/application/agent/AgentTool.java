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

import org.eclipse.syson.application.agent.dto.AgentProposalInput;
import org.eclipse.syson.application.agent.dto.AgentReply;

/**
 * Internal agent tool contract.
 *
 * @author Codex
 */
public interface AgentTool {

    AgentToolDefinition definition();

    AgentReply prepare(AgentToolRequest request);

    AgentReply execute(AgentToolRequest request, AgentProposalInput proposalInput);
}
