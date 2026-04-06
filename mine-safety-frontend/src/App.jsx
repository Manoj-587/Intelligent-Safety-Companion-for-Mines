import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AuthProvider } from './AuthContext';
import ProtectedRoute from './ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Mines from './pages/Mines';
import Zones from './pages/Zones';
import SensorData from './pages/SensorData';
import Alerts from './pages/Alerts';
import RiskPredictions from './pages/RiskPredictions';
import Users from './pages/Users';
import Profile from './pages/Profile';
import Reports from './pages/Reports';
import TopNavbar from './TopNavbar';
import SidebarNavbar from './Navbar';

function Layout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(true);

  useEffect(() => {
    const handleResize = () => {
      setSidebarOpen(window.innerWidth > 1024);
    };
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <>
      <TopNavbar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
      <SidebarNavbar sidebarOpen={sidebarOpen} />
      <motion.main
        className={`main-content${sidebarOpen ? '' : ' sidebar-closed'}`}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
      >
        <div>{children}</div>
      </motion.main>
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/dashboard" element={<ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>} />
          <Route path="/mines" element={<ProtectedRoute roles={['ADMIN']}><Layout><Mines /></Layout></ProtectedRoute>} />
          <Route path="/zones" element={<ProtectedRoute roles={['ADMIN','SUPERVISOR']}><Layout><Zones /></Layout></ProtectedRoute>} />
          <Route path="/sensor-data" element={<ProtectedRoute roles={['ADMIN','SUPERVISOR']}><Layout><SensorData /></Layout></ProtectedRoute>} />
          <Route path="/alerts" element={<ProtectedRoute><Layout><Alerts /></Layout></ProtectedRoute>} />
          <Route path="/risk-predictions" element={<ProtectedRoute roles={['ADMIN','SUPERVISOR']}><Layout><RiskPredictions /></Layout></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><Layout><Profile /></Layout></ProtectedRoute>} />
          <Route path="/reports" element={<ProtectedRoute roles={['ADMIN','SUPERVISOR']}><Layout><Reports /></Layout></ProtectedRoute>} />
          <Route path="/users" element={<ProtectedRoute roles={['ADMIN']}><Layout><Users /></Layout></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
