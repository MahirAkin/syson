# SysON Agent Implementation Log

This document is the project log for the current Agent milestone in this fork. It is written for local handover and future continuation work.

## Goal of this milestone

We wanted one native `Agent` panel inside the existing SysON workbench, not a second app, not a custom port setup, and not a Docker-only workaround. The milestone was considered successful once one real end-to-end flow worked inside SysON:

`ask -> propose -> confirm -> apply`

The first implemented modeling action is:

- create a `PartUsage` from chat in the currently selected SysML container

## What was implemented

### 1. Native workbench integration

The Agent is integrated as a right-side workbench tab next to the existing workbench views.

Main files:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/workbench/AgentWorkbenchConfigurationCustomizer.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/workbench/AgentWorkbenchConfigurationCustomizer.java)
- [`frontend/syson-components/src/extensions/registry/SysONExtensionRegistry.ts`](./frontend/syson-components/src/extensions/registry/SysONExtensionRegistry.ts)
- [`frontend/syson-components/src/extensions/SysONExtensionRegistryMergeStrategy.ts`](./frontend/syson-components/src/extensions/SysONExtensionRegistryMergeStrategy.ts)

### 2. Real agent chat shell

The placeholder panel was replaced with a real workbench chat view that contains:

- message list
- composer
- proposal card
- confirm/cancel actions
- loading and error handling

Main files:

- [`frontend/syson-components/src/extensions/agent/AgentWorkbenchView.tsx`](./frontend/syson-components/src/extensions/agent/AgentWorkbenchView.tsx)
- [`frontend/syson-components/src/extensions/agent/useAgentConversation.ts`](./frontend/syson-components/src/extensions/agent/useAgentConversation.ts)
- [`frontend/syson-components/src/extensions/agent/AgentProposalCard.tsx`](./frontend/syson-components/src/extensions/agent/AgentProposalCard.tsx)
- [`frontend/syson-components/src/extensions/agent/AgentMessageList.tsx`](./frontend/syson-components/src/extensions/agent/AgentMessageList.tsx)

### 3. Internal agent platform on the backend

The backend now contains an internal modular agent runtime with:

- a single orchestration service
- a tool registry
- multiple tools/capabilities
- an OpenAI-backed provider
- proposal confirmation handling
- GraphQL mutations for chat, confirm, and cancel

Main files:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentToolRegistry.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentToolRegistry.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/OpenAIResponsesAgentLLMProvider.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/OpenAIResponsesAgentLLMProvider.java)
- [`backend/application/syson-application-configuration/src/main/resources/schema/syson-agent.graphqls`](./backend/application/syson-application-configuration/src/main/resources/schema/syson-agent.graphqls)

### 4. First mutating capability

The first real capability is a modeling tool that prepares and executes creation of a `PartUsage`.

Main files:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/CreatePartUsageAgentTool.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/CreatePartUsageAgentTool.java)
- [`backend/services/syson-model-services/src/main/java/org/eclipse/syson/model/services/ModelMutationElementService.java`](./backend/services/syson-model-services/src/main/java/org/eclipse/syson/model/services/ModelMutationElementService.java)

### 5. First read-only capability

To prove the modular architecture is not only theoretical, a second tool was added. It performs a lightweight review on the selected element and reports naming/readiness hints.

Main file:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/GuidelineReviewAgentTool.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/GuidelineReviewAgentTool.java)

### 6. OpenAI is mandatory

The Agent does not fall back to heuristics anymore when no API key is configured. If no key is set, the Agent responds that it is unavailable.

Main files:

- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/AgentConversationService.java)
- [`backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/OpenAIResponsesAgentLLMProvider.java`](./backend/application/syson-application-configuration/src/main/java/org/eclipse/syson/application/agent/OpenAIResponsesAgentLLMProvider.java)

## Important bug fixes along the way

- Fixed the workbench selection integration so the Agent now reads the real global SysON selection instead of staying at `0 item(s) selected`.
- Fixed GraphQL proposal confirmation so the frontend only sends schema-valid proposal input on `confirm` and `cancel`.
- Fixed timeout/error paths in agent event handlers so backend failures no longer surface as hanging requests.
- Removed the misleading double-response behavior where the agent sounded optimistic before rejecting the missing selection.
- Improved the draft handling in the chat composer so the typed text is only cleared after a successful send.

## Current capabilities

### Mutating

- `modeling.create_part_usage`

Behavior:

- requires a valid selected container
- derives a reasonable part name from chat
- returns a proposal first
- only mutates after explicit confirmation

### Read-only

- `review.guidelines`

Behavior:

- works on the current selection
- does not create a proposal
- returns naming and target-readiness guidance

## Local development setup used for this milestone

### Frontend

- Vite dev server on `http://localhost:5173`

### Backend

- Spring Boot app on `http://localhost:8080`

### Database

- local Postgres container on `localhost:5433`

### OpenAI

- local ignored config in `dev.local.properties`
- startup helper in [`scripts/start-syson-dev.ps1`](./scripts/start-syson-dev.ps1)
- current default model: `gpt-4.1-mini`

## How to run locally

1. Create a local `dev.local.properties` based on [`dev.local.properties.example`](./dev.local.properties.example).
2. Set `syson.agent.llm.openai.api-key`.
3. Start the local infrastructure if needed.
4. Start the backend with [`scripts/start-syson-dev.ps1`](./scripts/start-syson-dev.ps1).
5. Start the frontend from `frontend/syson`.
6. Open `http://localhost:5173`.

## Verified milestone scenario

The following scenario worked end-to-end in the UI:

1. Open SysON.
2. Select a valid container such as a package.
3. Open the `Agent` workbench tab.
4. Enter `create a battery part`.
5. Receive a proposal card.
6. Click `Confirm`.
7. Receive the result message `Created PartUsage 'Battery'.`

## Remaining work before merge

- polish the chat copy and empty states a bit more
- exercise the new read-only review flow in the UI
- rebase the branch on top of `upstream/main`
- run the full merge-readiness pass after the rebase

## Branch state

Current feature branch:

- `feature/agent-workbench-panel`

Key milestone commit:

- `6537b9e7 Add agent workbench flow with proposal-based part creation`
