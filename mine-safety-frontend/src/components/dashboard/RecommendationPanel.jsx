import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import AirIcon from '@mui/icons-material/Air';
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment';
import AssignmentIcon from '@mui/icons-material/Assignment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import SecurityIcon from '@mui/icons-material/Security';
import { apiClient } from '../../services/api';

const defaultRecommendations = [
  {
    title: 'Continue Normal Operations',
    description: 'Maintain standard production workflow with routine safety awareness.',
    reason: 'All sensor readings are operating within safe parameters.',
    priority: 'LOW',
    category: 'PREVENTIVE',
    color: '#2e7d32',
    sensorTrigger: 'Routine Schedule',
    iconName: 'CheckIcon'
  },
  {
    title: 'Monitor Gas Levels Every 30 Minutes',
    description: 'Conduct regular multi-gas monitor checks across active mine working faces.',
    reason: 'Routine environmental safety compliance check.',
    priority: 'LOW',
    category: 'MONITORING',
    color: '#0288d1',
    sensorTrigger: 'Routine Schedule',
    iconName: 'ReportIcon'
  },
  {
    title: 'Schedule Routine Ventilation Inspection',
    description: 'Verify main fan differential pressure and stoppings during shift change.',
    reason: 'Periodic ventilation maintenance schedule.',
    priority: 'LOW',
    category: 'INSPECTION',
    color: '#0288d1',
    sensorTrigger: 'Routine Schedule',
    iconName: 'VentilationIcon'
  }
];

const renderIcon = (iconName, color) => {
  const sx = { color: color || 'primary.main', fontSize: 20 };
  switch (iconName) {
    case 'FireIcon':
      return <LocalFireDepartmentIcon sx={sx} />;
    case 'VentilationIcon':
      return <AirIcon sx={sx} />;
    case 'WarningIcon':
      return <WarningAmberIcon sx={sx} />;
    case 'CheckIcon':
      return <CheckCircleIcon sx={sx} />;
    case 'ShieldIcon':
      return <SecurityIcon sx={sx} />;
    case 'ReportIcon':
    default:
      return <AssignmentIcon sx={sx} />;
  }
};

const getCategoryColor = (category, priority) => {
  if (category === 'IMMEDIATE' || category === 'EMERGENCY' || priority === 'HIGH') return '#d32f2f';
  if (category === 'INSPECTION' || category === 'MONITORING' || priority === 'MEDIUM') return '#ed6c02';
  return '#0288d1';
};

const RecommendationPanel = () => {
  const [items, setItems] = useState(defaultRecommendations);

  const fetchRecommendations = async () => {
    try {
      const response = await apiClient.get('/api/predictions/latest');
      const data = response.data;

      if (data?.detailedRecommendations && data.detailedRecommendations.length > 0) {
        setItems(data.detailedRecommendations);
      } else if (data?.recommendation) {
        const rawRec = data.recommendation;
        const parts = rawRec.split(';').map(p => p.trim()).filter(Boolean);

        if (parts.length > 0) {
          const parsed = parts.slice(0, 6).map((p) => {
            let category = 'PREVENTIVE';
            let priority = 'LOW';
            let title = p;
            let description = p;
            let reason = 'Environmental threshold check.';
            let sensorTrigger = 'Mine Sensors';

            if (p.includes('[IMMEDIATE]')) {
              category = 'IMMEDIATE';
              priority = 'HIGH';
            } else if (p.includes('[EMERGENCY]')) {
              category = 'EMERGENCY';
              priority = 'HIGH';
            } else if (p.includes('[INSPECTION]')) {
              category = 'INSPECTION';
              priority = 'MEDIUM';
            } else if (p.includes('[MONITORING]')) {
              category = 'MONITORING';
              priority = 'MEDIUM';
            } else if (p.includes('[MAINTENANCE]')) {
              category = 'MAINTENANCE';
              priority = 'LOW';
            }

            if (p.includes('Title:')) {
              const match = p.match(/\[(.*?)\] (.*?): (.*?) \(Reason: (.*?); Triggered by: (.*?); Priority: (.*?)\)/);
              if (match) {
                category = match[1];
                title = match[2];
                description = match[3];
                reason = match[4];
                sensorTrigger = match[5];
                priority = match[6];
              }
            }

            return {
              title,
              description,
              reason,
              priority,
              category,
              color: getCategoryColor(category, priority),
              sensorTrigger,
              iconName: priority === 'HIGH' ? 'FireIcon' : category === 'INSPECTION' ? 'ReportIcon' : 'VentilationIcon'
            };
          });

          setItems(parsed);
        }
      }
    } catch {
      // Keep existing items on network error
    }
  };

  useEffect(() => {
    fetchRecommendations();
    const interval = setInterval(fetchRecommendations, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Paper sx={{ p: 3.5, display: 'grid', gap: 2 }} elevation={2}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6" sx={{ fontWeight: 800 }}>
          Recommendations
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700 }}>
          Max 6 • Priority Ordered
        </Typography>
      </Box>

      <Box sx={{ display: 'grid', gap: 1.5 }}>
        {items.map((item, idx) => (
          <Paper key={`${item.title}-${idx}`} sx={{ p: 2.25, borderRadius: 3, bgcolor: 'grey.50', borderLeft: `4px solid ${item.color || getCategoryColor(item.category, item.priority)}` }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 1.5, flexWrap: 'wrap', mb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {renderIcon(item.iconName, item.color)}
                <Typography variant="subtitle2" sx={{ fontWeight: 800 }}>
                  {item.title}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', gap: 0.75 }}>
                <Chip
                  label={item.category || 'PREVENTIVE'}
                  size="small"
                  sx={{
                    fontWeight: 800,
                    fontSize: '0.65rem',
                    borderRadius: 1.5,
                    bgcolor: item.color || getCategoryColor(item.category, item.priority),
                    color: '#fff'
                  }}
                />
                <Chip
                  label={`PRIORITY: ${item.priority || 'LOW'}`}
                  size="small"
                  variant="outlined"
                  sx={{
                    fontWeight: 800,
                    fontSize: '0.65rem',
                    borderRadius: 1.5,
                    borderColor: item.color || getCategoryColor(item.category, item.priority),
                    color: item.color || getCategoryColor(item.category, item.priority)
                  }}
                />
              </Box>
            </Box>

            <Typography variant="body2" color="text.primary" sx={{ mb: 1, lineHeight: 1.5 }}>
              {item.description}
            </Typography>

            <Box sx={{ p: 1.25, bgcolor: 'background.paper', borderRadius: 2, display: 'grid', gap: 0.5 }}>
              <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary' }}>
                💡 <strong>Reason:</strong> {item.reason}
              </Typography>
              {item.sensorTrigger && (
                <Typography variant="caption" sx={{ fontWeight: 700, color: 'primary.main' }}>
                  ⚡ <strong>Triggered by:</strong> {item.sensorTrigger}
                </Typography>
              )}
            </Box>
          </Paper>
        ))}
      </Box>
    </Paper>
  );
};

export default RecommendationPanel;
