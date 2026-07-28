import { useEffect, useState } from 'react';
import api from '../api';
import { useAuth } from '../AuthContext';

const empty = { mine: '', zone: '', methaneLevel: '', oxygenLevel: '', temperature: '', humidity: '', carbonMonoxideLevel: '', airQualityIndex: '', riskScore: '', riskLevel: 'SAFE' };

export default function SensorData() {
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN' || user?.role === 'SUPERVISOR';
  const [data, setData] = useState([]);
  const [mines, setMines] = useState([]);
  const [zones, setZones] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const load = () => Promise.all([api.get('/sensor-data'), api.get('/mines'), api.get('/zones')]).then(([s, m, z]) => { setData(s.data); setMines(m.data); setZones(z.data); });
  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = { ...form, mine: { id: form.mine }, zone: { id: form.zone }, methaneLevel: +form.methaneLevel, oxygenLevel: +form.oxygenLevel, temperature: +form.temperature, humidity: +form.humidity, carbonMonoxideLevel: form.carbonMonoxideLevel || null, airQualityIndex: form.airQualityIndex || null, riskScore: form.riskScore || null };
    if (editId) await api.put(`/sensor-data/${editId}`, payload);
    else await api.post('/sensor-data', payload);
    setForm(empty); setEditId(null); setShowForm(false); load();
  };

  const handleEdit = (s) => {
    setForm({ mine: s.mine?.id, zone: s.zone?.id, methaneLevel: s.methaneLevel, oxygenLevel: s.oxygenLevel, temperature: s.temperature, humidity: s.humidity, carbonMonoxideLevel: s.carbonMonoxideLevel || '', airQualityIndex: s.airQualityIndex || '', riskScore: s.riskScore || '', riskLevel: s.riskLevel });
    setEditId(s.id); setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this record?')) { await api.delete(`/sensor-data/${id}`); load(); }
  };

  const riskClass = (level) => ({ SAFE: 'badge-safe', WARNING: 'badge-warning', CRITICAL: 'badge-critical' }[level] || '');

  return (
    <div className="page">
      <div className="page-header">
        <h1>Sensor Data</h1>
        {canEdit && <button onClick={() => { setForm(empty); setEditId(null); setShowForm(true); }}>+ Add Reading</button>}
      </div>

      {showForm && canEdit && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{editId ? 'Edit Reading' : 'Add Reading'}</h3>
            <form onSubmit={handleSubmit}>
              <select value={form.mine} onChange={e => setForm({ ...form, mine: e.target.value })} required>
                <option value="">Select Mine</option>
                {mines.map(m => <option key={m.id} value={m.id}>{m.mineName}</option>)}
              </select>
              <select value={form.zone} onChange={e => setForm({ ...form, zone: e.target.value })} required>
                <option value="">Select Zone</option>
                {zones.map(z => <option key={z.id} value={z.id}>{z.zoneName}</option>)}
              </select>
              <input placeholder="Methane Level (%)" type="number" step="0.01" value={form.methaneLevel} onChange={e => setForm({ ...form, methaneLevel: e.target.value })} required />
              <input placeholder="Oxygen Level (%)" type="number" step="0.01" value={form.oxygenLevel} onChange={e => setForm({ ...form, oxygenLevel: e.target.value })} required />
              <input placeholder="Temperature (°C)" type="number" step="0.01" value={form.temperature} onChange={e => setForm({ ...form, temperature: e.target.value })} required />
              <input placeholder="Humidity (%)" type="number" step="0.01" value={form.humidity} onChange={e => setForm({ ...form, humidity: e.target.value })} required />
              <input placeholder="CO Level (ppm)" type="number" step="0.01" value={form.carbonMonoxideLevel} onChange={e => setForm({ ...form, carbonMonoxideLevel: e.target.value })} />
              <input placeholder="Air Quality Index" type="number" step="0.01" value={form.airQualityIndex} onChange={e => setForm({ ...form, airQualityIndex: e.target.value })} />
              <input placeholder="Risk Score" type="number" step="0.01" value={form.riskScore} onChange={e => setForm({ ...form, riskScore: e.target.value })} />
              <select value={form.riskLevel} onChange={e => setForm({ ...form, riskLevel: e.target.value })}>
                <option value="SAFE">Safe</option>
                <option value="WARNING">Warning</option>
                <option value="CRITICAL">Critical</option>
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
        <thead><tr><th>Mine</th><th>Zone</th><th>CH₄%</th><th>O₂%</th><th>Temp</th><th>Humidity</th><th>CO</th><th>AQI</th><th>Risk Score</th><th>Risk Level</th><th>Recorded At</th>{canEdit && <th>Actions</th>}</tr></thead>
        <tbody>
          {data.map(s => (
            <tr key={s.id}>
              <td>{s.mine?.mineName}</td><td>{s.zone?.zoneName}</td>
              <td>{s.methaneLevel}</td><td>{s.oxygenLevel}</td><td>{s.temperature}</td><td>{s.humidity}</td>
              <td>{s.carbonMonoxideLevel}</td><td>{s.airQualityIndex}</td><td>{s.riskScore}</td>
              <td><span className={`badge ${riskClass(s.riskLevel)}`}>{s.riskLevel}</span></td>
              <td>{s.recordedAt ? new Date(s.recordedAt).toLocaleString() : ''}</td>
              {canEdit && <td><button className="btn-sm" onClick={() => handleEdit(s)}>Edit</button> <button className="btn-sm btn-danger" onClick={() => handleDelete(s.id)}>Delete</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
