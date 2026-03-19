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
package org.eclipse.syson.model.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.SysmlFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModelMutationElementService}.
 *
 * @author Codex
 */
public class ModelMutationElementServiceTest {

    @Test
    public void testCreatePartUsageCreatesOwnedMembership() {
        ModelMutationElementService service = new ModelMutationElementService();
        Package owningPackage = SysmlFactory.eINSTANCE.createPackage();

        Optional<PartUsage> createdPartUsageOptional = service.createPartUsage(owningPackage, "Battery");

        assertThat(createdPartUsageOptional).isPresent();
        PartUsage createdPartUsage = createdPartUsageOptional.get();
        assertThat(createdPartUsage.getDeclaredName()).isEqualTo("Battery");
        assertThat(owningPackage.getOwnedRelationship())
                .singleElement()
                .isInstanceOf(OwningMembership.class);
        assertThat(owningPackage.getOwnedRelationship().get(0).getOwnedRelatedElement()).contains(createdPartUsage);
    }

    @Test
    public void testCreatePartUsageRejectsMembershipContainer() {
        ModelMutationElementService service = new ModelMutationElementService();
        OwningMembership membership = SysmlFactory.eINSTANCE.createOwningMembership();

        Optional<PartUsage> createdPartUsageOptional = service.createPartUsage(membership, "Battery");

        assertThat(createdPartUsageOptional).isEmpty();
    }
}
