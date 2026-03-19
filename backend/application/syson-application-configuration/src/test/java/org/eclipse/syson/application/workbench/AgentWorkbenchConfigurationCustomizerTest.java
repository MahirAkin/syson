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
package org.eclipse.syson.application.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.DefaultViewConfiguration;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.WorkbenchConfiguration;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.WorkbenchMainPanelConfiguration;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.WorkbenchSidePanelConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link AgentWorkbenchConfigurationCustomizer}.
 *
 * @author Codex
 */
public class AgentWorkbenchConfigurationCustomizerTest {

    private final AgentWorkbenchConfigurationCustomizer customizer = new AgentWorkbenchConfigurationCustomizer();

    @Test
    public void addAgentViewAfterDetailsInRightWorkbenchPanel() {
        WorkbenchConfiguration workbenchConfiguration = new WorkbenchConfiguration(
                new WorkbenchMainPanelConfiguration("main", List.of()),
                List.of(
                        new WorkbenchSidePanelConfiguration("left", true, List.of(new DefaultViewConfiguration("explorer", true))),
                        new WorkbenchSidePanelConfiguration("right", true, List.of(
                                new DefaultViewConfiguration("details", true),
                                new DefaultViewConfiguration("query", false)
                        ))
                )
        );

        WorkbenchConfiguration customizedWorkbenchConfiguration = this.customizer.customize("editing-context", workbenchConfiguration);

        List<String> rightPanelViewIds = customizedWorkbenchConfiguration.workbenchPanels().stream()
                .filter(panel -> "right".equals(panel.id()))
                .findFirst()
                .orElseThrow()
                .views()
                .stream()
                .map(view -> view.id())
                .toList();

        assertThat(rightPanelViewIds).containsExactly("details", AgentWorkbenchConfigurationCustomizer.AGENT_VIEW_ID, "query");
    }

    @Test
    public void doNotDuplicateAgentViewIfItAlreadyExists() {
        WorkbenchConfiguration workbenchConfiguration = new WorkbenchConfiguration(
                new WorkbenchMainPanelConfiguration("main", List.of()),
                List.of(
                        new WorkbenchSidePanelConfiguration("right", true, List.of(
                                new DefaultViewConfiguration("details", true),
                                new DefaultViewConfiguration(AgentWorkbenchConfigurationCustomizer.AGENT_VIEW_ID, false)
                        ))
                )
        );

        WorkbenchConfiguration customizedWorkbenchConfiguration = this.customizer.customize("editing-context", workbenchConfiguration);

        long agentViewCount = customizedWorkbenchConfiguration.workbenchPanels().stream()
                .flatMap(panel -> panel.views().stream())
                .filter(view -> AgentWorkbenchConfigurationCustomizer.AGENT_VIEW_ID.equals(view.id()))
                .count();

        assertThat(agentViewCount).isEqualTo(1);
    }
}
