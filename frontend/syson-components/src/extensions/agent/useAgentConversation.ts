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

import { gql, useMutation } from '@apollo/client';
import { useMultiToast } from '@eclipse-sirius/sirius-components-core';
import { useState } from 'react';
import { AgentChatMessage, AgentMutationPayload, AgentProposal } from './agent.types';

const sendAgentMessageMutation = gql`
  mutation sendAgentMessage($input: SendAgentMessageInput!) {
    sendAgentMessage(input: $input) {
      __typename
      ... on ErrorPayload {
        messages {
          body
          level
        }
      }
      ... on AgentMessageSuccessPayload {
        reply {
          pendingConfirmation
          messages {
            id
            role
            content
            variant
          }
          proposal {
            proposalId
            toolId
            actionType
            targetObjectId
            targetLabel
            elementType
            summary
            argumentsJson
            fields {
              name
              value
            }
          }
        }
      }
    }
  }
`;

const confirmAgentProposalMutation = gql`
  mutation confirmAgentProposal($input: ConfirmAgentProposalInput!) {
    confirmAgentProposal(input: $input) {
      __typename
      ... on ErrorPayload {
        messages {
          body
          level
        }
      }
      ... on AgentProposalSuccessPayload {
        reply {
          pendingConfirmation
          messages {
            id
            role
            content
            variant
          }
          proposal {
            proposalId
            toolId
            actionType
            targetObjectId
            targetLabel
            elementType
            summary
            argumentsJson
            fields {
              name
              value
            }
          }
        }
      }
    }
  }
`;

const cancelAgentProposalMutation = gql`
  mutation cancelAgentProposal($input: CancelAgentProposalInput!) {
    cancelAgentProposal(input: $input) {
      __typename
      ... on ErrorPayload {
        messages {
          body
          level
        }
      }
      ... on AgentProposalSuccessPayload {
        reply {
          pendingConfirmation
          messages {
            id
            role
            content
            variant
          }
          proposal {
            proposalId
            toolId
            actionType
            targetObjectId
            targetLabel
            elementType
            summary
            argumentsJson
            fields {
              name
              value
            }
          }
        }
      }
    }
  }
`;

interface SendAgentMessageMutationData {
  sendAgentMessage: AgentMutationPayload;
}

interface ConfirmAgentProposalMutationData {
  confirmAgentProposal: AgentMutationPayload;
}

interface CancelAgentProposalMutationData {
  cancelAgentProposal: AgentMutationPayload;
}

export interface UseAgentConversationValue {
  messages: AgentChatMessage[];
  pendingProposal: AgentProposal | null;
  loading: boolean;
  sendMessage: (message: string) => Promise<boolean>;
  confirmProposal: () => Promise<void>;
  cancelProposal: () => Promise<void>;
}

const toAgentProposalInput = (proposal: AgentProposal) => ({
  proposalId: proposal.proposalId,
  toolId: proposal.toolId,
  actionType: proposal.actionType,
  targetObjectId: proposal.targetObjectId,
  targetLabel: proposal.targetLabel,
  elementType: proposal.elementType,
  summary: proposal.summary,
  argumentsJson: proposal.argumentsJson,
});

const toErrorMessages = (payload: AgentMutationPayload): AgentChatMessage[] => {
  if (payload.__typename !== 'ErrorPayload') {
    return payload.reply.messages;
  }

  return payload.messages.map((message, index) => ({
    id: `error-${index}-${crypto.randomUUID()}`,
    role: 'assistant',
    content: message.body,
    variant: 'error',
  }));
};

export const useAgentConversation = (
  editingContextId: string,
  selectedObjectIds: string[]
): UseAgentConversationValue => {
  const { addErrorMessage } = useMultiToast();
  const [messages, setMessages] = useState<AgentChatMessage[]>([
    {
      id: 'agent-welcome',
      role: 'assistant',
      content:
        'Describe what you want to do in SysON. I can already create a PartUsage through a safe proposal flow and run a lightweight guideline review on the current selection.',
      variant: 'text',
    },
  ]);
  const [pendingProposal, setPendingProposal] = useState<AgentProposal | null>(null);

  const [sendMutation, { loading: sendLoading }] =
    useMutation<SendAgentMessageMutationData>(sendAgentMessageMutation);
  const [confirmMutation, { loading: confirmLoading }] =
    useMutation<ConfirmAgentProposalMutationData>(confirmAgentProposalMutation);
  const [cancelMutation, { loading: cancelLoading }] =
    useMutation<CancelAgentProposalMutationData>(cancelAgentProposalMutation);

  const appendReplyPayload = (payload: AgentMutationPayload) => {
    const nextMessages = toErrorMessages(payload);
    setMessages((currentMessages) => [...currentMessages, ...nextMessages]);
    if (payload.__typename === 'ErrorPayload') {
      setPendingProposal(null);
    } else {
      setPendingProposal(payload.reply.pendingConfirmation ? payload.reply.proposal : null);
    }
  };

  const sendMessage = async (message: string) => {
    const sanitizedMessage = message.trim();
    if (!sanitizedMessage) {
      return false;
    }

    setMessages((currentMessages) => [
      ...currentMessages,
      {
        id: crypto.randomUUID(),
        role: 'user',
        content: sanitizedMessage,
        variant: 'text',
      },
    ]);

    try {
      const result = await sendMutation({
        variables: {
          input: {
            id: crypto.randomUUID(),
            editingContextId,
            message: sanitizedMessage,
            selectedObjectIds,
          },
        },
      });
      if (result.data) {
        appendReplyPayload(result.data.sendAgentMessage);
      }
      return true;
    } catch (error) {
      addErrorMessage('The agent request failed unexpectedly.');
      return false;
    }
  };

  const confirmProposal = async () => {
    if (!pendingProposal) {
      return;
    }
    try {
      const result = await confirmMutation({
        variables: {
          input: {
            id: crypto.randomUUID(),
            editingContextId,
            proposal: toAgentProposalInput(pendingProposal),
          },
        },
      });
      if (result.data) {
        appendReplyPayload(result.data.confirmAgentProposal);
      }
    } catch (error) {
      addErrorMessage('The proposal could not be applied.');
    }
  };

  const cancelProposal = async () => {
    if (!pendingProposal) {
      return;
    }
    try {
      const result = await cancelMutation({
        variables: {
          input: {
            id: crypto.randomUUID(),
            editingContextId,
            proposal: toAgentProposalInput(pendingProposal),
          },
        },
      });
      if (result.data) {
        appendReplyPayload(result.data.cancelAgentProposal);
      }
    } catch (error) {
      addErrorMessage('The proposal could not be cancelled cleanly.');
    }
  };

  return {
    messages,
    pendingProposal,
    loading: sendLoading || confirmLoading || cancelLoading,
    sendMessage,
    confirmProposal,
    cancelProposal,
  };
};
