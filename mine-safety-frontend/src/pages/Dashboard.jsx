import { useEffect, useState } from 'react';
import api from '../api';

export default function Dashboard() {
  const [stats, setStats] = useState({ mines: 0, zones: 0, alerts: 0, sensors: 0, risks: 0 });

  useEffect(() => {
    Promise.all([
      api.get('/mines'),
      api.get('/zones'),
      api.get('/alerts'),
      api.get('/sensor-data'),
      api.get('/risk-predictions'),
    ]).then(([mines, zones, alerts, sensors, risks]) => {
      setStats({
        mines: mines.data.length,
        zones: zones.data.length,
        alerts: alerts.data.length,
        sensors: sensors.data.length,
        risks: risks.data.length,
      });
    });
  }, []);

  const activeAlerts = stats.alerts;

  return (
    <div className="page">
      <h1>Dashboard</h1>
      <div className="stats-grid">
        <div className="stat-card"><div className="stat-icon">🏭</div><div className="stat-value">{stats.mines}</div><div className="stat-label">Mines</div></div>
        <div className="stat-card"><div className="stat-icon">📍</div><div className="stat-value">{stats.zones}</div><div className="stat-label">Zones</div></div>
        <div className="stat-card alert-card"><div className="stat-icon">🚨</div><div className="stat-value">{activeAlerts}</div><div className="stat-label">Alerts</div></div>
        <div className="stat-card"><div className="stat-icon">📡</div><div className="stat-value">{stats.sensors}</div><div className="stat-label">Sensor Readings</div></div>
        <div className="stat-card"><div className="stat-icon">⚠️</div><div className="stat-value">{stats.risks}</div><div className="stat-label">Risk Predictions</div></div>
      </div>
    </div>
  );
}
