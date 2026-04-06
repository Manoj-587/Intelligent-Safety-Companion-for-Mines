import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Download, FileText, Filter, Calendar, AlertCircle, ShieldCheck } from 'lucide-react';
import api from '../api';

export default function Reports() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('all');
  const [dateRange, setDateRange] = useState('today');

  useEffect(() => {
    loadReports();
  }, [filterType, dateRange]);

  const loadReports = async () => {
    setLoading(true);
    try {
      const endpoint = filterType === 'alerts' ? '/alerts' : '/sensor-data';
      const res = await api.get(endpoint);
      setReports(res.data.slice(0, 100)); // Limit for demo
    } catch (error) {
      console.error('Load reports error:', error);
    } finally {
      setLoading(false);
    }
  };

  const exportCSV = () => {
    const headers = filterType === 'all' 
      ? ['Mine', 'Zone', 'CH4%', 'O2%', 'Temp', 'Risk', 'Time']
      : ['Mine', 'Zone', 'Type', 'Level', 'Status', 'Time'];
    
    const csvContent = [
      headers.join(','),
      ...reports.map(r => [
        r.mine?.mineName || '',
        r.zone?.zoneName || '',
        filterType === 'all' 
          ? [r.methaneLevel, r.oxygenLevel, r.temperature, r.riskLevel, r.recordedAt].join(',')
          : [r.alertType, r.riskLevel, r.status, r.createdAt].join(','),
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `mine-safety-${filterType}-${dateRange}-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="page-header">
        <h1>Generate Reports</h1>
        <motion.button 
          onClick={exportCSV}
          className="flex items-center gap-2 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 shadow-xl hover:shadow-2xl"
          whileHover={{ scale: 1.05, y: -2 }}
          whileTap={{ scale: 0.95 }}
        >
          <Download className="w-5 h-5" />
          Download CSV
        </motion.button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <motion.button 
          className={`p-6 rounded-2xl border-2 transition-all ${filterType === 'all' ? 'border-blue-500 bg-blue-500/10 shadow-2xl shadow-blue-500/25' : 'border-white/20 hover:border-white/40'}`}
          onClick={() => setFilterType('all')}
          whileHover={{ scale: 1.02 }}
        >
          <FileText className="w-12 h-12 mx-auto mb-3 text-blue-400" />
          <h3 className="font-bold text-xl mb-1">All Data</h3>
          <p className="text-gray-400">Sensor readings + alerts</p>
        </motion.button>

        <motion.button 
          className={`p-6 rounded-2xl border-2 transition-all ${filterType === 'sensors' ? 'border-emerald-500 bg-emerald-500/10 shadow-2xl shadow-emerald-500/25' : 'border-white/20 hover:border-white/40'}`}
          onClick={() => setFilterType('sensors')}
          whileHover={{ scale: 1.02 }}
        >
          <ShieldCheck className="w-12 h-12 mx-auto mb-3 text-emerald-400" />
          <h3 className="font-bold text-xl mb-1">Sensors</h3>
          <p className="text-gray-400">Environmental data</p>
        </motion.button>

        <motion.button 
          className={`p-6 rounded-2xl border-2 transition-all ${filterType === 'alerts' ? 'border-red-500 bg-red-500/10 shadow-2xl shadow-red-500/25' : 'border-white/20 hover:border-white/40'}`}
          onClick={() => setFilterType('alerts')}
          whileHover={{ scale: 1.02 }}
        >
          <AlertCircle className="w-12 h-12 mx-auto mb-3 text-red-400" />
          <h3 className="font-bold text-xl mb-1">Alerts</h3>
          <p className="text-gray-400">Safety incidents</p>
        </motion.button>
      </div>

      {loading ? (
        <div className="stat-card p-12 text-center">
          <div className="loading w-16 h-16 mx-auto mb-4" />
          <p>Loading report data...</p>
        </div>
      ) : (
        <motion.div 
          className="data-table-container"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>Mine</th>
                <th>Zone</th>
                <th>CH₄%</th>
                <th>O₂%</th>
                <th>Temperature</th>
                <th>Risk Level</th>
                <th>Recorded</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((report, index) => (
                <motion.tr 
                  key={report.id || index}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.02 }}
                  className="hover:bg-white/5"
                >
                  <td>{report.mine?.mineName || 'N/A'}</td>
                  <td>{report.zone?.zoneName || 'N/A'}</td>
                  <td>{report.methaneLevel?.toFixed(2) || 'N/A'}</td>
                  <td>{report.oxygenLevel?.toFixed(2) || 'N/A'}</td>
                  <td>{report.temperature || 'N/A'}°C</td>
                  <td>
                    <span className={`badge ${report.riskLevel === 'CRITICAL' ? 'badge-critical' : report.riskLevel === 'WARNING' ? 'badge-warning' : 'badge-safe'}`}>
                      {report.riskLevel}
                    </span>
                  </td>
                  <td>{new Date(report.recordedAt || report.createdAt).toLocaleString()}</td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </motion.div>
      )}
    </motion.div>
  );
}

