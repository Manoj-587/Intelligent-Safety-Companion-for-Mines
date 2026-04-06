import { useEffect, useState } from 'react';
import api from '../api';
import { useAuth } from '../AuthContext';

const empty = { mineCode: '', mineName: '', location: '', district: '', state: '', totalArea: '', depth: '', status: 'ACTIVE' };

export default function Mines() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [mines, setMines] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const load = () => api.get('/mines').then(r => setMines(r.data));
  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = { ...form, totalArea: form.totalArea || null, depth: form.depth || null };
    if (editId) await api.put(`/mines/${editId}`, payload);
    else await api.post('/mines', payload);
    setForm(empty); setEditId(null); setShowForm(false); load();
  };

  const handleEdit = (m) => {
    setForm({ mineCode: m.mineCode, mineName: m.mineName, location: m.location, district: m.district || '', state: m.state || '', totalArea: m.totalArea || '', depth: m.depth || '', status: m.status });
    setEditId(m.id); setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this mine?')) { await api.delete(`/mines/${id}`); load(); }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Mines</h1>
        {isAdmin && <button onClick={() => { setForm(empty); setEditId(null); setShowForm(true); }}>+ Add Mine</button>}
      </div>

      {showForm && isAdmin && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{editId ? 'Edit Mine' : 'Add Mine'}</h3>
            <form onSubmit={handleSubmit}>
              <input placeholder="Mine Code" value={form.mineCode} onChange={e => setForm({ ...form, mineCode: e.target.value })} required />
              <input placeholder="Mine Name" value={form.mineName} onChange={e => setForm({ ...form, mineName: e.target.value })} required />
              <input placeholder="Location" value={form.location} onChange={e => setForm({ ...form, location: e.target.value })} required />
              <input placeholder="District" value={form.district} onChange={e => setForm({ ...form, district: e.target.value })} />
              <input placeholder="State" value={form.state} onChange={e => setForm({ ...form, state: e.target.value })} />
              <input placeholder="Total Area (sq km)" type="number" value={form.totalArea} onChange={e => setForm({ ...form, totalArea: e.target.value })} />
              <input placeholder="Depth (m)" type="number" value={form.depth} onChange={e => setForm({ ...form, depth: e.target.value })} />
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
        <thead><tr><th>Code</th><th>Name</th><th>Location</th><th>District</th><th>State</th><th>Area</th><th>Depth</th><th>Status</th>{isAdmin && <th>Actions</th>}</tr></thead>
        <tbody>
          {mines.map(m => (
            <tr key={m.id}>
              <td>{m.mineCode}</td><td>{m.mineName}</td><td>{m.location}</td>
              <td>{m.district}</td><td>{m.state}</td><td>{m.totalArea}</td><td>{m.depth}</td>
              <td><span className={`badge badge-${m.status.toLowerCase()}`}>{m.status}</span></td>
              {isAdmin && <td><button className="btn-sm" onClick={() => handleEdit(m)}>Edit</button> <button className="btn-sm btn-danger" onClick={() => handleDelete(m.id)}>Delete</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
