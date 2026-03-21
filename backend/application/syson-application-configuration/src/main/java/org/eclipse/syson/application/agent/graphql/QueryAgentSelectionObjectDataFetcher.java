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
package org.eclipse.syson.application.agent.graphql;

import java.util.Map;
import java.util.Objects;

import org.eclipse.sirius.components.annotations.spring.graphql.QueryDataFetcher;
import org.eclipse.sirius.components.graphql.api.IDataFetcherWithFieldCoordinates;
import org.eclipse.syson.application.agent.AgentSelectionObjectService;
import org.eclipse.syson.application.agent.dto.AgentSelectionObject;

import graphql.schema.DataFetchingEnvironment;

/**
 * Data fetcher for {@code Query#agentSelectionObject}.
 *
 * @author Codex
 */
@QueryDataFetcher(type = "Query", field = "agentSelectionObject")
public class QueryAgentSelectionObjectDataFetcher implements IDataFetcherWithFieldCoordinates<AgentSelectionObject> {

    private final AgentSelectionObjectService agentSelectionObjectService;

    public QueryAgentSelectionObjectDataFetcher(AgentSelectionObjectService agentSelectionObjectService) {
        this.agentSelectionObjectService = Objects.requireNonNull(agentSelectionObjectService);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentSelectionObject get(DataFetchingEnvironment environment) throws Exception {
        Map<String, Object> arguments = environment.getArguments();
        String editingContextId = Objects.toString(arguments.get("editingContextId"), "");
        String objectId = Objects.toString(arguments.get("objectId"), "");
        return this.agentSelectionObjectService.findById(editingContextId, objectId).orElse(null);
    }
}
