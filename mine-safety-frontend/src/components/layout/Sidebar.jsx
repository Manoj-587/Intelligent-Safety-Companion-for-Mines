import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Avatar from '@mui/material/Avatar';
import { Add, History, Person, ChevronRight } from '@mui/icons-material';
import DeveloperToggle from '../developer/DeveloperToggle';

const roles = ['Worker', 'Supervisor', 'Maintenance Engineer', 'Safety Officer'];

const Sidebar = ({ selectedRole, onRoleChange, developerEnabled, onToggleDeveloper }) => {
  return (
    <Drawer
      variant="permanent"
      PaperProps={{
        sx: {
          width: { xs: '100%', md: 300 },
          backgroundColor: 'primary.main',
          color: '#fff',
          borderRight: 'none',
          px: { xs: 2, md: 3 },
          py: 3,
          position: 'relative'
        }
      }}
      sx={{
        flexShrink: 0,
        width: { xs: '100%', md: 300 },
        '& .MuiDrawer-paper': {
          width: { xs: '100%', md: 300 }
        }
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 4 }}>
        <Avatar sx={{ bgcolor: 'secondary.main', width: 46, height: 46 }}>
          <Person />
        </Avatar>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 800 }}>
            Safety Companion
          </Typography>
          <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.78)' }}>
            Mining operations dashboard
          </Typography>
        </Box>
      </Box>

      <Button
        variant="contained"
        color="secondary"
        startIcon={<Add />}
        sx={{ width: '100%', mb: 3, color: 'primary.dark' }}
      >
        New Chat
      </Button>

      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle2" sx={{ textTransform: 'uppercase', mb: 1.5, color: 'rgba(255,255,255,0.75)' }}>
          Session history
        </Typography>
        <List sx={{ bgcolor: 'rgba(255,255,255,0.08)', borderRadius: 3, py: 1 }}>
          <ListItem disablePadding>
            <ListItemButton
              selected
              sx={{
                borderRadius: 3,
                px: 2,
                py: 1.5,
                '&.Mui-selected': {
                  backgroundColor: 'rgba(255,255,255,0.18)'
                },
                '&:hover': {
                  backgroundColor: 'rgba(255,255,255,0.14)'
                }
              }}
            >
              <ListItemIcon sx={{ color: '#fff', minWidth: 40 }}>
                <History />
              </ListItemIcon>
              <ListItemText
                primary="Current session"
                secondary="Live risk conversation"
                primaryTypographyProps={{ color: '#fff', fontWeight: 700 }}
                secondaryTypographyProps={{ color: 'rgba(255,255,255,0.72)', fontSize: '0.85rem' }}
              />
              <ChevronRight sx={{ color: 'rgba(255,255,255,0.65)' }} />
            </ListItemButton>
          </ListItem>
        </List>
      </Box>

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.18)', mb: 3 }} />

      <Box sx={{ mb: 3 }}>
        <FormControl fullWidth>
          <InputLabel sx={{ color: '#fff' }}>Role</InputLabel>
          <Select
            value={selectedRole}
            label="Role"
            onChange={(event) => onRoleChange(event.target.value)}
            sx={{
              color: '#fff',
              '.MuiOutlinedInput-notchedOutline': {
                borderColor: 'rgba(255,255,255,0.38)'
              },
              '.MuiSvgIcon-root': {
                color: '#fff'
              }
            }}
          >
            {roles.map((role) => (
              <MenuItem key={role} value={role}>
                {role}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Box sx={{ mt: 'auto', pt: 3, borderTop: '1px solid rgba(255,255,255,0.14)' }}>
        <DeveloperToggle enabled={developerEnabled} onChange={onToggleDeveloper} />
        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.70)', display: 'block', mt: 1 }}>
          Display backend diagnostics when developer mode is enabled.
        </Typography>
      </Box>
    </Drawer>
  );
};

export default Sidebar;
