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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.application.agent.dto.AgentMessage;
import org.eclipse.syson.application.agent.dto.AgentReply;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GuidelineReviewAgentTool}.
 *
 * @author Codex
 */
public class GuidelineReviewAgentToolTest {

    @Test
    public void testPrepareRequiresSelection() {
        IObjectSearchService objectSearchService = mock(IObjectSearchService.class);
        GuidelineReviewAgentTool tool = new GuidelineReviewAgentTool(objectSearchService);

        AgentToolRequest request = new AgentToolRequest(this.editingContext(), "review the selection", List.of(), Map.of());

        AgentReply reply = tool.prepare(request);

        assertThat(reply.pendingConfirmation()).isFalse();
        assertThat(reply.messages()).singleElement()
                .extracting(AgentMessage::content)
                .asString()
                .contains("Select one SysML element first");
    }

    @Test
    public void testPrepareReportsGeneratedNameAndContainerReadiness() {
        IObjectSearchService objectSearchService = mock(IObjectSearchService.class);
        GuidelineReviewAgentTool tool = new GuidelineReviewAgentTool(objectSearchService);

        Package selectedPackage = SysmlFactory.eINSTANCE.createPackage();
        selectedPackage.setDeclaredName("Package1");

        IEditingContext editingContext = this.editingContext();
        when(objectSearchService.getObject(editingContext, "target-1")).thenReturn(Optional.of(selectedPackage));

        AgentToolRequest request = new AgentToolRequest(editingContext, "review the selected element", List.of("target-1"), Map.of());

        AgentReply reply = tool.prepare(request);

        assertThat(reply.pendingConfirmation()).isFalse();
        assertThat(reply.messages()).singleElement()
                .extracting(AgentMessage::content)
                .asString()
                .contains("Review summary for Package 'Package1'")
                .contains("generated default name")
                .contains("valid container for the current PartUsage creation capability");
    }

    private IEditingContext editingContext() {
        IEditingContext editingContext = mock(IEditingContext.class);
        when(editingContext.getId()).thenReturn(UUID.randomUUID().toString());
        return editingContext;
    }
}
