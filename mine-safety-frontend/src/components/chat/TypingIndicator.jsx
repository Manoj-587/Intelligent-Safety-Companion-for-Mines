import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

const TypingIndicator = () => {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, px: 1, py: 1.5, bgcolor: 'grey.100', borderRadius: 4 }}>
      <Box sx={{ display: 'flex', gap: 0.75, alignItems: 'center' }}>
        {['dot1', 'dot2', 'dot3'].map((dot) => (
          <Box
            key={dot}
            sx={{
              width: 9,
              height: 9,
              bgcolor: 'secondary.main',
              borderRadius: '50%',
              animation: `${dot} 1.2s infinite ease-in-out`
            }}
          />
        ))}
      </Box>
      <Typography variant="caption" color="text.secondary">
        Assistant is typing...
      </Typography>
      <style>{`
        @keyframes dot1 { 0%, 100% { opacity: 0.2; transform: translateY(0); } 50% { opacity: 1; transform: translateY(-3px); } }
        @keyframes dot2 { 0%, 100% { opacity: 0.2; transform: translateY(0); } 50% { opacity: 1; transform: translateY(-3px); } }
        @keyframes dot3 { 0%, 100% { opacity: 0.2; transform: translateY(0); } 50% { opacity: 1; transform: translateY(-3px); } }
      `}</style>
    </Box>
  );
};

export default TypingIndicator;
