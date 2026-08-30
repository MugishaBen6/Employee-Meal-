import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Users,
  Utensils,
  FileSpreadsheet,
  DollarSign,
  Activity,
  UserCog,
  Settings as SettingsIcon,
  LogOut,
  X,
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { user, logout, hasRole } = useAuth();

  const navItems = [
    {
      label: 'Dashboard',
      path: '/dashboard',
      icon: LayoutDashboard,
      roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR'],
    },
    {
      label: 'Employees',
      path: '/employees',
      icon: Users,
      roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR'],
    },
    {
      label: 'Meal Recording',
      path: '/meals',
      icon: Utensils,
      roles: ['ADMIN', 'HR'],
    },
    {
      label: 'Reports',
      path: '/reports',
      icon: FileSpreadsheet,
      roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR'],
    },
    {
      label: 'Expenses',
      path: '/expenses',
      icon: DollarSign,
      roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT'],
    },
    {
      label: 'Activity Logs',
      path: '/audit-logs',
      icon: Activity,
      roles: ['ADMIN', 'MANAGING_DIRECTOR'],
    },
    {
      label: 'Users',
      path: '/users',
      icon: UserCog,
      roles: ['ADMIN'],
    },
    {
      label: 'Settings',
      path: '/settings',
      icon: SettingsIcon,
      roles: ['ADMIN'],
    },
  ];

  const filteredNavItems = navItems.filter((item) => hasRole(...(item.roles as any)));

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-xs lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed top-0 left-0 z-50 h-screen w-64 bg-slate-900 text-slate-300 flex flex-col transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Header */}
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-800 bg-slate-950/50">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-500 to-sky-400 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20">
              <Utensils className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-white tracking-wide text-base block leading-none">MealSys</span>
              <span className="text-[10px] text-indigo-400 font-medium tracking-wider uppercase">Enterprise</span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="lg:hidden text-slate-400 hover:text-white p-1 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 py-6 px-4 space-y-1.5 overflow-y-auto">
          {filteredNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                      : 'hover:bg-slate-800/80 text-slate-400 hover:text-slate-200'
                  }`
                }
              >
                <Icon className="w-5 h-5 shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* User Card & Logout */}
        <div className="p-4 border-t border-slate-800/80 bg-slate-950/30">
          <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/50 mb-2 border border-slate-700/50">
            <div className="truncate">
              <p className="text-sm font-semibold text-white truncate">{user?.fullName || user?.username}</p>
              <p className="text-xs text-indigo-400 font-mono tracking-wider">{user?.role}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-sm font-medium text-rose-400 hover:bg-rose-500/10 hover:text-rose-300 transition-colors border border-rose-500/20"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  );
};
