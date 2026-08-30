import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Menu, ShieldCheck, User } from 'lucide-react';
import { Badge } from '../common/Badge';

interface NavbarProps {
  onToggleSidebar: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onToggleSidebar }) => {
  const { user } = useAuth();

  const roleColors: Record<string, 'info' | 'success' | 'warning' | 'danger'> = {
    ADMIN: 'danger',
    MANAGING_DIRECTOR: 'warning',
    ACCOUNTANT: 'info',
    HR: 'success',
  };

  return (
    <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200/80 sticky top-0 z-30 px-4 sm:px-6 flex items-center justify-between shadow-xs">
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="lg:hidden p-2 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
        >
          <Menu className="w-5 h-5" />
        </button>
        <div className="hidden sm:block">
          <h1 className="text-base font-semibold text-slate-800">
            Employee Meal Management System
          </h1>
          <p className="text-xs text-slate-500">Rwanda Factory & Office Logistics</p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-3 bg-slate-50 p-1.5 pr-3 rounded-full border border-slate-200">
            <div className="w-8 h-8 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold text-sm shadow-xs">
              {user.firstName ? user.firstName.charAt(0) : user.username.charAt(0)}
            </div>
            <div className="hidden md:block text-left">
              <p className="text-xs font-semibold text-slate-800 leading-none">{user.fullName || user.username}</p>
              <p className="text-[10px] text-slate-500 font-mono mt-0.5">{user.email}</p>
            </div>
            <Badge variant={roleColors[user.role] || 'neutral'}>
              <ShieldCheck className="w-3 h-3 mr-1 inline" />
              {user.role}
            </Badge>
          </div>
        )}
      </div>
    </header>
  );
};
