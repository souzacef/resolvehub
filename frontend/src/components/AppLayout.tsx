import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';

export function AppLayout() {
  const { email, role, logout } = useAuth();
  const navigate = useNavigate();

  const userLabel = email && role ? `Logged in as ${email} · ${role}` : null;
  const canManageUsers = role === 'ADMIN' || role === 'MANAGER';

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">ResolveHub</div>
        {userLabel ? <p className="auth-user-label">{userLabel}</p> : null}
        <nav className="nav-links">
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/tickets">Tickets</NavLink>
          {canManageUsers ? (
            <NavLink to="/organization/users">Organization Users</NavLink>
          ) : null}
        </nav>
        <button className="ghost-button" type="button" onClick={handleLogout}>
          Logout
        </button>
      </aside>
      <main className="content-area">
        <Outlet />
      </main>
    </div>
  );
}
