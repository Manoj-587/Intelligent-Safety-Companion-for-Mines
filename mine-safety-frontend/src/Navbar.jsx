import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">⛏ Mine Safety</div>
      <div className="navbar-links">
        <Link to="/dashboard">Dashboard</Link>
        <Link to="/mines">Mines</Link>
        <Link to="/zones">Zones</Link>
        <Link to="/sensor-data">Sensor Data</Link>
        <Link to="/alerts">Alerts</Link>
        <Link to="/risk-predictions">Risk Predictions</Link>
        {user?.role === 'ADMIN' && <Link to="/users">Users</Link>}
      </div>
      <div className="navbar-user">
        <span>{user?.fullName} ({user?.role})</span>
        <button onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  );
}
