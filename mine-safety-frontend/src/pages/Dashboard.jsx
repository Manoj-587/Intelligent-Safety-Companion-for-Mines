import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Line } from 'react-chartjs-2';
import { 
  AlertCircle, 
  Activity, 
  MapPin, 
  Radio, 
  ShieldAlert, 
  TrendingUp,
  HardDrive 
} from 'lucide-react';
import api from '../api';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

export default function Dashboard() {
  const [stats, setStats] = useState({ mines: 0, zones: 0, alerts: 0, sensors: 0, risks: 0 });
  const [recentSensors, setRecentSensors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeRisks, setActiveRisks] = useState(0);

  const fetchData = async () => {
    try {
      const [minesRes, zonesRes, alertsRes, sensorsRes, risksRes] = await Promise.all([
        api.get('/mines'),
        api.get('/zones'),
        api.get('/alerts'),
        api.get('/sensor-data?_sort=recordedAt&_order=desc&_limit=50'),
        api.get('/risk-predictions'),
      ]);
      setStats({
        mines: minesRes.data.length,
        zones: zonesRes.data.length,
        alerts: alertsRes.data.filter(a => a.status === 'ACTIVE').length,
        sensors: sensorsRes.data.length,
        risks: risksRes.data.length,
      });
      setRecentSensors(sensorsRes.data.slice(0, 20));
      setActiveRisks(risksRes.data.filter(r => r.predictedRisk === 'CRITICAL').length);
    } catch (error) {
      console.error('Dashboard data fetch error:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000); // Realtime polling every 5s
    return () => clearInterval(interval);
  }, []);

  const chartData = {
    labels: recentSensors.map((_, i) => `T-${20-i}`),
    datasets: [
      {
        label: 'Risk Score',
        data: recentSensors.map(s => s.riskScore || 0),
        borderColor: 'rgb(79, 142, 247)',
        backgroundColor: 'rgba(79, 142, 247, 0.1)',
        tension: 0.4,
        pointBackgroundColor: recentSensors.map(s => {
          if (s.riskLevel === 'CRITICAL') return '#e05252';
          if (s.riskLevel === 'WARNING') return '#f5a623';
          return '#3ecf8e';
        }),
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    plugins: {
      legend: { position: 'top' },
      title: { display: true, text: 'Recent Sensor Risk Trends' },
    },
    scales: {
      y: { beginAtZero: true },
    },
    animation: { duration: 2000, easing: 'easeInOutQuart' },
  };

  const statVariants = {
    hidden: { opacity: 0, y: 50 },
    visible: { 
      opacity: 1, 
      y: 0,
      transition: { duration: 0.6, ease: 'easeOut' }
    }
  };

  if (loading) {
    return (
      <div className="page" style={{ display: 'grid', placeItems: 'center', height: '60vh' }}>
        <div className="loading" style={{ width: '50px', height: '50px' }} />
      </div>
    );
  }

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.5 }}>
      <div className="page-header">
        <motion.h1 
          variants={statVariants}
          initial="hidden"
          animate="visible"
        >
          Safety Dashboard
        </motion.h1>
        <motion.div 
          animate={{ scale: [1, 1.05, 1] }}
          transition={{ duration: 2, repeat: Infinity }}
        >
          <Activity className="w-8 h-8 text-primary" />
        </motion.div>
      </div>

      <AnimatePresence>
        <motion.div 
          className="stats-grid"
          variants={{ visible: { transition: { staggerChildren: 0.2 } } }}
          initial="hidden"
          animate="visible"
        >
          <motion.div className="stat-card" variants={statVariants}>
            <motion.div className="stat-icon" whileHover={{ scale: 1.1, rotate: 360 }} transition={{ type: "spring" }}>
              <HardDrive className="w-12 h-12 mx-auto text-primary" />
            </motion.div>
            <motion.div 
              className="stat-value"
              animate={{ scale: [1, 1.2, 1] }}
              transition={{ duration: 1.5, repeat: Infinity }}
            >
              {stats.mines.toLocaleString()}
            </motion.div>
            <div className="stat-label">Mines</div>
          </motion.div>

          <motion.div className="stat-card" variants={statVariants}>
            <motion.div className="stat-icon" whileHover={{ scale: 1.1 }}>
              <MapPin className="w-12 h-12 mx-auto text-success" />
            </motion.div>
            <div className="stat-value">{stats.zones.toLocaleString()}</div>
            <div className="stat-label">Zones</div>
          </motion.div>

          <motion.div className={`stat-card ${activeRisks > 0 ? '' : 'alert-card'}`} variants={statVariants}>
            <motion.div className="stat-icon" whileHover={{ scale: 1.1 }}>
              <ShieldAlert className={`w-12 h-12 mx-auto ${activeRisks > 0 ? 'text-danger animate-pulse' : 'text-warning'}`} />
            </motion.div>
            <div className="stat-value">{stats.alerts}</div>
            <div className="stat-label">Active Alerts</div>
          </motion.div>

          <motion.div className="stat-card" variants={statVariants}>
            <motion.div className="stat-icon" whileHover={{ scale: 1.1 }}>
              <Radio className="w-12 h-12 mx-auto text-warning" />
            </motion.div>
            <div className="stat-value">{stats.sensors}</div>
            <div className="stat-label">Sensor Readings</div>
          </motion.div>

          <motion.div className="stat-card alert-card" variants={statVariants}>
            <motion.div className="stat-icon" whileHover={{ scale: 1.1 }}>
              <TrendingUp className="w-12 h-12 mx-auto text-danger" />
            </motion.div>
            <div className="stat-value">{activeRisks}</div>
            <div className="stat-label">Critical Risks</div>
          </motion.div>
        </motion.div>
      </AnimatePresence>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginTop: '3rem' }}>
        <motion.div 
          className="stat-card" 
          initial={{ opacity: 0, x: -50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.5 }}
          style={{ height: '400px', padding: '2rem' }}
        >
          <Line data={chartData} options={chartOptions} />
        </motion.div>

        <motion.div 
          className="stat-card" 
          initial={{ opacity: 0, x: 50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.7 }}
          style={{ padding: '2rem' }}
        >
          <h3 style={{ marginBottom: '1rem', fontSize: '1.2rem' }}>Quick Actions</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <button className="w-full" onClick={() => window.location.href = '/sensor-data'}>
              <Radio className="inline w-5 h-5 mr-2" /> View Live Sensors
            </button>
            <button className="w-full" onClick={() => window.location.href = '/alerts'}>
              <AlertCircle className="inline w-5 h-5 mr-2 text-danger" /> Check Alerts
            </button>
            <button className="w-full" onClick={() => window.location.href = '/risk-predictions'}>
              <TrendingUp className="inline w-5 h-5 mr-2" /> Risk Predictions
            </button>
          </div>
          <div style={{ marginTop: '2rem', padding: '1rem', background: 'rgba(224,82,82,0.1)', borderRadius: '8px', border: '1px solid rgba(224,82,82,0.3)' }}>
            <AlertCircle className="inline w-5 h-5 mr-2 text-danger" />
            <span>{activeRisks > 0 ? `${activeRisks} critical risks detected!` : 'All zones safe 🚀'}</span>
          </div>
        </motion.div>
      </div>

      <motion.p 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 2 }}
        className="text-center mt-8 text-text-muted"
        style={{ fontSize: '0.9rem' }}
      >
        Data updates every 5 seconds • Last update: {new Date().toLocaleTimeString()}
      </motion.p>
    </motion.div>
  );
}
