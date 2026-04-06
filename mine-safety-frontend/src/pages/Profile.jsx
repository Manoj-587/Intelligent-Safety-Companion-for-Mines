import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { User, Shield, Edit3, Save, X, AlertCircle } from 'lucide-react';
import api from '../api';
import { useAuth } from '../AuthContext';

export default function Profile() {
  const { user, updateUser } = useAuth();
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({ fullName: '', email: '', phoneNumber: '' });
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!user?.email) { setLoading(false); return; }
    api.get(`/users/by-email?email=${encodeURIComponent(user.email)}`)
      .then(({ data }) => {
        setProfile(data);
        setFormData({ fullName: data.fullName || '', email: data.email || '', phoneNumber: data.phoneNumber || '' });
      })
      .catch(() => {
        // fallback to cached auth data
        setProfile(user);
        setFormData({ fullName: user.fullName || '', email: user.email || '', phoneNumber: user.phoneNumber || '' });
      })
      .finally(() => setLoading(false));
  }, [user?.email]);

  const handleCancel = () => { setEditMode(false); setError(''); setSuccess(''); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const userId = profile?.id ?? user?.id;
      const { data } = await api.put(`/users/${userId}`, formData);
      setProfile(data);
      updateUser({ fullName: data.fullName, email: data.email, phoneNumber: data.phoneNumber });
      setSuccess('Profile updated successfully!');
      setEditMode(false);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="page">Loading...</div>;
  if (!profile && !user) return <div className="page">No profile data found.</div>;

  const displayRole = profile?.role ?? user?.role ?? '';
  const displayStatus = profile?.status ?? user?.status ?? '';

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="page-header">
        <h1>User Profile</h1>
        {!editMode && (
          <motion.button onClick={() => { setEditMode(true); setError(''); setSuccess(''); }}
            className="flex items-center gap-2" whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Edit3 className="w-5 h-5" /> Edit Profile
          </motion.button>
        )}
      </div>

      <div className="max-w-2xl mx-auto">
        {error && (
          <motion.div className="alert alert-error mb-4 flex items-center gap-2"
            initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}>
            <AlertCircle className="w-4 h-4 flex-shrink-0" />{error}
          </motion.div>
        )}
        {success && (
          <motion.div className="alert alert-success mb-4 flex items-center gap-2"
            initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}>
            <Shield className="w-4 h-4 flex-shrink-0" />{success}
          </motion.div>
        )}

        <motion.div className="stat-card p-8"
          initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ duration: 0.4 }}>
          <div className="flex justify-center mb-6">
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-xl">
              <User className="w-12 h-12 text-white" />
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-semibold mb-1" style={{ color: 'var(--text-muted)' }}>Full Name</label>
              <input type="text" value={formData.fullName} disabled={!editMode} required
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })} />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold mb-1" style={{ color: 'var(--text-muted)' }}>Email</label>
                <input type="email" value={formData.email} disabled={!editMode} required
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1" style={{ color: 'var(--text-muted)' }}>Phone</label>
                <input type="tel" value={formData.phoneNumber} disabled={!editMode}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })} />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold mb-1" style={{ color: 'var(--text-muted)' }}>Role</label>
                <input type="text" value={displayRole} disabled />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1" style={{ color: 'var(--text-muted)' }}>Status</label>
                <div className="pt-1">
                  <span className={`badge ${displayStatus === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}`}>
                    {displayStatus || '—'}
                  </span>
                </div>
              </div>
            </div>

            {editMode && (
              <div className="flex gap-3 pt-4" style={{ borderTop: '1px solid var(--border)' }}>
                <motion.button type="submit" disabled={saving} className="flex-1 flex items-center justify-center gap-2"
                  whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                  {saving ? <div className="loading w-5 h-5" /> : <><Save className="w-4 h-4" />Save Changes</>}
                </motion.button>
                <motion.button type="button" onClick={handleCancel}
                  className="flex-1 btn-secondary flex items-center justify-center gap-2"
                  whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                  <X className="w-4 h-4" />Cancel
                </motion.button>
              </div>
            )}
          </form>
        </motion.div>
      </div>
    </motion.div>
  );
}
