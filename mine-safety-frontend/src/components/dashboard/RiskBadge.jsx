import Chip from '@mui/material/Chip';
import CircleIcon from '@mui/icons-material/Circle';

const labelMap = {
  low: 'LOW',
  medium: 'MEDIUM',
  high: 'HIGH'
};

const iconColorMap = {
  low: '#2e7d32',
  medium: '#f7b500',
  high: '#d32f2f'
};

const RiskBadge = ({ level = 'high' }) => {
  return (
    <Chip
      icon={<CircleIcon sx={{ color: iconColorMap[level] || iconColorMap.high, fontSize: 12 }} />}
      label={labelMap[level] || 'HIGH'}
      size="small"
      sx={{
        fontWeight: 700,
        letterSpacing: 0.7,
        backgroundColor: 'rgba(255,255,255,0.92)',
        color: 'text.primary'
      }}
    />
  );
};

export default RiskBadge;
