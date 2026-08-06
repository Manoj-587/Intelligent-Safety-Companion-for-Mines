import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import RiskBadge from './RiskBadge';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { apiClient } from '../../services/api';

const RiskSummaryCard = () => {
  const [latestSensor, setLatestSensor] = useState(null);
  const [latestPrediction, setLatestPrediction] = useState(null);

  const fetchDashboardData = async () => {
    try {
      const [sensorRes, predRes] = await Promise.allSettled([
        apiClient.get('/api/sensors/latest'),
        apiClient.get('/api/predictions/latest')
      ]);

      if (sensorRes.status === 'fulfilled' && sensorRes.value?.data) {
        setLatestSensor(sensorRes.value.data);
      }
      if (predRes.status === 'fulfilled' && predRes.value?.data) {
        setLatestPrediction(predRes.value.data);
      }
    } catch {
      // Ignore background refresh errors
    }
  };

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 5000);
    return () => clearInterval(interval);
  }, []);

  const riskLevel = (latestPrediction?.riskLevel || 'LOW').toLowerCase();
  const isSafe = riskLevel === 'low' || riskLevel === 'safe';

  return (
    <Paper sx={{ p: 3.5, display: 'flex', flexDirection: 'column', gap: 2.25 }} elevation={2}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 800 }}>
            Risk Summary
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Live underground sensor data & AI risk level
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isSafe ? <CheckCircleIcon color="success" fontSize="small" /> : <WarningAmberIcon color="warning" fontSize="small" />}
          <RiskBadge level={riskLevel} />
        </Box>
      </Box>

      <Box sx={{ p: 2.5, bgcolor: 'grey.50', borderRadius: 3 }}>
        <Typography variant="caption" sx={{ fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.6, color: 'text.secondary' }}>
          Live Sensor Metrics
        </Typography>
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 1, mt: 1 }}>
          <Typography variant="body2" sx={{ fontWeight: 700 }}>
            Methane: <span style={{ color: latestSensor?.methane >= 2.0 ? '#d32f2f' : '#2e7d32' }}>{latestSensor?.methane ?? '--'}%</span>
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 700 }}>
            CO: <span>{latestSensor?.carbonMonoxide ?? '--'} ppm</span>
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 700 }}>
            Temp: <span>{latestSensor?.temperature ?? '--'}°C</span>
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 700 }}>
            Oxygen: <span>{latestSensor?.oxygen ?? '--'}%</span>
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 700 }}>
            Airflow: <span>{latestSensor?.airflow ?? '--'} m/s</span>
          </Typography>
          <Typography variant="body2" sx={{ fontWeight: 700 }}>
            Pressure: <span>{latestSensor?.pressure ?? '--'} kPa</span>
          </Typography>
        </Box>
      </Box>

      {latestPrediction && (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 700, mb: 0.5, letterSpacing: 0.25 }}>
            Assessment Status
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
            {latestPrediction.riskLevel === 'HIGH' || latestPrediction.riskLevel === 'CRITICAL'
              ? 'Elevated hazard detected — safety protocol active.'
              : latestPrediction.riskLevel === 'MEDIUM'
              ? 'Warning levels observed — close monitoring advised.'
              : 'Mine operations running within safe parameters.'}
          </Typography>
        </Box>
      )}
    </Paper>
  );
};

export default RiskSummaryCard;
