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

import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.components.core.api.IEditingContextSearchService;
import org.eclipse.sirius.components.core.api.ILabelService;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.application.agent.dto.AgentSelectionObject;
import org.eclipse.syson.sysml.Element;
import org.springframework.stereotype.Service;

/**
 * Resolves minimal label and type metadata for the currently selected object shown in the Agent panel.
 *
 * @author Codex
 */
@Service
public class AgentSelectionObjectService {

    private final IEditingContextSearchService editingContextSearchService;

    private final IObjectSearchService objectSearchService;

    private final ILabelService labelService;

    public AgentSelectionObjectService(IEditingContextSearchService editingContextSearchService, IObjectSearchService objectSearchService, ILabelService labelService) {
        this.editingContextSearchService = Objects.requireNonNull(editingContextSearchService);
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.labelService = Objects.requireNonNull(labelService);
    }

    public Optional<AgentSelectionObject> findById(String editingContextId, String objectId) {
        return this.editingContextSearchService.findById(editingContextId)
                .flatMap(editingContext -> this.objectSearchService.getObject(editingContext, objectId))
                .map(object -> new AgentSelectionObject(objectId, this.resolveLabel(object, objectId), this.resolveType(object)));
    }

    private String resolveLabel(Object object, String fallbackId) {
        String label = fallbackId;
        if (object instanceof Element element) {
            label = Optional.ofNullable(element.getDeclaredName())
                    .filter(name -> !name.isBlank())
                    .orElse(fallbackId);
        }
        String styledLabel = this.labelService.getStyledLabel(object).toString().trim();
        if (!styledLabel.isBlank() && styledLabel.length() < label.length()) {
            label = styledLabel;
        } else if (!styledLabel.isBlank() && label.equals(fallbackId)) {
            label = styledLabel;
        }
        return label;
    }

    private String resolveType(Object object) {
        String type = object.getClass().getSimpleName();
        if (object instanceof EObject eObject) {
            type = eObject.eClass().getName();
        }
        return type;
    }
}
