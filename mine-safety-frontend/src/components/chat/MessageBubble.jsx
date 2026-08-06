import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Alert from '@mui/material/Alert';
import MarkdownMessage from './MarkdownMessage';
import Timestamp from './Timestamp';
import RoleChip from './RoleChip';

const MessageBubble = ({ message }) => {
  const isUser = message.role === 'user';
  const showAlert = message.status === 'blocked' || message.status === 'degraded';

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: isUser ? 'flex-end' : 'flex-start',
        gap: 1,
        width: '100%',
        animation: 'fadeIn 240ms ease-out',
        '@keyframes fadeIn': {
          '0%': { opacity: 0, transform: 'translateY(12px)' },
          '100%': { opacity: 1, transform: 'translateY(0)' }
        }
      }}
    >
      <Paper
        sx={{
          maxWidth: '90%',
          p: 2.25,
          backgroundColor: isUser ? 'primary.main' : 'grey.100',
          color: isUser ? '#fff' : 'text.primary',
          boxShadow: isUser ? '0 12px 30px rgba(31, 55, 82, 0.10)' : '0 10px 24px rgba(15, 36, 60, 0.04)',
          borderRadius: 28,
          borderTopRightRadius: isUser ? 8 : 28,
          borderTopLeftRadius: isUser ? 28 : 8,
          alignSelf: isUser ? 'flex-end' : 'flex-start'
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1, gap: 1 }}>
          <RoleChip role={message.role} />
          <Timestamp value={message.timestamp} />
        </Box>
        <MarkdownMessage content={message.content} />
      </Paper>

      {showAlert && (
        <Alert
          severity={message.status === 'blocked' ? 'error' : 'warning'}
          sx={{ width: '100%', maxWidth: '90%', borderRadius: 3, fontSize: '0.92rem' }}
        >
          {message.status === 'blocked'
            ? 'This response was blocked to meet safety policies.'
            : 'The assistant response may be degraded due to current system status.'}
        </Alert>
      )}
    </Box>
  );
};

export default MessageBubble;
