import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Line } from 'react-chartjs-2';
import {
  AlertCircle, Activity, MapPin, Radio, ShieldAlert, TrendingUp,
  HardDrive, Users, CheckCircle, Eye, Shield
} from 'lucide-react';
import api from '../api';
import { useAuth } from '../AuthContext';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const cardVariant = {
  hidden: { opacity: 0, y: 40 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

function StatCard({ icon: Icon, value, label, color = 'text-primary', pulse = false }) {
  return (
    <motion.div className="stat-card" variants={cardVariant}>
      <motion.div className="stat-icon" whileHover={{ scale: 1.1 }}>
        <Icon className={`w-12 h-12 mx-auto ${color} ${pulse ? 'animate-pulse' : ''}`} />
      </motion.div>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </motion.div>
  );
}

function MineRiskChart({ mine, sensors }) {
  const mineSensors = sensors.filter(s => s.mine?.id === mine.id).slice(-20);
  const chartData = {
    labels: mineSensors.map((_, i) => `T-${mineSensors.length - i}`),
    datasets: [{
      label: 'Risk Score',
      data: mineSensors.map(s => s.riskScore || 0),
      borderColor: 'rgb(79,142,247)',
      backgroundColor: 'rgba(79,142,247,0.1)',
      tension: 0.4,
      pointBackgroundColor: mineSensors.map(s =>
        s.riskLevel === 'CRITICAL' ? '#e05252' : s.riskLevel === 'WARNING' ? '#f5a623' : '#3ecf8e'
      ),
    }],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: `${mine.mineName} — Risk Trend` },
    },
    scales: { y: { beginAtZero: true } },
  };
  return (
    <motion.div
      className="stat-card"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      style={{ height: 300, padding: '1.5rem' }}
    >
      {mineSensors.length === 0
        ? <div style={{ display: 'grid', placeItems: 'center', height: '100%', opacity: 0.4 }}>No sensor data for {mine.mineName}</div>
        : <Line data={chartData} options={options} />
      }
    </motion.div>
  );
}

// ─── ADMIN DASHBOARD ────────────────────────────────────────────────────────
function AdminDashboard() {
  const [stats, setStats] = useState({ mines: 0, zones: 0, users: 0, alerts: 0, sensors: 0, criticalRisks: 0 });
  const [mines, setMines] = useState([]);
  const [allSensors, setAllSensors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [simMsg, setSimMsg] = useState('');

  const triggerSim = async () => {
    try {
      await api.post('/simulator/trigger');
      setSimMsg('✓ Simulation cycle triggered');
      setTimeout(() => setSimMsg(''), 3000);
      fetchData();
    } catch { setSimMsg('No zones found — add a mine & zone first'); setTimeout(() => setSimMsg(''), 4000); }
  };

  const fetchData = async () => {
    try {
      const [minesRes, zonesRes, usersRes, alertsRes, sensorsRes, risksRes] = await Promise.all([
        api.get('/mines'),
        api.get('/zones'),
        api.get('/users'),
        api.get('/alerts'),
        api.get('/sensor-data'),
        api.get('/risk-predictions'),
      ]);
      setMines(minesRes.data);
      setAllSensors(sensorsRes.data);
      setStats({
        mines: minesRes.data.length,
        zones: zonesRes.data.length,
        users: usersRes.data.length,
        alerts: alertsRes.data.filter(a => a.status === 'ACTIVE').length,
        sensors: sensorsRes.data.length,
        criticalRisks: risksRes.data.filter(r => r.predictedRisk === 'CRITICAL').length,
      });
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); const t = setInterval(fetchData, 10000); return () => clearInterval(t); }, []);

  if (loading) return <div className="page" style={{ display: 'grid', placeItems: 'center', height: '60vh' }}><div className="loading" style={{ width: 50, height: 50 }} /></div>;

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="page-header">
        <h1>Admin Dashboard</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {simMsg && <span style={{ fontSize: '0.85rem', color: '#3ecf8e' }}>{simMsg}</span>}
          <button onClick={triggerSim} style={{ fontSize: '0.85rem', padding: '0.5rem 1rem' }}>⚡ Trigger Simulation</button>
          <motion.div animate={{ scale: [1, 1.05, 1] }} transition={{ duration: 2, repeat: Infinity }}>
            <Shield size={28} />
          </motion.div>
        </div>
      </div>

      <motion.div className="stats-grid" variants={{ visible: { transition: { staggerChildren: 0.15 } } }} initial="hidden" animate="visible">
        <StatCard icon={HardDrive} value={stats.mines} label="Total Mines" color="text-primary" />
        <StatCard icon={MapPin} value={stats.zones} label="Total Zones" color="text-success" />
        <StatCard icon={Users} value={stats.users} label="Total Users" color="text-primary" />
        <StatCard icon={Radio} value={stats.sensors} label="Sensor Readings" color="text-warning" />
        <StatCard icon={ShieldAlert} value={stats.alerts} label="Active Alerts" color={stats.alerts > 0 ? 'text-danger' : 'text-success'} pulse={stats.alerts > 0} />
        <StatCard icon={TrendingUp} value={stats.criticalRisks} label="Critical Risks" color={stats.criticalRisks > 0 ? 'text-danger' : 'text-success'} pulse={stats.criticalRisks > 0} />
      </motion.div>

      <div style={{ marginTop: '2rem' }}>
        <h2 style={{ marginBottom: '1rem', fontSize: '1.1rem', opacity: 0.8 }}>Risk Trend — Per Mine</h2>
        {mines.length === 0 ? (
          <div style={{ textAlign: 'center', opacity: 0.4, padding: '2rem' }}>No mines found. Add mines first.</div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
            {mines.map(mine => (
              <MineRiskChart key={mine.id} mine={mine} sensors={allSensors} />
            ))}
          </div>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '2rem', marginTop: '2rem' }}>
        <motion.div className="stat-card" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>System Status</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem' }}>
            {[
              { label: 'Active Mines', value: stats.mines, danger: false },
              { label: 'Monitored Zones', value: stats.zones, danger: false },
              { label: 'Active Alerts', value: stats.alerts, danger: stats.alerts > 0 },
              { label: 'Critical Risks', value: stats.criticalRisks, danger: stats.criticalRisks > 0 },
            ].map(({ label, value, danger }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.75rem', background: danger ? 'rgba(224,82,82,0.1)' : 'rgba(255,255,255,0.05)', borderRadius: 8 }}>
                <span>{label}</span><strong style={{ color: danger ? '#e05252' : '#3ecf8e' }}>{value}</strong>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.85rem', opacity: 0.5 }}>
        Auto-refreshes every 10s · Last update: {new Date().toLocaleTimeString()}
      </p>
    </motion.div>
  );
}

// ─── SUPERVISOR DASHBOARD ────────────────────────────────────────────────────
function SupervisorDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState({ activeAlerts: 0, warningAlerts: 0, criticalRisks: 0, sensors: 0 });
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [mineSensors, setMineSensors] = useState([]);
  const [assignedMine, setAssignedMine] = useState(null);
  const [loading, setLoading] = useState(true);
  const [simMsg, setSimMsg] = useState('');

  const triggerSim = async () => {
    try {
      await api.post('/simulator/trigger');
      setSimMsg('✓ Triggered');
      setTimeout(() => setSimMsg(''), 3000);
      fetchData();
    } catch { setSimMsg('Failed'); setTimeout(() => setSimMsg(''), 3000); }
  };

  const fetchData = async () => {
    try {
      const mineId = user?.assignedMineId;

      const [alertsRes, sensorsRes, risksRes] = await Promise.all([
        api.get('/alerts'),
        mineId ? api.get(`/sensor-data/by-mine/${mineId}`) : api.get('/sensor-data'),
        mineId ? api.get(`/risk-predictions/by-mine/${mineId}`) : api.get('/risk-predictions'),
      ]);

      if (mineId && !assignedMine) {
        api.get(`/mines/${mineId}`).then(r => setAssignedMine(r.data)).catch(() => {});
      }

      const active = alertsRes.data.filter(a => a.status === 'ACTIVE');
      const filteredAlerts = mineId ? active.filter(a => a.mine?.id === mineId) : active;

      setStats({
        activeAlerts: filteredAlerts.length,
        warningAlerts: filteredAlerts.filter(a => a.riskLevel === 'WARNING').length,
        criticalRisks: risksRes.data.filter(r => r.predictedRisk === 'CRITICAL').length,
        sensors: sensorsRes.data.length,
      });
      setRecentAlerts(filteredAlerts.slice(0, 5));
      setMineSensors(sensorsRes.data.slice(-20));
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); const t = setInterval(fetchData, 5000); return () => clearInterval(t); }, []);

  const chartData = {
    labels: mineSensors.map((_, i) => `T-${mineSensors.length - i}`),
    datasets: [{
      label: 'Risk Score',
      data: mineSensors.map(s => s.riskScore || 0),
      borderColor: '#f5a623',
      backgroundColor: 'rgba(245,166,35,0.1)',
      tension: 0.4,
      pointBackgroundColor: mineSensors.map(s =>
        s.riskLevel === 'CRITICAL' ? '#e05252' : s.riskLevel === 'WARNING' ? '#f5a623' : '#3ecf8e'
      ),
    }],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
      title: {
        display: true,
        text: assignedMine ? `${assignedMine.mineName} — Live Risk Trend` : 'Live Sensor Risk Trend',
      },
    },
    scales: { y: { beginAtZero: true } },
  };

  const riskColor = l => l === 'CRITICAL' ? '#e05252' : l === 'WARNING' ? '#f5a623' : '#3ecf8e';

  if (loading) return <div className="page" style={{ display: 'grid', placeItems: 'center', height: '60vh' }}><div className="loading" style={{ width: 50, height: 50 }} /></div>;

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="page-header">
        <h1>Supervisor Dashboard{assignedMine ? ` — ${assignedMine.mineName}` : ''}</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {simMsg && <span style={{ fontSize: '0.85rem', color: '#3ecf8e' }}>{simMsg}</span>}
          <button onClick={triggerSim} style={{ fontSize: '0.85rem', padding: '0.5rem 1rem' }}>⚡ Trigger Simulation</button>
          <motion.div animate={{ scale: [1, 1.05, 1] }} transition={{ duration: 2, repeat: Infinity }}>
            <Activity size={28} />
          </motion.div>
        </div>
      </div>

      {!user?.assignedMineId && (
        <div style={{ padding: '1rem', background: 'rgba(245,166,35,0.1)', border: '1px solid rgba(245,166,35,0.4)', borderRadius: 8, marginBottom: '1.5rem', fontSize: '0.9rem' }}>
          ⚠ You have not been assigned to a mine yet. Contact your admin.
        </div>
      )}

      <motion.div className="stats-grid" variants={{ visible: { transition: { staggerChildren: 0.15 } } }} initial="hidden" animate="visible">
        <StatCard icon={ShieldAlert} value={stats.activeAlerts} label="Active Alerts" color={stats.activeAlerts > 0 ? 'text-danger' : 'text-success'} pulse={stats.activeAlerts > 0} />
        <StatCard icon={AlertCircle} value={stats.warningAlerts} label="Warning Alerts" color="text-warning" />
        <StatCard icon={TrendingUp} value={stats.criticalRisks} label="Critical Risks" color={stats.criticalRisks > 0 ? 'text-danger' : 'text-success'} pulse={stats.criticalRisks > 0} />
        <StatCard icon={Radio} value={stats.sensors} label="Sensor Readings" color="text-primary" />
      </motion.div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem', marginTop: '2rem' }}>
        <motion.div className="stat-card" initial={{ opacity: 0, x: -40 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.4 }} style={{ height: 360, padding: '1.5rem' }}>
          {mineSensors.length === 0
            ? <div style={{ display: 'grid', placeItems: 'center', height: '100%', opacity: 0.4 }}>No sensor data available</div>
            : <Line data={chartData} options={chartOptions} />
          }
        </motion.div>

        <motion.div className="stat-card" initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.5 }} style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>Active Alerts</h3>
          {recentAlerts.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: '#3ecf8e' }}>
              <CheckCircle size={40} style={{ margin: '0 auto 0.5rem' }} />
              <p>All zones safe!</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
              {recentAlerts.map(a => (
                <div key={a.id} style={{ padding: '0.75rem', background: 'rgba(255,255,255,0.05)', borderRadius: 8, borderLeft: `3px solid ${riskColor(a.riskLevel)}` }}>
                  <div style={{ fontSize: '0.8rem', fontWeight: 600, color: riskColor(a.riskLevel) }}>{a.riskLevel} · {a.alertType}</div>
                  <div style={{ fontSize: '0.85rem', marginTop: 2 }}>{a.message}</div>
                  <div style={{ fontSize: '0.75rem', opacity: 0.5, marginTop: 2 }}>{a.mine?.mineName} → {a.zone?.zoneName}</div>
                </div>
              ))}
            </div>
          )}
        </motion.div>
      </div>
      <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.85rem', opacity: 0.5 }}>
        Auto-refreshes every 5s · Last update: {new Date().toLocaleTimeString()}
      </p>
    </motion.div>
  );
}

// ─── WORKER DASHBOARD ────────────────────────────────────────────────────────
function WorkerDashboard() {
  const [latestSensor, setLatestSensor] = useState(null);
  const [activeAlerts, setActiveAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [sensorsRes, alertsRes] = await Promise.all([
        api.get('/sensor-data'),
        api.get('/alerts'),
      ]);
      const sensors = sensorsRes.data;
      setLatestSensor(sensors.length > 0 ? sensors[sensors.length - 1] : null);
      setActiveAlerts(alertsRes.data.filter(a => a.status === 'ACTIVE').slice(0, 5));
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); const t = setInterval(fetchData, 5000); return () => clearInterval(t); }, []);

  const riskColor = l => l === 'CRITICAL' ? '#e05252' : l === 'WARNING' ? '#f5a623' : '#3ecf8e';
  const riskBg = l => l === 'CRITICAL' ? 'rgba(224,82,82,0.12)' : l === 'WARNING' ? 'rgba(245,166,35,0.12)' : 'rgba(62,207,142,0.12)';

  if (loading) return <div className="page" style={{ display: 'grid', placeItems: 'center', height: '60vh' }}><div className="loading" style={{ width: 50, height: 50 }} /></div>;

  const level = latestSensor?.riskLevel || 'SAFE';

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="page-header">
        <h1>Safety Status</h1>
        <motion.div animate={{ scale: [1, 1.05, 1] }} transition={{ duration: 2, repeat: Infinity }}>
          <Eye size={28} />
        </motion.div>
      </div>

      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5 }}
        style={{
          background: riskBg(level),
          border: `2px solid ${riskColor(level)}`,
          borderRadius: 16,
          padding: '2rem',
          textAlign: 'center',
          marginBottom: '2rem',
        }}
      >
        <motion.div
          animate={level === 'CRITICAL' ? { scale: [1, 1.1, 1] } : {}}
          transition={{ duration: 1, repeat: Infinity }}
        >
          {level === 'CRITICAL' ? <ShieldAlert size={64} style={{ color: riskColor(level), margin: '0 auto' }} /> :
           level === 'WARNING' ? <AlertCircle size={64} style={{ color: riskColor(level), margin: '0 auto' }} /> :
           <CheckCircle size={64} style={{ color: riskColor(level), margin: '0 auto' }} />}
        </motion.div>
        <h2 style={{ color: riskColor(level), fontSize: '2rem', margin: '0.75rem 0 0.25rem' }}>{level}</h2>
        <p style={{ opacity: 0.7 }}>
          {level === 'CRITICAL' ? '⚠ Danger! Follow evacuation procedures immediately.' :
           level === 'WARNING' ? 'Caution! Stay alert and follow supervisor instructions.' :
           'Environment is safe. Continue normal operations.'}
        </p>
      </motion.div>

      {latestSensor && (
        <motion.div className="stat-card" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} style={{ padding: '1.5rem', marginBottom: '2rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>Latest Sensor Reading</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '1rem' }}>
            {[
              { label: 'Methane (CH₄)', value: `${latestSensor.methaneLevel}%`, warn: latestSensor.methaneLevel > 1 },
              { label: 'Oxygen (O₂)', value: `${latestSensor.oxygenLevel}%`, warn: latestSensor.oxygenLevel < 19.5 },
              { label: 'Temperature', value: `${latestSensor.temperature}°C`, warn: latestSensor.temperature > 35 },
              { label: 'Humidity', value: `${latestSensor.humidity}%`, warn: false },
              { label: 'CO Level', value: latestSensor.carbonMonoxideLevel != null ? `${latestSensor.carbonMonoxideLevel} ppm` : 'N/A', warn: latestSensor.carbonMonoxideLevel > 25 },
              { label: 'Risk Score', value: latestSensor.riskScore ?? 'N/A', warn: latestSensor.riskScore > 60 },
            ].map(({ label, value, warn }) => (
              <div key={label} style={{ padding: '1rem', background: warn ? 'rgba(245,166,35,0.1)' : 'rgba(255,255,255,0.05)', borderRadius: 10, border: warn ? '1px solid rgba(245,166,35,0.4)' : '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
                <div style={{ fontSize: '0.75rem', opacity: 0.6, marginBottom: 4 }}>{label}</div>
                <div style={{ fontSize: '1.2rem', fontWeight: 700, color: warn ? '#f5a623' : 'inherit' }}>{value}</div>
              </div>
            ))}
          </div>
          <div style={{ marginTop: '0.75rem', fontSize: '0.8rem', opacity: 0.5 }}>
            Zone: {latestSensor.zone?.zoneName} · Mine: {latestSensor.mine?.mineName}
          </div>
        </motion.div>
      )}

      <motion.div className="stat-card" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }} style={{ padding: '1.5rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>
          <AlertCircle size={18} style={{ display: 'inline', marginRight: 6 }} />
          Active Alerts ({activeAlerts.length})
        </h3>
        {activeAlerts.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '1.5rem', color: '#3ecf8e' }}>
            <CheckCircle size={32} style={{ margin: '0 auto 0.5rem' }} />
            <p>No active alerts. Stay safe!</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
            {activeAlerts.map(a => (
              <div key={a.id} style={{ padding: '0.75rem', background: riskBg(a.riskLevel), borderRadius: 8, borderLeft: `3px solid ${riskColor(a.riskLevel)}` }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: riskColor(a.riskLevel) }}>{a.riskLevel} · {a.alertType}</div>
                <div style={{ fontSize: '0.85rem', marginTop: 2 }}>{a.message}</div>
                <div style={{ fontSize: '0.75rem', opacity: 0.5, marginTop: 2 }}>{a.mine?.mineName} → {a.zone?.zoneName}</div>
              </div>
            ))}
          </div>
        )}
      </motion.div>

      <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.85rem', opacity: 0.5 }}>
        Auto-refreshes every 5s · Last update: {new Date().toLocaleTimeString()}
      </p>
    </motion.div>
  );
}

// ─── MAIN EXPORT ─────────────────────────────────────────────────────────────
export default function Dashboard() {
  const { user } = useAuth();

  if (user?.role === 'ADMIN') return <AdminDashboard />;
  if (user?.role === 'SUPERVISOR') return <SupervisorDashboard />;
  return <WorkerDashboard />;
}
