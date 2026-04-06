import { useState, useEffect, useRef } from 'react';
import { Shield, Bell, Menu, User, ChevronRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import api from './api';

export default function TopNavbar({ sidebarOpen, setSidebarOpen }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [alerts, setAlerts] = useState([]);
  const [showNotifs, setShowNotifs] = useState(false);
  const notifRef = useRef(null);

  // Fetch active alerts for notification dropdown
  useEffect(() => {
    const load = () => {
      api.get('/alerts')
        .then(r => setAlerts(r.data.filter(a => a.status === 'ACTIVE').slice(0, 8)))
        .catch(() => {});
    };
    load();
    const interval = setInterval(load, 30000); // refresh every 30s
    return () => clearInterval(interval);
  }, []);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setShowNotifs(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const riskColor = (level) => ({
    CRITICAL: '#e05252',
    WARNING: '#f5a623',
    SAFE: '#3ecf8e',
  }[level] || '#8892a4');

  const unreadCount = alerts.length;

  return (
    <motion.header
      className="top-navbar"
      initial={{ y: -60, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.4 }}
    >
      {/* Left */}
      <div className="top-navbar-left">
        <button className="hamburger-btn" onClick={() => setSidebarOpen(!sidebarOpen)} aria-label="Toggle sidebar">
          <Menu size={22} />
        </button>
        <Shield className="brand-icon" size={22} />
        <span className="brand-name">Mine Safety Companion</span>
      </div>

      {/* Right */}
      <div className="top-navbar-right">

        {/* Bell with dropdown */}
        <div className="notif-wrapper" ref={notifRef}>
          <button className="notif-btn" onClick={() => setShowNotifs(v => !v)} aria-label="Notifications">
            <Bell size={18} />
            {unreadCount > 0 && (
              <span className="notif-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
            )}
          </button>

          <AnimatePresence>
            {showNotifs && (
              <motion.div
                className="notif-dropdown"
                initial={{ opacity: 0, y: -8, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -8, scale: 0.97 }}
                transition={{ duration: 0.15 }}
              >
                <div className="notif-header">
                  <span>Active Alerts</span>
                  {unreadCount > 0 && <span className="notif-count">{unreadCount}</span>}
                </div>

                {alerts.length === 0 ? (
                  <div className="notif-empty">No active alerts 🎉</div>
                ) : (
                  <div className="notif-list">
                    {alerts.map(alert => (
                      <div key={alert.id} className="notif-item">
                        <span
                          className="notif-dot"
                          style={{ background: riskColor(alert.riskLevel) }}
                        />
                        <div className="notif-body">
                          <span className="notif-msg">{alert.message}</span>
                          <span className="notif-meta">
                            {alert.mine?.mineName} · {alert.zone?.zoneName} · {alert.alertType}
                          </span>
                        </div>
                        <span
                          className="notif-level"
                          style={{ color: riskColor(alert.riskLevel) }}
                        >
                          {alert.riskLevel}
                        </span>
                      </div>
                    ))}
                  </div>
                )}

                <button
                  className="notif-footer"
                  onClick={() => { setShowNotifs(false); navigate('/alerts'); }}
                >
                  View all alerts <ChevronRight size={14} />
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* User pill → navigates to profile */}
        <button className="user-pill-btn" onClick={() => navigate('/profile')}>
          <div className="user-avatar">
            <User size={16} />
          </div>
          <div className="user-pill">
            <span className="user-name">{user?.fullName}</span>
            <span className="user-role">{user?.role}</span>
          </div>
        </button>

      </div>
    </motion.header>
  );
}
