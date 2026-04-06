import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { User, Mail, Phone, Shield, Edit3, Save, X, AlertCircle } from 'lucide-react';
import api from '../api';
import { useAuth } from '../AuthContext';

export default function Profile() {
  const { user } = useAuth();
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
    role: '',
    status: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (user) {
      setFormData({
        fullName: user.fullName || '',
        email: user.email || '',
        phoneNumber: user.phoneNumber || '',
        role: user.role || '',
        status: user.status || '',
      });
      setLoading(false);
    }
  }, [user]);

  const handleEdit = () => {
    setEditMode(true);
    setError('');
    setSuccess('');
  };

  const handleCancel = () => {
    setEditMode(false);
    setError('');
    setSuccess('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      // Note: Backend may require admin to update, or self-update endpoint
      await api.put(`/users/${user.id}`, formData);
      setSuccess('Profile updated successfully!');
      setTimeout(() => setSuccess(''), 3000);
      // Refresh auth user if needed
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="page">Loading...</div>;
  }

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="page-header">
        <h1>User Profile</h1>
        {!editMode && (
          <motion.button 
            onClick={handleEdit}
            className="flex items-center gap-2"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            <Edit3 className="w-5 h-5" />
            Edit Profile
          </motion.button>
        )}
      </div>

      <div className="max-w-2xl mx-auto">
        {error && (
          <motion.div 
            className="alert-error p-4 rounded-xl mb-6 flex items-center gap-3"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
          >
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            {error}
          </motion.div>
        )}
        {success && (
          <motion.div 
            className="alert-success p-4 rounded-xl mb-6 flex items-center gap-3"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
          >
            <Shield className="w-5 h-5 flex-shrink-0" />
            {success}
          </motion.div>
        )}

        <motion.div 
          className="stat-card p-8 text-center"
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.5 }}
        >
          <div className="w-32 h-32 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full mx-auto mb-6 flex items-center justify-center shadow-2xl">
            <User className="w-16 h-16 text-white" />
          </div>
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-semibold mb-2 text-gray-300">Full Name</label>
              <input
                type="text"
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                disabled={!editMode}
                className="w-full p-4 bg-white/5 border border-white/20 rounded-xl backdrop-blur-sm text-lg font-bold text-white text-center focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-semibold mb-2 text-gray-300">Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  disabled={!editMode}
                  className="w-full p-4 bg-white/5 border border-white/20 rounded-xl backdrop-blur-sm focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2 text-gray-300">Phone</label>
                <input
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  disabled={!editMode}
                  className="w-full p-4 bg-white/5 border border-white/20 rounded-xl backdrop-blur-sm focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-semibold mb-2 text-gray-300">Role</label>
                <input
                  type="text"
                  value={formData.role}
                  disabled
                  className="w-full p-4 bg-white/5 border border-white/20 rounded-xl backdrop-blur-sm text-lg font-semibold bg-gradient-to-r from-purple-500/20 to-blue-500/20 text-purple-300"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2 text-gray-300">Status</label>
                <span className={`badge px-4 py-2 font-bold ${formData.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}`}>
                  {formData.status}
                </span>
              </div>
            </div>

            {editMode && (
              <div className="flex gap-4 pt-6 border-t border-white/10">
                <motion.button
                  type="submit"
                  disabled={saving}
                  className="flex-1 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white font-bold py-4 px-8 rounded-xl shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all flex items-center justify-center gap-2"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  {saving ? (
                    <div className="loading w-6 h-6 border-white/30 border-t-white" />
                  ) : (
                    <>
                      <Save className="w-5 h-5" />
                      Save Changes
                    </>
                  )}
                </motion.button>
                <motion.button
                  type="button"
                  onClick={handleCancel}
                  className="flex-1 bg-white/10 hover:bg-white/20 border border-white/20 text-white font-bold py-4 px-8 rounded-xl backdrop-blur-sm hover:shadow-xl transition-all flex items-center justify-center gap-2"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  <X className="w-5 h-5" />
                  Cancel
                </motion.button>
              </div>
            )}
          </form>
        </motion.div>
      </div>
    </motion.div>
  );
}

