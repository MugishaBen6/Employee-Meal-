import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AppLayout from './components/layout/AppLayout';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import SetupAdmin from './pages/SetupAdmin';

// Route-level code splitting for high performance
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Employees = lazy(() => import('./pages/Employees'));
const MealRecording = lazy(() => import('./pages/MealRecording'));
const Reports = lazy(() => import('./pages/Reports'));
const Expenses = lazy(() => import('./pages/Expenses'));
const AuditLogs = lazy(() => import('./pages/AuditLogs'));
const Users = lazy(() => import('./pages/Users'));
const Settings = lazy(() => import('./pages/Settings'));
const Trash = lazy(() => import('./pages/Trash'));
const NotFound = lazy(() => import('./pages/NotFound'));
const UserDashboard = lazy(() => import('./pages/UserDashboard'));

import { Role } from './types';

const PageFallback = () => (
  <div className="flex items-center justify-center min-h-[400px] w-full">
    <div className="flex flex-col items-center gap-3">
      <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
      <p className="text-xs text-slate-400 font-medium tracking-wide uppercase">Loading...</p>
    </div>
  </div>
);

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
        <Route index element={<Navigate to={getDashboardPath()} replace />} />
        <Route
          path="dashboard"
          element={
            <ProtectedRoute roles={['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR']}>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/dashboard"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="director/dashboard"
          element={
            <ProtectedRoute roles={['MANAGING_DIRECTOR']}>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="accountant/dashboard"
          element={
            <ProtectedRoute roles={['ACCOUNTANT']}>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="hr/dashboard"
          element={
            <ProtectedRoute roles={['HR']}>
              <Dashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="employees"
          element={
            <ProtectedRoute roles={['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR']}>
              <Employees />
            </ProtectedRoute>
          }
        />
        
        <Route
          path="meals"
          element={
            <ProtectedRoute roles={['ADMIN', 'HR', 'USER']}>
              <MealRecording />
            </ProtectedRoute>
          }
        />

        <Route
          path="reports"
          element={
            <ProtectedRoute roles={['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR']}>
              <Reports />
            </ProtectedRoute>
          }
        />
        
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
          path="trash"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Trash />
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

        <Route
          path="user/dashboard"
          element={
            <ProtectedRoute roles={['USER']}>
              <UserDashboard />
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
