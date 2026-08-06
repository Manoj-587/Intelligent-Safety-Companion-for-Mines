import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';

const DeveloperToggle = ({ enabled, onChange }) => {
  return <FormControlLabel control={<Switch checked={enabled} onChange={onChange} color="secondary" />} label="Developer mode" />;
};

export default DeveloperToggle;
