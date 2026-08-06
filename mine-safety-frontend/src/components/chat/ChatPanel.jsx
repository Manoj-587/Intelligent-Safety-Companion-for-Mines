import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import Skeleton from '@mui/material/Skeleton';
import Button from '@mui/material/Button';
import ChatHeader from './ChatHeader';
import MessageList from './MessageList';
import ChatInput from './ChatInput';
import TypingIndicator from './TypingIndicator';
import { useChat } from '../../hooks/useChat';

const ChatPanel = ({ roleLabel, backendRole, developerEnabled, onDiagnosticsUpdate }) => {
  const { messages, isTyping, status, isLoading, error, connectionStatus, developerDiagnostics, sendMessage, setAutoScrollRef } = useChat(backendRole);
  const [recentMessage, setRecentMessage] = useState(null);

  useEffect(() => {
    onDiagnosticsUpdate(developerDiagnostics);
  }, [developerDiagnostics, onDiagnosticsUpdate]);

  const handleSend = async (message) => {
    setRecentMessage(message);
    await sendMessage(message);
  };

  const handleRetry = async () => {
    if (recentMessage) {
      await sendMessage(recentMessage);
    }
  };

  return (
    <Paper sx={{ p: { xs: 2, md: 3 }, minHeight: 520, display: 'flex', flexDirection: 'column', gap: 2 }} elevation={2}>
      <ChatHeader
        sessionName="Mine Safety Chat"
        role={roleLabel}
        assistantStatus={status}
        connectionStatus={connectionStatus}
      />
      {error ? (
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={handleRetry}>
              Retry
            </Button>
          }
          sx={{ borderRadius: 2 }}
        >
          {error.includes('timed out')
            ? 'The backend request timed out. Please try again.'
            : 'Offline mode detected. Check your connection and retry.'}
        </Alert>
      ) : null}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
        {messages.length === 0 && isLoading ? (
          <Box sx={{ display: 'grid', gap: 2 }}>
            <Skeleton variant="rounded" height={90} />
            <Skeleton variant="rounded" height={90} />
          </Box>
        ) : messages.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: 'center', bgcolor: 'grey.50', border: '1px dashed', borderColor: 'grey.300' }}>
            <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>
              Start a safety conversation
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Ask the companion about risk, procedures, or equipment safety and receive prioritized guidance.
            </Typography>
          </Paper>
        ) : (
          <MessageList messages={messages} autoScrollRef={setAutoScrollRef} />
        )}
        {isTyping && <TypingIndicator />}
      </Box>
      <ChatInput disabled={isLoading || status === 'blocked' || connectionStatus === 'offline'} onSend={handleSend} />
    </Paper>
  );
};

export default ChatPanel;
