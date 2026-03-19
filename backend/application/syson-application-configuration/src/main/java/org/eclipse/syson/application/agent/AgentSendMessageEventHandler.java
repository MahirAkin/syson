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
import java.util.Objects;
import java.util.UUID;

import org.eclipse.sirius.components.collaborative.api.ChangeDescription;
import org.eclipse.sirius.components.collaborative.api.ChangeKind;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventHandler;
import org.eclipse.sirius.components.collaborative.api.Monitoring;
import org.eclipse.sirius.components.collaborative.messages.ICollaborativeMessageService;
import org.eclipse.sirius.components.core.api.ErrorPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IInput;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.representations.Message;
import org.eclipse.sirius.components.representations.MessageLevel;
import org.eclipse.syson.application.agent.dto.AgentMessageSuccessPayload;
import org.eclipse.syson.application.agent.dto.SendAgentMessageInput;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Sinks.Many;
import reactor.core.publisher.Sinks.One;

/**
 * Event handler for send agent message mutation.
 *
 * @author Codex
 */
@Service
public class AgentSendMessageEventHandler implements IEditingContextEventHandler {

    private final AgentConversationService agentConversationService;

    private final ICollaborativeMessageService messageService;

    private final Counter counter;

    public AgentSendMessageEventHandler(AgentConversationService agentConversationService, ICollaborativeMessageService messageService, MeterRegistry meterRegistry) {
        this.agentConversationService = Objects.requireNonNull(agentConversationService);
        this.messageService = Objects.requireNonNull(messageService);
        this.counter = Counter.builder(Monitoring.EVENT_HANDLER)
                .tag(Monitoring.NAME, this.getClass().getSimpleName())
                .register(meterRegistry);
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IInput input) {
        return input instanceof SendAgentMessageInput;
    }

    @Override
    public void handle(One<IPayload> payloadSink, Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, IInput input) {
        this.counter.increment();

        UUID payloadId = UUID.randomUUID();
        if (input instanceof SendAgentMessageInput sendAgentMessageInput) {
            payloadId = sendAgentMessageInput.id();
        }
        IPayload payload = new ErrorPayload(payloadId, List.of(
                new Message(this.messageService.invalidInput(input.getClass().getSimpleName(), SendAgentMessageInput.class.getSimpleName()), MessageLevel.ERROR)));
        ChangeDescription changeDescription = new ChangeDescription(ChangeKind.NOTHING, editingContext.getId(), input);

        if (input instanceof SendAgentMessageInput sendAgentMessageInput) {
            payload = new AgentMessageSuccessPayload(sendAgentMessageInput.id(),
                    this.agentConversationService.handleMessage(editingContext, sendAgentMessageInput.message(), sendAgentMessageInput.selectedObjectIds()));
        }

        payloadSink.tryEmitValue(payload);
        changeDescriptionSink.tryEmitNext(changeDescription);
    }
}
