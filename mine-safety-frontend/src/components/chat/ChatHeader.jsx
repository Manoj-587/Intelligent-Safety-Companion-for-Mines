import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';

const assistantLabelMap = {
  normal: 'Assistant connected',
  degraded: 'Degraded response',
  blocked: 'Blocked response'
};

const assistantColorMap = {
  normal: 'success',
  degraded: 'warning',
  blocked: 'error'
};

const connectionLabelMap = {
  connected: 'Backend connected',
  connecting: 'Connecting...',
  offline: 'Backend offline'
};

const connectionColorMap = {
  connected: 'success',
  connecting: 'warning',
  offline: 'error'
};

const ChatHeader = ({ sessionName, role, assistantStatus, connectionStatus }) => {
  return (
    <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: 'flex-start', gap: 2, mb: 2 }}>
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          {sessionName}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Role: {role}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        <Chip label={connectionLabelMap[connectionStatus]} color={connectionColorMap[connectionStatus]} />
        <Chip label={assistantLabelMap[assistantStatus]} color={assistantColorMap[assistantStatus]} />
      </Box>
    </Box>
  );
};

export default ChatHeader;
