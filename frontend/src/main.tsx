import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './features/auth/AuthContext';
import { BackendHealthProvider } from './features/health/BackendHealthContext';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <BrowserRouter>
      <BackendHealthProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BackendHealthProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
