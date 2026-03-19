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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AgentConversationService}.
 *
 * @author Codex
 */
public class AgentConversationServiceTest {

    @Test
    public void testHandleMessageWithoutConfiguredProvider() {
        AgentToolRegistry agentToolRegistry = mock(AgentToolRegistry.class);
        AgentLLMProvider agentLLMProvider = mock(AgentLLMProvider.class);
        when(agentLLMProvider.isConfigured()).thenReturn(false);

        AgentConversationService service = new AgentConversationService(agentToolRegistry, agentLLMProvider, new ObjectMapper());

        AgentReply reply = service.handleMessage(this.editingContext(), "create a battery part", List.of("target-1"));

        assertThat(reply.pendingConfirmation()).isFalse();
        assertThat(reply.messages()).singleElement()
                .extracting(AgentMessage::content)
                .asString()
                .contains("no OpenAI API key is configured");
    }

    @Test
    public void testHandleMessageWithUnavailableOpenAIResponse() {
        AgentToolRegistry agentToolRegistry = mock(AgentToolRegistry.class);
        AgentLLMProvider agentLLMProvider = mock(AgentLLMProvider.class);
        when(agentLLMProvider.isConfigured()).thenReturn(true);
        when(agentLLMProvider.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        AgentConversationService service = new AgentConversationService(agentToolRegistry, agentLLMProvider, new ObjectMapper());

        AgentReply reply = service.handleMessage(this.editingContext(), "create a battery part", List.of("target-1"));

        assertThat(reply.pendingConfirmation()).isFalse();
        assertThat(reply.messages()).singleElement()
                .extracting(AgentMessage::content)
                .asString()
                .contains("could not obtain a valid routing response from OpenAI");
    }

    @Test
    public void testHandleMessageRoutesToRegisteredTool() {
        AgentToolRegistry agentToolRegistry = mock(AgentToolRegistry.class);
        AgentLLMProvider agentLLMProvider = mock(AgentLLMProvider.class);
        AgentTool agentTool = mock(AgentTool.class);

        when(agentLLMProvider.isConfigured()).thenReturn(true);
        when(agentLLMProvider.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of("""
                        {"toolId":"modeling.create_part_usage","assistantMessage":"I am preparing a safe modeling proposal.","arguments":{"declaredName":"Battery","targetObjectId":"target-1"}}
                        """));
        when(agentToolRegistry.findById("modeling.create_part_usage")).thenReturn(Optional.of(agentTool));
        when(agentTool.prepare(org.mockito.ArgumentMatchers.any())).thenReturn(new AgentReply(
                List.of(new AgentMessage(UUID.randomUUID().toString(), "assistant", "Proposal ready.", "text")),
                null,
                true));

        AgentConversationService service = new AgentConversationService(agentToolRegistry, agentLLMProvider, new ObjectMapper());

        AgentReply reply = service.handleMessage(this.editingContext(), "create a battery part", List.of("target-1"));

        assertThat(reply.messages()).hasSize(2);
        assertThat(reply.messages().get(0).variant()).isEqualTo("status");
        assertThat(reply.messages().get(0).content()).contains("preparing a safe modeling proposal");
        assertThat(reply.messages().get(1).content()).isEqualTo("Proposal ready.");
    }

    private IEditingContext editingContext() {
        IEditingContext editingContext = mock(IEditingContext.class);
        when(editingContext.getId()).thenReturn(UUID.randomUUID().toString());
        return editingContext;
    }
}
