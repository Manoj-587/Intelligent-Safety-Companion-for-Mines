import { useEffect, useState } from 'react';
import api from '../api';
import { useAuth } from '../AuthContext';

const empty = { sensorData: '', mine: '', zone: '', alertType: 'GAS', riskLevel: 'WARNING', message: '', status: 'ACTIVE' };

export default function Alerts() {
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN' || user?.role === 'SUPERVISOR';
  const [alerts, setAlerts] = useState([]);
  const [mines, setMines] = useState([]);
  const [zones, setZones] = useState([]);
  const [sensors, setSensors] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const load = () => Promise.all([api.get('/alerts'), api.get('/mines'), api.get('/zones'), api.get('/sensor-data')]).then(([a, m, z, s]) => { setAlerts(a.data); setMines(m.data); setZones(z.data); setSensors(s.data); });
  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = { ...form, sensorData: { id: form.sensorData }, mine: { id: form.mine }, zone: { id: form.zone } };
    if (editId) await api.put(`/alerts/${editId}`, payload);
    else await api.post('/alerts', payload);
    setForm(empty); setEditId(null); setShowForm(false); load();
  };

  const handleEdit = (a) => {
    setForm({ sensorData: a.sensorData?.id, mine: a.mine?.id, zone: a.zone?.id, alertType: a.alertType, riskLevel: a.riskLevel, message: a.message, status: a.status });
    setEditId(a.id); setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this alert?')) { await api.delete(`/alerts/${id}`); load(); }
  };

  const riskClass = (level) => ({ SAFE: 'badge-safe', WARNING: 'badge-warning', CRITICAL: 'badge-critical' }[level] || '');
  const statusClass = (s) => s === 'ACTIVE' ? 'badge-critical' : 'badge-safe';

  return (
    <div className="page">
      <div className="page-header">
        <h1>Alerts</h1>
        {canEdit && <button onClick={() => { setForm(empty); setEditId(null); setShowForm(true); }}>+ Add Alert</button>}
      </div>

      {showForm && canEdit && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{editId ? 'Edit Alert' : 'Add Alert'}</h3>
            <form onSubmit={handleSubmit}>
              <select value={form.sensorData} onChange={e => setForm({ ...form, sensorData: e.target.value })} required>
                <option value="">Select Sensor Reading</option>
                {sensors.map(s => <option key={s.id} value={s.id}>Reading #{s.id} - {s.mine?.mineName}</option>)}
              </select>
              <select value={form.mine} onChange={e => setForm({ ...form, mine: e.target.value })} required>
                <option value="">Select Mine</option>
                {mines.map(m => <option key={m.id} value={m.id}>{m.mineName}</option>)}
              </select>
              <select value={form.zone} onChange={e => setForm({ ...form, zone: e.target.value })} required>
                <option value="">Select Zone</option>
                {zones.map(z => <option key={z.id} value={z.id}>{z.zoneName}</option>)}
              </select>
              <select value={form.alertType} onChange={e => setForm({ ...form, alertType: e.target.value })}>
                {['GAS','OXYGEN','TEMPERATURE','MULTI_FACTOR'].map(t => <option key={t} value={t}>{t}</option>)}
              </select>
              <select value={form.riskLevel} onChange={e => setForm({ ...form, riskLevel: e.target.value })}>
                <option value="SAFE">Safe</option>
                <option value="WARNING">Warning</option>
                <option value="CRITICAL">Critical</option>
              </select>
              <textarea placeholder="Alert message" value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} required />
              <select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Active</option>
                <option value="RESOLVED">Resolved</option>
                <option value="ACKNOWLEDGED">Acknowledged</option>
              </select>
              <div className="form-actions">
                <button type="submit">{editId ? 'Update' : 'Create'}</button>
                <button type="button" className="btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <table className="data-table">
        <thead><tr><th>Mine</th><th>Zone</th><th>Type</th><th>Risk Level</th><th>Message</th><th>Status</th><th>Created At</th>{canEdit && <th>Actions</th>}</tr></thead>
        <tbody>
          {alerts.map(a => (
            <tr key={a.id}>
              <td>{a.mine?.mineName}</td><td>{a.zone?.zoneName}</td><td>{a.alertType}</td>
              <td><span className={`badge ${riskClass(a.riskLevel)}`}>{a.riskLevel}</span></td>
              <td>{a.message}</td>
              <td><span className={`badge ${statusClass(a.status)}`}>{a.status}</span></td>
              <td>{a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}</td>
              {canEdit && <td><button className="btn-sm" onClick={() => handleEdit(a)}>Edit</button> <button className="btn-sm btn-danger" onClick={() => handleDelete(a.id)}>Delete</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
