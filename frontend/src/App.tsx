import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AppLayout from './components/layout/AppLayout';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import SetupAdmin from './pages/SetupAdmin';
import Dashboard from './pages/Dashboard';
import Employees from './pages/Employees';
import MealRecording from './pages/MealRecording';
import Reports from './pages/Reports';
import Expenses from './pages/Expenses';
import AuditLogs from './pages/AuditLogs';
import Users from './pages/Users';
import Settings from './pages/Settings';
import NotFound from './pages/NotFound';
import { Role } from './types';

const ProtectedRoute: React.FC<{ children: React.ReactNode; roles?: Role[] }> = ({ children, roles }) => {
  const { isAuthenticated, user, getDashboardPath } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to={getDashboardPath()} replace />;
  }

  return <>{children}</>;
};

export const AppRoutes: React.FC = () => {
  const { isAuthenticated, getDashboardPath } = useAuth();

  return (
    <Routes>
      {/* Public Authentication Routes */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to={getDashboardPath()} replace /> : <Login />}
      />
      <Route
        path="/register"
        element={isAuthenticated ? <Navigate to={getDashboardPath()} replace /> : <Register />}
      />
      <Route
        path="/forgot-password"
        element={isAuthenticated ? <Navigate to={getDashboardPath()} replace /> : <ForgotPassword />}
      />
      <Route
        path="/reset-password"
        element={isAuthenticated ? <Navigate to={getDashboardPath()} replace /> : <ResetPassword />}
      />
      <Route
        path="/setup-admin"
        element={<SetupAdmin />}
      />

      {/* Authenticated Protected Routes */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="admin/dashboard" element={<Dashboard />} />
        <Route path="director/dashboard" element={<Dashboard />} />
        <Route path="accountant/dashboard" element={<Dashboard />} />
        <Route path="hr/dashboard" element={<Dashboard />} />

        <Route path="employees" element={<Employees />} />
        
        <Route
          path="meals"
          element={
            <ProtectedRoute roles={['ADMIN', 'HR']}>
              <MealRecording />
            </ProtectedRoute>
          }
        />

        <Route path="reports" element={<Reports />} />
        
        <Route
          path="expenses"
          element={
            <ProtectedRoute roles={['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT']}>
              <Expenses />
            </ProtectedRoute>
          }
        />

        <Route
          path="audit-logs"
          element={
            <ProtectedRoute roles={['ADMIN', 'MANAGING_DIRECTOR']}>
              <AuditLogs />
            </ProtectedRoute>
          }
        />

        <Route
          path="users"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Users />
            </ProtectedRoute>
          }
        />

        <Route
          path="settings"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Settings />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
};

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
