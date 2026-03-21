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

import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IEditingContextSearchService;
import org.eclipse.sirius.components.core.api.ILabelService;
import org.eclipse.sirius.components.core.api.labels.StyledString;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.application.agent.dto.AgentSelectionObject;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AgentSelectionObjectService}.
 *
 * @author Codex
 */
public class AgentSelectionObjectServiceTest {

    @Test
    public void testFindByIdReturnsDeclaredNameAndEClassName() {
        IEditingContextSearchService editingContextSearchService = mock(IEditingContextSearchService.class);
        IObjectSearchService objectSearchService = mock(IObjectSearchService.class);
        ILabelService labelService = mock(ILabelService.class);
        AgentSelectionObjectService service = new AgentSelectionObjectService(editingContextSearchService, objectSearchService, labelService);

        String editingContextId = UUID.randomUUID().toString();
        IEditingContext editingContext = mock(IEditingContext.class);
        Package selectedPackage = SysmlFactory.eINSTANCE.createPackage();
        selectedPackage.setDeclaredName("VehicleSystem");

        when(editingContextSearchService.findById(editingContextId)).thenReturn(Optional.of(editingContext));
        when(objectSearchService.getObject(editingContext, "target-1")).thenReturn(Optional.of(selectedPackage));
        when(labelService.getStyledLabel(selectedPackage)).thenReturn(StyledString.of("VehicleSystem"));

        Optional<AgentSelectionObject> selectionObjectOptional = service.findById(editingContextId, "target-1");

        assertThat(selectionObjectOptional).isPresent();
        assertThat(selectionObjectOptional.get().label()).isEqualTo("VehicleSystem");
        assertThat(selectionObjectOptional.get().type()).isEqualTo("Package");
        assertThat(selectionObjectOptional.get().id()).isEqualTo("target-1");
    }
}
