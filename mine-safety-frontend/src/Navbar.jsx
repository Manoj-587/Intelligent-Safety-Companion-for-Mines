import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Home, Factory, MapPin, Radio, AlertCircle, TrendingUp, Users, LogOut, User, FileText } from 'lucide-react';
import { useAuth } from './AuthContext';

export default function Navbar({ sidebarOpen }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const navLinks = [
    { path: '/dashboard', icon: Home, label: 'Dashboard' },
    { path: '/mines', icon: Factory, label: 'Mines' },
    { path: '/zones', icon: MapPin, label: 'Zones' },
    { path: '/sensor-data', icon: Radio, label: 'Sensors' },
    { path: '/alerts', icon: AlertCircle, label: 'Alerts' },
    { path: '/risk-predictions', icon: TrendingUp, label: 'Predictions' },
    { path: '/reports', icon: FileText, label: 'Reports' },
    { path: '/profile', icon: User, label: 'Profile' },
  ];

  if (user?.role === 'ADMIN') {
    navLinks.splice(6, 0, { path: '/users', icon: Users, label: 'Users' });
  }

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <AnimatePresence>
      {sidebarOpen && (
        <motion.nav
          className="sidebar-navbar"
          initial={{ x: -260 }}
          animate={{ x: 0 }}
          exit={{ x: -260 }}
          transition={{ duration: 0.3, type: 'spring', stiffness: 300, damping: 30 }}
        >
          <div className="sidebar-header">
            <span>Navigation</span>
          </div>
          <div className="sidebar-links">
            {navLinks.map(({ path, icon: Icon, label }) => {
              const isActive = location.pathname === path;
              return (
                <Link key={path} to={path} className={`sidebar-link${isActive ? ' active' : ''}`}>
                  <Icon size={18} />
                  <span>{label}</span>
                </Link>
              );
            })}
          </div>
          <div className="sidebar-footer">
            <button className="logout-btn" onClick={handleLogout}>
              <LogOut size={18} />
              Logout
            </button>
          </div>
        </motion.nav>
      )}
    </AnimatePresence>
  );
}
