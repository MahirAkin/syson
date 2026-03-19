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

import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { makeStyles } from 'tss-react/mui';
import { AgentChatMessage } from './agent.types';

const useAgentMessageListStyles = makeStyles()((theme) => ({
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: theme.spacing(1.25),
  },
  bubble: {
    maxWidth: '92%',
    padding: theme.spacing(1.25),
    borderRadius: theme.spacing(1.5),
    whiteSpace: 'pre-wrap',
  },
  assistant: {
    alignSelf: 'flex-start',
    backgroundColor: theme.palette.action.hover,
  },
  user: {
    alignSelf: 'flex-end',
    backgroundColor: theme.palette.primary.light,
    color: theme.palette.primary.contrastText,
  },
  status: {
    borderLeft: `3px solid ${theme.palette.info.main}`,
  },
  result: {
    borderLeft: `3px solid ${theme.palette.success.main}`,
  },
  error: {
    borderLeft: `3px solid ${theme.palette.error.main}`,
  },
}));

interface AgentMessageListProps {
  messages: AgentChatMessage[];
}

export const AgentMessageList = ({ messages }: AgentMessageListProps) => {
  const { classes, cx } = useAgentMessageListStyles();

  return (
    <Box className={classes.list}>
      {messages.map((message) => (
        <Box
          key={message.id}
          className={cx(
            classes.bubble,
            message.role === 'user' ? classes.user : classes.assistant,
            message.variant === 'status' && classes.status,
            message.variant === 'result' && classes.result,
            message.variant === 'error' && classes.error
          )}>
          <Typography variant="body2">{message.content}</Typography>
        </Box>
      ))}
    </Box>
  );
};
