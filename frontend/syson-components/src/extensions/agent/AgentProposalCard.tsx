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

import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { makeStyles } from 'tss-react/mui';
import { AgentProposal } from './agent.types';

const useAgentProposalCardStyles = makeStyles()((theme) => ({
  card: {
    padding: theme.spacing(1.5),
    border: `1px solid ${theme.palette.divider}`,
    backgroundColor: theme.palette.background.default,
  },
  value: {
    fontFamily: 'monospace',
    overflowWrap: 'anywhere',
  },
}));

interface AgentProposalCardProps {
  proposal: AgentProposal;
  disabled: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const AgentProposalCard = ({
  proposal,
  disabled,
  onConfirm,
  onCancel,
}: AgentProposalCardProps) => {
  const { classes } = useAgentProposalCardStyles();

  return (
    <Paper className={classes.card} elevation={0}>
      <Stack spacing={1.5}>
        <div>
          <Typography variant="subtitle1">Pending proposal</Typography>
          <Typography variant="body2" color="text.secondary">
            {proposal.summary}
          </Typography>
        </div>

        <Stack spacing={0.75}>
          {proposal.fields.map((field) => (
            <div key={`${proposal.proposalId}-${field.name}`}>
              <Typography variant="caption" color="text.secondary">
                {field.name}
              </Typography>
              <Typography variant="body2" className={classes.value}>
                {field.value}
              </Typography>
            </div>
          ))}
        </Stack>

        <Stack direction="row" spacing={1}>
          <Button variant="contained" onClick={onConfirm} disabled={disabled}>
            Confirm
          </Button>
          <Button variant="outlined" color="inherit" onClick={onCancel} disabled={disabled}>
            Cancel
          </Button>
        </Stack>
      </Stack>
    </Paper>
  );
};
