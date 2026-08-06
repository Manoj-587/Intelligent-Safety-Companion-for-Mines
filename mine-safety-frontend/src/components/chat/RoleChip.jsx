import Chip from '@mui/material/Chip';

const labelMap = {
  user: 'User',
  assistant: 'Assistant',
  system: 'System'
};

const colorMap = {
  user: 'primary',
  assistant: 'secondary',
  system: 'default'
};

const RoleChip = ({ role }) => {
  return <Chip label={labelMap[role] || role} size="small" color={colorMap[role] || 'default'} sx={{ textTransform: 'none' }} />;
};

export default RoleChip;
