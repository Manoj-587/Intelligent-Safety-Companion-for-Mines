import { useEffect, useState } from 'react';
import api from '../api';
import { useAuth } from '../AuthContext';

const empty = { sensorData: '', mine: '', zone: '', predictedRisk: 'SAFE', probabilitySafe: '', probabilityWarning: '', probabilityCritical: '', overallRiskScore: '', modelVersion: '' };

export default function RiskPredictions() {
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN' || user?.role === 'SUPERVISOR';
  const [predictions, setPredictions] = useState([]);
  const [mines, setMines] = useState([]);
  const [zones, setZones] = useState([]);
  const [sensors, setSensors] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const load = () => Promise.all([api.get('/risk-predictions'), api.get('/mines'), api.get('/zones'), api.get('/sensor-data')]).then(([p, m, z, s]) => { setPredictions(p.data); setMines(m.data); setZones(z.data); setSensors(s.data); });
  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = { ...form, sensorData: { id: form.sensorData }, mine: { id: form.mine }, zone: { id: form.zone }, probabilitySafe: form.probabilitySafe || null, probabilityWarning: form.probabilityWarning || null, probabilityCritical: form.probabilityCritical || null, overallRiskScore: +form.overallRiskScore };
    if (editId) await api.put(`/risk-predictions/${editId}`, payload);
    else await api.post('/risk-predictions', payload);
    setForm(empty); setEditId(null); setShowForm(false); load();
  };

  const handleEdit = (p) => {
    setForm({ sensorData: p.sensorData?.id, mine: p.mine?.id, zone: p.zone?.id, predictedRisk: p.predictedRisk, probabilitySafe: p.probabilitySafe || '', probabilityWarning: p.probabilityWarning || '', probabilityCritical: p.probabilityCritical || '', overallRiskScore: p.overallRiskScore, modelVersion: p.modelVersion || '' });
    setEditId(p.id); setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this prediction?')) { await api.delete(`/risk-predictions/${id}`); load(); }
  };

  const riskClass = (level) => ({ SAFE: 'badge-safe', WARNING: 'badge-warning', CRITICAL: 'badge-critical' }[level] || '');

  return (
    <div className="page">
      <div className="page-header">
        <h1>Risk Predictions</h1>
        {canEdit && <button onClick={() => { setForm(empty); setEditId(null); setShowForm(true); }}>+ Add Prediction</button>}
      </div>

      {showForm && canEdit && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{editId ? 'Edit Prediction' : 'Add Prediction'}</h3>
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
              <select value={form.predictedRisk} onChange={e => setForm({ ...form, predictedRisk: e.target.value })}>
                <option value="SAFE">Safe</option>
                <option value="WARNING">Warning</option>
                <option value="CRITICAL">Critical</option>
              </select>
              <input placeholder="Probability Safe (0-1)" type="number" step="0.01" min="0" max="1" value={form.probabilitySafe} onChange={e => setForm({ ...form, probabilitySafe: e.target.value })} />
              <input placeholder="Probability Warning (0-1)" type="number" step="0.01" min="0" max="1" value={form.probabilityWarning} onChange={e => setForm({ ...form, probabilityWarning: e.target.value })} />
              <input placeholder="Probability Critical (0-1)" type="number" step="0.01" min="0" max="1" value={form.probabilityCritical} onChange={e => setForm({ ...form, probabilityCritical: e.target.value })} />
              <input placeholder="Overall Risk Score" type="number" step="0.01" value={form.overallRiskScore} onChange={e => setForm({ ...form, overallRiskScore: e.target.value })} required />
              <input placeholder="Model Version" value={form.modelVersion} onChange={e => setForm({ ...form, modelVersion: e.target.value })} />
              <div className="form-actions">
                <button type="submit">{editId ? 'Update' : 'Create'}</button>
                <button type="button" className="btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <table className="data-table">
        <thead><tr><th>Mine</th><th>Zone</th><th>Predicted Risk</th><th>P(Safe)</th><th>P(Warning)</th><th>P(Critical)</th><th>Risk Score</th><th>Model</th><th>Time</th>{canEdit && <th>Actions</th>}</tr></thead>
        <tbody>
          {predictions.map(p => (
            <tr key={p.id}>
              <td>{p.mine?.mineName}</td><td>{p.zone?.zoneName}</td>
              <td><span className={`badge ${riskClass(p.predictedRisk)}`}>{p.predictedRisk}</span></td>
              <td>{p.probabilitySafe}</td><td>{p.probabilityWarning}</td><td>{p.probabilityCritical}</td>
              <td>{p.overallRiskScore}</td><td>{p.modelVersion}</td>
              <td>{p.predictionTime ? new Date(p.predictionTime).toLocaleString() : ''}</td>
              {canEdit && <td><button className="btn-sm" onClick={() => handleEdit(p)}>Edit</button> <button className="btn-sm btn-danger" onClick={() => handleDelete(p.id)}>Delete</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
