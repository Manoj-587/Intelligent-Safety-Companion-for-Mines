import { useState } from 'react';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import SendIcon from '@mui/icons-material/Send';

const ChatInput = ({ disabled = false, onSend }) => {
  const [value, setValue] = useState('');

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!value.trim()) return;
    onSend(value.trim());
    setValue('');
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 2 }}>
      <TextField
        fullWidth
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder="Ask about current mine safety conditions..."
        disabled={disabled}
        size="small"
        sx={{ bgcolor: 'white', borderRadius: 3 }}
        InputProps={{
          sx: {
            borderRadius: 3,
            bgcolor: 'grey.50'
          }
        }}
      />
      <IconButton type="submit" color="primary" disabled={disabled || !value.trim()} sx={{ bgcolor: disabled ? 'grey.200' : 'secondary.main', color: disabled ? 'text.disabled' : '#1f2632' }}>
        <SendIcon />
      </IconButton>
    </Box>
  );
};

export default ChatInput;
