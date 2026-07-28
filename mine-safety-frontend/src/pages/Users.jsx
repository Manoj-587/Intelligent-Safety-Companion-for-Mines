import { useEffect, useState } from 'react';
import api from '../api';

const empty = { fullName: '', email: '', password: '', role: 'WORKER', phoneNumber: '', status: 'ACTIVE' };

export default function Users() {
  const [users, setUsers] = useState([]);
  const [mines, setMines] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [assignModal, setAssignModal] = useState(null); // { userId, currentMineId }
  const [selectedMine, setSelectedMine] = useState('');

  const load = () => Promise.all([
    api.get('/users').then(r => setUsers(r.data)),
    api.get('/mines').then(r => setMines(r.data)),
  ]);

  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (editId) await api.put(`/users/${editId}`, form);
    else await api.post('/users', form);
    setForm(empty); setEditId(null); setShowForm(false); load();
  };

  const handleEdit = (u) => {
    setForm({ fullName: u.fullName, email: u.email, password: '', role: u.role, phoneNumber: u.phoneNumber || '', status: u.status });
    setEditId(u.id); setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this user?')) { await api.delete(`/users/${id}`); load(); }
  };

  const openAssign = (u) => {
    setAssignModal({ userId: u.id });
    setSelectedMine(u.assignedMine?.id || '');
  };

  const handleAssign = async () => {
    if (selectedMine) {
      await api.put(`/users/${assignModal.userId}/assign-mine/${selectedMine}`);
    } else {
      await api.put(`/users/${assignModal.userId}/unassign-mine`);
    }
    setAssignModal(null);
    load();
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Users</h1>
        <button onClick={() => { setForm(empty); setEditId(null); setShowForm(true); }}>+ Add User</button>
      </div>

      {showForm && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>{editId ? 'Edit User' : 'Add User'}</h3>
            <form onSubmit={handleSubmit}>
              <input placeholder="Full Name" value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} required />
              <input placeholder="Email" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} required />
              <input placeholder={editId ? 'New Password (leave blank to keep)' : 'Password'} type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required={!editId} />
              <input placeholder="Phone Number" value={form.phoneNumber} onChange={e => setForm({ ...form, phoneNumber: e.target.value })} />
              <select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>
                <option value="WORKER">Worker</option>
                <option value="SUPERVISOR">Supervisor</option>
                <option value="ADMIN">Admin</option>
              </select>
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

      {assignModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>Assign Mine</h3>
            <select value={selectedMine} onChange={e => setSelectedMine(e.target.value)}>
              <option value="">-- Unassign --</option>
              {mines.map(m => <option key={m.id} value={m.id}>{m.mineName}</option>)}
            </select>
            <div className="form-actions">
              <button onClick={handleAssign}>Save</button>
              <button className="btn-secondary" onClick={() => setAssignModal(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      <table className="data-table">
        <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Phone</th><th>Status</th><th>Assigned Mine</th><th>Actions</th></tr></thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.fullName}</td><td>{u.email}</td><td>{u.role}</td><td>{u.phoneNumber}</td>
              <td><span className={`badge badge-${u.status.toLowerCase()}`}>{u.status}</span></td>
              <td>{u.assignedMine?.mineName || <span style={{ opacity: 0.4 }}>—</span>}</td>
              <td>
                <button className="btn-sm" onClick={() => handleEdit(u)}>Edit</button>{' '}
                {(u.role === 'SUPERVISOR' || u.role === 'WORKER') && (
                  <button className="btn-sm" onClick={() => openAssign(u)}>Assign Mine</button>
                )}{' '}
                <button className="btn-sm btn-danger" onClick={() => handleDelete(u.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
