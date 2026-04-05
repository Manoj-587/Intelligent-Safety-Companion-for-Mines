import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './AuthContext';
import ProtectedRoute from './ProtectedRoute';
import Navbar from './Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Mines from './pages/Mines';
import Zones from './pages/Zones';
import SensorData from './pages/SensorData';
import Alerts from './pages/Alerts';
import RiskPredictions from './pages/RiskPredictions';
import Users from './pages/Users';

function Layout({ children }) {
  return (
    <>
      <Navbar />
      <main className="main-content">{children}</main>
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
          <Route path="/mines" element={<ProtectedRoute><Layout><Mines /></Layout></ProtectedRoute>} />
          <Route path="/zones" element={<ProtectedRoute><Layout><Zones /></Layout></ProtectedRoute>} />
          <Route path="/sensor-data" element={<ProtectedRoute><Layout><SensorData /></Layout></ProtectedRoute>} />
          <Route path="/alerts" element={<ProtectedRoute><Layout><Alerts /></Layout></ProtectedRoute>} />
          <Route path="/risk-predictions" element={<ProtectedRoute><Layout><RiskPredictions /></Layout></ProtectedRoute>} />
          <Route path="/users" element={<ProtectedRoute roles={['ADMIN']}><Layout><Users /></Layout></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
