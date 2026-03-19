# SysON Agent Platform README

This document explains how the Agent platform inside SysON is structured and how to add a new capability cleanly. It is intentionally written so both humans and coding agents can use it as an implementation guide.

## Purpose

SysON exposes one visible `Agent` workbench panel in the right sidebar. Behind that single panel sits an internal modular agent platform.

The design goal is:

- one user-facing assistant
- many internal capabilities
- predictable extension points
- safe model mutation through review and confirmation
- merge-friendly integration into the main open source product

Do not build separate agent apps, extra UI shells, or parallel frontend entry points for new capabilities.

## Product rules

Any new capability added to this platform should follow these rules:

- Keep one visible workbench tab: `Agent`.
- Do not add a second agent sidebar tab unless the product architecture explicitly changes.
- Treat the backend tool registry as the modular extension surface.
- Use the LLM for routing, intent recognition, argument extraction, and response synthesis.
- Keep domain logic deterministic in Java services.
- Never mutate the model silently.
- Every mutating capability must follow `suggest -> confirm -> apply`.

## Current architecture

### Frontend

The frontend provides a single chat-oriented workbench shell.

Main files:

- [`frontend/syson-components/src/extensions/agent/AgentWorkbenchView.tsx`](./frontend/syson-components/src/extensions/agent/AgentWorkbenchView.tsx)
- [`frontend/syson-components/src/extensions/agent/useAgentConversation.ts`](./frontend/syson-components/src/extensions/agent/useAgentConversation.ts)
- [`frontend/syson-components/src/extensions/agent/AgentMessageList.tsx`](./frontend/syson-components/src/extensions/agent/AgentMessageList.tsx)
- [`frontend/syson-components/src/extensions/agent/AgentProposalCard.tsx`](./frontend/syson-components/src/extensions/agent/AgentProposalCard.tsx)

Responsibilities:

- render chat history
- render pending proposals
- send user messages
- confirm or cancel proposals
- stay generic so tool-specific UI does not leak into the shell

### Backend

The backend contains the actual agent runtime.

Main files:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentTool.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentTool.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentToolRegistry.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentToolRegistry.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentLLMProvider.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentLLMProvider.java)
- [`backend/application/syson-application-configuration/src/main/resources/schema/syson-agent.graphqls`](./backend/application/syson-application-configuration/src/main/resources/schema/syson-agent.graphqls)

Responsibilities:

- route a user message to one tool
- keep the list of registered tools
- execute tool logic
- manage proposal confirmation
- bridge the UI to deterministic model services

## Capability types

There are two current patterns.

### 1. Read-only tool

A read-only tool returns messages only.

Example:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/GuidelineReviewAgentTool.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/GuidelineReviewAgentTool.java)

Properties:

- `mutating = false`
- no proposal required
- no model mutation
- useful for reviews, checks, explanations, recommendations

### 2. Mutating tool

A mutating tool returns a proposal first, then applies the change only after confirmation.

Example:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/CreatePartUsageAgentTool.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/CreatePartUsageAgentTool.java)

Properties:

- `mutating = true`
- `prepare(...)` creates the proposal
- `execute(...)` applies the change after confirmation
- execution should delegate to normal SysON application/domain services

## Extension contract

To add a new capability, implement the following pieces.

### Step 1. Create a new backend tool

Add a class in:

- `backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent`

Implement:

- `AgentTool`
- `definition()`
- `prepare(...)`
- `execute(...)`

Requirements:

- give it a stable tool id
- give it a clear human-readable name
- describe it so the router can choose it
- make the mutating flag truthful

## Tool definition rules

Tool ids should be stable and namespaced.

Recommended patterns:

- `modeling.create_part_usage`
- `review.guidelines`
- `review.naming_conventions`
- `analysis.structure_summary`

Avoid vague ids such as:

- `tool1`
- `agentAction`
- `customFeature`

## Routing rules

The router lives in:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java)

When you add a new tool:

1. Register it as a Spring `@Service`.
2. Ensure the tool description is good enough for LLM routing.
3. Update the system prompt guidance if the new capability needs specific routing hints.
4. Keep the JSON routing format stable.

The routing response must resolve to exactly one registered tool id.

## Mutation rules

For mutating tools, follow this pattern exactly:

1. `prepare(...)` validates the request and target context.
2. `prepare(...)` builds a proposal object with a readable summary and structured fields.
3. The frontend renders the proposal.
4. The user confirms.
5. `execute(...)` applies the change through normal SysON services.
6. The tool returns a result message.

Never do this:

- apply the model change directly in `prepare(...)`
- let the frontend mutate the model
- hide mutating side effects behind a read-only tool

## Proposal design

Every mutating proposal should include at least:

- action type
- target object id
- target label
- element type
- human-readable summary
- serialized arguments
- proposal fields for the UI

The proposal must be understandable without reading backend code.

## Domain service rule

Do not put complex model mutation logic directly into tool classes.

Instead:

- keep routing/orchestration in agent classes
- keep SysML mutation in dedicated services
- reuse existing SysON services whenever possible

Current example:

- [`backend/services/syson-model-services/src/main/java/org/eclipse/syson/model/services/ModelMutationElementService.java`](./backend/services/syson-model-services/src/main/java/org/eclipse/syson/model/services/ModelMutationElementService.java)

## Frontend rule

Do not add hardcoded tool-specific screens to the workbench shell for every new capability.

Prefer:

- generic message rendering
- generic proposal rendering
- optional incremental enhancement only when a capability truly needs richer display

If richer UI is needed, extend the generic rendering pattern instead of branching the whole workbench view.

## OpenAI rule

The Agent platform currently requires an OpenAI API key.

Local property:

- `syson.agent.llm.openai.api-key`

Optional related properties:

- `syson.agent.llm.openai.model`
- `syson.agent.llm.openai.base-url`
- `syson.agent.llm.openai.timeout-seconds`

The agent should be unavailable when no key is configured. Do not add silent fallback behavior that hides configuration problems.

## Recommended workflow for adding a new tool

1. Decide whether the capability is read-only or mutating.
2. Define a stable tool id and description.
3. Implement the tool class.
4. Reuse or add the needed domain service.
5. Update routing guidance if needed.
6. Add focused tests.
7. Validate the capability through the existing `Agent` workbench tab.
8. Keep the visible UI as one unified assistant.

## Minimal checklist for a new read-only tool

- The tool is a Spring service.
- It implements `AgentTool`.
- `definition().mutating()` is `false`.
- `prepare(...)` returns a useful reply.
- `execute(...)` safely delegates to `prepare(...)` or another read-only path.
- There is at least one tool-specific test.

## Minimal checklist for a new mutating tool

- The tool is a Spring service.
- It implements `AgentTool`.
- `definition().mutating()` is `true`.
- `prepare(...)` returns a proposal instead of mutating.
- `execute(...)` performs the mutation through a SysON service.
- Confirmation is required in the UI.
- Failure cases return actionable messages.
- There is at least one tool test and one service test.

## Example future capabilities

Good candidates for this platform:

- naming convention review
- architecture smell review
- guideline compliance review
- element creation for more SysML types
- recommendation or explanation assistants
- import and cleanup helpers

## Anti-patterns

Avoid these patterns:

- a second frontend app just for the agent
- a different port only for agent work
- putting business logic into React components
- silent model mutation
- one-off hacks that only work in one fork
- tool ids that are not stable
- bypassing the shared workbench integration

## Agent-readable implementation brief

If you are an engineering agent and need to add a new capability, follow this exact recipe:

1. Keep the visible UI unchanged: one `Agent` workbench tab.
2. Add a new backend class implementing `AgentTool`.
3. Give it a stable namespaced id and accurate mutating flag.
4. If mutating, return an `AgentProposal` from `prepare(...)`.
5. If mutating, execute only from `execute(...)` after confirmation.
6. Route to it through `AgentConversationService`.
7. Reuse or extend a dedicated SysON service for domain changes.
8. Add tests near the existing agent tests.
9. Do not introduce a second client, port, or standalone agent shell.

## Where to start

If you want to continue this platform, read these files first:

- [`AGENT_IMPLEMENTATION_LOG.md`](./AGENT_IMPLEMENTATION_LOG.md)
- [`frontend/syson-components/src/extensions/agent/AgentWorkbenchView.tsx`](./frontend/syson-components/src/extensions/agent/AgentWorkbenchView.tsx)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/CreatePartUsageAgentTool.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/CreatePartUsageAgentTool.java)
