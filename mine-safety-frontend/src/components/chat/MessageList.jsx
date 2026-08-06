import Box from '@mui/material/Box';
import MessageBubble from './MessageBubble';

const MessageList = ({ messages, autoScrollRef }) => {
  return (
    <Box
      ref={autoScrollRef}
      sx={{
        height: '100%',
        overflowY: 'auto',
        px: 1,
        display: 'flex',
        flexDirection: 'column',
        gap: 1.75,
        scrollBehavior: 'smooth',
        '&::-webkit-scrollbar': {
          width: 8
        },
        '&::-webkit-scrollbar-thumb': {
          backgroundColor: 'rgba(31, 55, 82, 0.14)',
          borderRadius: 999
        }
      }}
    >
      {messages.map((message) => (
        <MessageBubble key={message.id} message={message} />
      ))}
    </Box>
  );
};

export default MessageList;
