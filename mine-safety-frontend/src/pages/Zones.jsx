import { useEffect, useState } from 'react';
import api from '../api';
import { useAuth } from '../AuthContext';

const empty = { zoneCode: '', zoneName: '', mine: null, zoneType: 'GENERAL', maxSafeTemperature: '', minSafeOxygen: '', maxSafeMethane: '', status: 'ACTIVE' };

export default function Zones() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [zones, setZones] = useState([]);
  const [mines, setMines] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const load = () => Promise.all([api.get('/zones'), api.get('/mines')]).then(([z, m]) => { setZones(z.data); setMines(m.data); });
  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = { ...form, mine: { id: form.mine }, maxSafeTemperature: form.maxSafeTemperature || null, minSafeOxygen: form.minSafeOxygen || null, maxSafeMethane: form.maxSafeMethane || null };
    if (editId) await api.put(`/zones/${editId}`, payload);
    else await api.post('/zones', payload);
    setForm(empty); setEditId(null); setShowForm(false); load();
  };

  const handleEdit = (z) => {
    setForm({ zoneCode: z.zoneCode, zoneName: z.zoneName, mine: z.mine?.id, zoneType: z.zoneType, maxSafeTemperature: z.maxSafeTemperature || '', minSafeOxygen: z.minSafeOxygen || '', maxSafeMethane: z.maxSafeMethane || '', status: z.status });
    setEditId(z.id); setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this zone?')) { await api.delete(`/zones/${id}`); load(); }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Zones</h1>
        {isAdmin && <button onClick={() => { setForm(empty); setEditId(null); setShowForm(true); }}>+ Add Zone</button>}
      </div>

      {showForm && isAdmin && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{editId ? 'Edit Zone' : 'Add Zone'}</h3>
            <form onSubmit={handleSubmit}>
              <input placeholder="Zone Code" value={form.zoneCode} onChange={e => setForm({ ...form, zoneCode: e.target.value })} required />
              <input placeholder="Zone Name" value={form.zoneName} onChange={e => setForm({ ...form, zoneName: e.target.value })} required />
              <select value={form.mine || ''} onChange={e => setForm({ ...form, mine: e.target.value })} required>
                <option value="">Select Mine</option>
                {mines.map(m => <option key={m.id} value={m.id}>{m.mineName}</option>)}
              </select>
              <select value={form.zoneType} onChange={e => setForm({ ...form, zoneType: e.target.value })}>
                {['EXTRACTION','VENTILATION','STORAGE','BLASTING','EQUIPMENT','GENERAL'].map(t => <option key={t} value={t}>{t}</option>)}
              </select>
              <input placeholder="Max Safe Temperature (°C)" type="number" value={form.maxSafeTemperature} onChange={e => setForm({ ...form, maxSafeTemperature: e.target.value })} />
              <input placeholder="Min Safe Oxygen (%)" type="number" value={form.minSafeOxygen} onChange={e => setForm({ ...form, minSafeOxygen: e.target.value })} />
              <input placeholder="Max Safe Methane (%)" type="number" value={form.maxSafeMethane} onChange={e => setForm({ ...form, maxSafeMethane: e.target.value })} />
              <select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
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
        <thead><tr><th>Code</th><th>Name</th><th>Mine</th><th>Type</th><th>Max Temp</th><th>Min O₂</th><th>Max CH₄</th><th>Status</th>{isAdmin && <th>Actions</th>}</tr></thead>
        <tbody>
          {zones.map(z => (
            <tr key={z.id}>
              <td>{z.zoneCode}</td><td>{z.zoneName}</td><td>{z.mine?.mineName}</td><td>{z.zoneType}</td>
              <td>{z.maxSafeTemperature}</td><td>{z.minSafeOxygen}</td><td>{z.maxSafeMethane}</td>
              <td><span className={`badge badge-${z.status.toLowerCase()}`}>{z.status}</span></td>
              {isAdmin && <td><button className="btn-sm" onClick={() => handleEdit(z)}>Edit</button> <button className="btn-sm btn-danger" onClick={() => handleDelete(z.id)}>Delete</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
