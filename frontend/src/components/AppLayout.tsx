import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';

export function AppLayout() {
  const { logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">ResolveHub</div>
        <nav className="nav-links">
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/tickets">Tickets</NavLink>
        </nav>
        <button className="ghost-button" type="button" onClick={logout}>
          Logout
        </button>
      </aside>
      <main className="content-area">
        <Outlet />
      </main>
    </div>
  );
}
