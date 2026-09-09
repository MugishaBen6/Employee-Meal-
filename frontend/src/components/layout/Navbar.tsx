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
    <header className="h-16 bg-white/90 backdrop-blur-md border-b border-slate-200/80 sticky top-0 z-30 px-3 sm:px-6 flex items-center justify-between shadow-xs">
      <div className="flex items-center gap-2.5 sm:gap-3 min-w-0">
        <button
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
          className="lg:hidden p-2 rounded-xl text-slate-700 hover:bg-slate-100 active:bg-slate-200 transition-colors shrink-0 flex items-center justify-center min-w-[42px] min-h-[42px]"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Mobile App Title */}
        <div className="sm:hidden flex items-center gap-1.5 truncate">
          <span className="font-bold text-slate-900 tracking-tight text-sm">MealSys</span>
          <span className="text-[10px] px-1.5 py-0.5 bg-indigo-50 text-indigo-700 font-semibold rounded border border-indigo-200">
            Enterprise
          </span>
        </div>

        {/* Desktop / Tablet App Title */}
        <div className="hidden sm:block truncate">
          <h1 className="text-base font-semibold text-slate-800 truncate">
            Employee Meal Management System
          </h1>
          <p className="text-xs text-slate-500 truncate">Rwanda Factory & Office Logistics</p>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        {user && (
          <div className="flex items-center gap-2 sm:gap-3 bg-slate-50 p-1 sm:p-1.5 sm:pr-3 rounded-full border border-slate-200">
            <div className="w-8 h-8 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold text-xs sm:text-sm shadow-xs shrink-0">
              {user.firstName ? user.firstName.charAt(0) : user.username.charAt(0)}
            </div>
            <div className="hidden md:block text-left">
              <p className="text-xs font-semibold text-slate-800 leading-none truncate max-w-[140px]">{user.fullName || user.username}</p>
              <p className="text-[10px] text-slate-500 font-mono mt-0.5 truncate max-w-[140px]">{user.email}</p>
            </div>
            <Badge variant={roleColors[user.role] || 'neutral'} size="sm">
              <ShieldCheck className="w-3 h-3 mr-0.5 sm:mr-1 inline shrink-0" />
              <span className="text-[10px] sm:text-xs font-semibold">{user.role}</span>
            </Badge>
          </div>
        )}
      </div>
    </header>
  );
};
