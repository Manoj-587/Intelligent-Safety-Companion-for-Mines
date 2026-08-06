import Typography from '@mui/material/Typography';

const Timestamp = ({ value }) => {
  const time = new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <Typography variant="caption" color="text.secondary">
      {time}
    </Typography>
  );
};

export default Timestamp;
