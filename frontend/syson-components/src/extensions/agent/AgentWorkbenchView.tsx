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

import { useSelection, WorkbenchViewComponentProps, WorkbenchViewHandle } from '@eclipse-sirius/sirius-components-core';
import SmartToyOutlinedIcon from '@mui/icons-material/SmartToyOutlined';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import SendRoundedIcon from '@mui/icons-material/SendRounded';
import { forwardRef, KeyboardEvent, useImperativeHandle, useState } from 'react';
import { makeStyles } from 'tss-react/mui';
import { AgentMessageList } from './AgentMessageList';
import { AgentProposalCard } from './AgentProposalCard';
import { useAgentConversation } from './useAgentConversation';

const useAgentWorkbenchViewStyles = makeStyles()((theme) => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    padding: theme.spacing(1.5),
    gap: theme.spacing(1.5),
    backgroundColor: theme.palette.background.paper,
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(1),
  },
  icon: {
    color: theme.palette.primary.main,
  },
  section: {
    padding: theme.spacing(1.5),
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.action.hover,
  },
  hintList: {
    margin: theme.spacing(0.75, 0, 0),
    paddingLeft: theme.spacing(2.25),
  },
  transcript: {
    flex: 1,
    minHeight: 0,
    overflowY: 'auto',
    paddingRight: theme.spacing(0.5),
  },
  composer: {
    display: 'grid',
    gridTemplateColumns: '1fr auto',
    gap: theme.spacing(1),
    alignItems: 'end',
  },
  code: {
    overflowWrap: 'anywhere',
    fontFamily: 'monospace',
  },
}));

export const AgentWorkbenchView = forwardRef<WorkbenchViewHandle, WorkbenchViewComponentProps>(
  ({ id, editingContextId }, ref) => {
    const { classes } = useAgentWorkbenchViewStyles();
    const { selection } = useSelection();
    const [draftMessage, setDraftMessage] = useState('');
    const selectedObjectIds = selection.entries.map((entry) => entry.id);
    const { messages, pendingProposal, loading, sendMessage, confirmProposal, cancelProposal } =
      useAgentConversation(editingContextId, selectedObjectIds);

    useImperativeHandle(
      ref,
      () => ({
        id,
        getWorkbenchViewConfiguration: () => ({}),
        applySelection: null,
      }),
      [id]
    );

    const firstSelectedObjectId = selection.entries[0]?.id ?? null;
    const handleSubmit = async () => {
      const nextMessage = draftMessage;
      const sent = await sendMessage(nextMessage);
      if (sent) {
        setDraftMessage('');
      }
    };

    const handleKeyDown = async (event: KeyboardEvent<HTMLDivElement>) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        await handleSubmit();
      }
    };

    return (
      <Box className={classes.root} data-testid="agent-workbench-view">
        <div className={classes.header}>
          <SmartToyOutlinedIcon className={classes.icon} />
          <div>
            <Typography variant="h6">Agent</Typography>
            <Typography variant="body2" color="text.secondary">
              Integrated assistant workspace for SysON.
            </Typography>
          </div>
        </div>

        <Box className={classes.section}>
          <Typography variant="subtitle2">Editing context</Typography>
          <Typography variant="body2" color="text.secondary" className={classes.code}>
            {editingContextId}
          </Typography>
        </Box>

        <Box className={classes.section}>
          <Typography variant="subtitle2">Current selection</Typography>
          <Typography variant="body2" color="text.secondary">
            {selection.entries.length} item(s) selected
          </Typography>
          <Typography variant="body2" color="text.secondary" className={classes.code}>
            {firstSelectedObjectId ?? 'No element selected yet'}
          </Typography>
        </Box>

        <Box className={classes.section}>
          <Typography variant="subtitle2">Available now</Typography>
          <Typography component="div" variant="body2" color="text.secondary">
            <ul className={classes.hintList}>
              <li>Create a `PartUsage` through `ask -&gt; propose -&gt; confirm -&gt; apply`.</li>
              <li>Run a lightweight guideline review on the current selection.</li>
            </ul>
          </Typography>
        </Box>

        {pendingProposal ? (
          <AgentProposalCard
            proposal={pendingProposal}
            disabled={loading}
            onConfirm={confirmProposal}
            onCancel={cancelProposal}
          />
        ) : null}

        <Divider />

        <Box className={classes.transcript}>
          <AgentMessageList messages={messages} />
        </Box>

        <div className={classes.composer}>
          <TextField
            minRows={3}
            maxRows={8}
            multiline
            value={draftMessage}
            onChange={(event) => setDraftMessage(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Try: create a battery part or review the selected element"
          />
          <IconButton color="primary" onClick={handleSubmit} disabled={loading || !draftMessage.trim()}>
            {loading ? <CircularProgress size={20} /> : <SendRoundedIcon />}
          </IconButton>
        </div>
      </Box>
    );
  }
);

AgentWorkbenchView.displayName = 'AgentWorkbenchView';
