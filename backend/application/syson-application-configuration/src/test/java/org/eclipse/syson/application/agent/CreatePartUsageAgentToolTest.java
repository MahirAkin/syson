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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.eclipse.syson.model.services.ModelMutationElementService;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CreatePartUsageAgentTool}.
 *
 * @author Codex
 */
public class CreatePartUsageAgentToolTest {

    @Test
    public void testPrepareUsesSelectedTargetAndNaturalLanguageName() {
        IObjectSearchService objectSearchService = mock(IObjectSearchService.class);
        ModelMutationElementService modelMutationElementService = mock(ModelMutationElementService.class);
        CreatePartUsageAgentTool tool = new CreatePartUsageAgentTool(new ObjectMapper(), objectSearchService, modelMutationElementService);

        Package targetPackage = SysmlFactory.eINSTANCE.createPackage();
        targetPackage.setDeclaredName("VehicleSystem");

        IEditingContext editingContext = mock(IEditingContext.class);
        when(editingContext.getId()).thenReturn(UUID.randomUUID().toString());
        when(objectSearchService.getObject(editingContext, "target-1")).thenReturn(Optional.of(targetPackage));

        AgentToolRequest request = new AgentToolRequest(editingContext, "create a vehicle part", List.of("target-1"), Map.of());

        AgentReply reply = tool.prepare(request);

        assertThat(reply.pendingConfirmation()).isTrue();
        assertThat(reply.proposal()).isNotNull();
        assertThat(reply.proposal().summary()).contains("Vehicle");
        assertThat(reply.proposal().summary()).contains("VehicleSystem");
        assertThat(reply.proposal().fields()).extracting(field -> field.value()).contains("Vehicle", "VehicleSystem");
    }

    @Test
    public void testPrepareRequiresTargetSelection() {
        IObjectSearchService objectSearchService = mock(IObjectSearchService.class);
        ModelMutationElementService modelMutationElementService = mock(ModelMutationElementService.class);
        CreatePartUsageAgentTool tool = new CreatePartUsageAgentTool(new ObjectMapper(), objectSearchService, modelMutationElementService);

        AgentToolRequest request = new AgentToolRequest(this.editingContext(), "create a battery part", List.of(), Map.of());

        AgentReply reply = tool.prepare(request);

        assertThat(reply.pendingConfirmation()).isFalse();
        assertThat(reply.messages()).singleElement()
                .extracting(AgentMessage::content)
                .asString()
                .contains("Select the package, part, or definition");
    }

    private IEditingContext editingContext() {
        IEditingContext editingContext = mock(IEditingContext.class);
        when(editingContext.getId()).thenReturn(UUID.randomUUID().toString());
        return editingContext;
    }
}
