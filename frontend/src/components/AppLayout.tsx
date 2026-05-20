import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';

export function AppLayout() {
  const { email, role, logout } = useAuth();

  const userLabel = email && role ? `Logged in as ${email} · ${role}` : null;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">ResolveHub</div>
        {userLabel ? <p className="auth-user-label">{userLabel}</p> : null}
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
