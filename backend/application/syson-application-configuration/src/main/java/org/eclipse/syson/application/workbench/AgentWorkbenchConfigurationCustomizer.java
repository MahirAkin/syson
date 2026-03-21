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

import java.util.ArrayList;
import java.util.List;

import java.util.Objects;

import org.eclipse.sirius.components.collaborative.workbenchconfiguration.api.IDefaultWorkbenchConfigurationProvider;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.api.IWorkbenchConfigurationProviderDelegate;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.DefaultViewConfiguration;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.IViewConfiguration;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.WorkbenchConfiguration;
import org.eclipse.sirius.components.collaborative.workbenchconfiguration.dto.WorkbenchSidePanelConfiguration;
import org.springframework.stereotype.Service;

/**
 * Adds the agent view to the right workbench panel.
 * <p>
 * Sirius Web 2026.1.6 resolves workbench layouts through {@link IWorkbenchConfigurationProviderDelegate} rather than
 * the older customizer hook, so this class wraps the default provider and augments its result.
 * </p>
 *
 * @author Codex
 */
@Service
public class AgentWorkbenchConfigurationCustomizer implements IWorkbenchConfigurationProviderDelegate {

    public static final String AGENT_VIEW_ID = "agent";

    private static final String RIGHT_PANEL_ID = "right";

    private static final String DETAILS_VIEW_ID = "details";

    private final IDefaultWorkbenchConfigurationProvider defaultWorkbenchConfigurationProvider;

    public AgentWorkbenchConfigurationCustomizer(IDefaultWorkbenchConfigurationProvider defaultWorkbenchConfigurationProvider) {
        this.defaultWorkbenchConfigurationProvider = Objects.requireNonNull(defaultWorkbenchConfigurationProvider);
    }

    @Override
    public boolean canHandle(String editingContextId) {
        return true;
    }

    @Override
    public WorkbenchConfiguration getWorkbenchConfiguration(String editingContextId) {
        WorkbenchConfiguration workbenchConfiguration = this.defaultWorkbenchConfigurationProvider.getWorkbenchConfiguration(editingContextId);
        var customizedPanels = workbenchConfiguration.workbenchPanels().stream()
                .map(this::customizeRightPanel)
                .toList();

        return new WorkbenchConfiguration(workbenchConfiguration.mainPanel(), customizedPanels);
    }

    private WorkbenchSidePanelConfiguration customizeRightPanel(WorkbenchSidePanelConfiguration panelConfiguration) {
        WorkbenchSidePanelConfiguration customizedPanelConfiguration = panelConfiguration;
        boolean isRightPanel = RIGHT_PANEL_ID.equals(panelConfiguration.id());
        boolean containsAgentView = panelConfiguration.views().stream().anyMatch(view -> AGENT_VIEW_ID.equals(view.id()));

        if (isRightPanel && !containsAgentView) {
            List<IViewConfiguration> customizedViews = new ArrayList<>();
            boolean inserted = false;
            for (IViewConfiguration viewConfiguration : panelConfiguration.views()) {
                customizedViews.add(viewConfiguration);
                if (DETAILS_VIEW_ID.equals(viewConfiguration.id())) {
                    customizedViews.add(new DefaultViewConfiguration(AGENT_VIEW_ID, false));
                    inserted = true;
                }
            }
            if (!inserted) {
                customizedViews.add(new DefaultViewConfiguration(AGENT_VIEW_ID, false));
            }

            customizedPanelConfiguration = new WorkbenchSidePanelConfiguration(panelConfiguration.id(), panelConfiguration.isOpen(), customizedViews);
        }
        return customizedPanelConfiguration;
    }
}
