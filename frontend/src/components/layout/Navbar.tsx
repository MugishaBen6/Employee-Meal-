import React from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Menu, CalendarDays } from 'lucide-react';
import { initialsOf, roleLabel } from './roleLabels';

interface NavbarProps {
  onToggleSidebar: () => void;
}

/** Page title shown in the top bar, by route. */
const PAGE_TITLES: { prefix: string; title: string; subtitle: string }[] = [
  { prefix: '/user/dashboard', title: 'My Dashboard', subtitle: 'Your meal records at a glance' },
  { prefix: '/admin/dashboard', title: 'Dashboard', subtitle: 'Organisation overview' },
  { prefix: '/director/dashboard', title: 'Dashboard', subtitle: 'Organisation overview' },
  { prefix: '/accountant/dashboard', title: 'Dashboard', subtitle: 'Organisation overview' },
  { prefix: '/hr/dashboard', title: 'Dashboard', subtitle: 'Organisation overview' },
  { prefix: '/dashboard', title: 'Dashboard', subtitle: 'Organisation overview' },
  { prefix: '/employees', title: 'Employees', subtitle: 'Staff and daily meal attendance' },
  { prefix: '/meals', title: 'Meal Recording', subtitle: 'Record ATE / DID NOT EAT' },
  { prefix: '/reports', title: 'Reports', subtitle: 'Date-range reports and exports' },
  { prefix: '/expenses', title: 'Expenses', subtitle: 'Meal spending analysis' },
  { prefix: '/audit-logs', title: 'Activity Logs', subtitle: 'Audit trail of all changes' },
  { prefix: '/users', title: 'Users', subtitle: 'Accounts, roles and approvals' },
  { prefix: '/settings', title: 'Settings', subtitle: 'System configuration' },
  { prefix: '/trash', title: 'Trash', subtitle: 'Deleted records recovery' },
];

const roleTone: Record<string, string> = {
  ADMIN: 'bg-indigo-50 text-indigo-700 ring-indigo-600/15',
  MANAGING_DIRECTOR: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  ACCOUNTANT: 'bg-sky-50 text-sky-700 ring-sky-600/15',
  HR: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
  USER: 'bg-slate-100 text-slate-700 ring-slate-500/15',
};

const kigaliDateLabel = (): string =>
  new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Africa/Kigali',
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date());

export const Navbar: React.FC<NavbarProps> = ({ onToggleSidebar }) => {
  const { user } = useAuth();
  const { pathname } = useLocation();
  const page = PAGE_TITLES.find((p) => pathname.startsWith(p.prefix));
  const displayName = user?.fullName || user?.username || '';

  return (
    <header className="h-16 sm:h-[68px] sticky top-0 z-30 px-3 sm:px-6 lg:px-8 flex items-center justify-between gap-3 bg-white/75 backdrop-blur-xl border-b border-slate-200/70 print:hidden">
      <div className="flex items-center gap-2.5 sm:gap-3 min-w-0">
        <button
          onClick={onToggleSidebar}
          aria-label="Open navigation menu"
          className="lg:hidden p-2 rounded-xl text-slate-700 hover:bg-slate-100 active:bg-slate-200 transition-colors shrink-0 flex items-center justify-center min-w-[42px] min-h-[42px] ring-1 ring-slate-200/80 bg-white"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="min-w-0">
          <h1 className="font-display text-[15px] sm:text-lg font-bold text-slate-900 leading-tight truncate">
            {page?.title ?? 'Employee Meal Management'}
          </h1>
          <p className="hidden sm:block text-xs text-slate-500 truncate">{page?.subtitle ?? 'RWANDA PLASTIC INDUSTRY'}</p>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        <div className="hidden lg:flex items-center gap-2 text-xs font-medium text-slate-500 bg-white/80 ring-1 ring-slate-200/80 rounded-full px-3 py-1.5">
          <CalendarDays className="w-3.5 h-3.5 text-indigo-500" />
          <span>{kigaliDateLabel()}</span>
        </div>

        {user && (
          <div className="flex items-center gap-2.5 rounded-full bg-white ring-1 ring-slate-200/80 p-1 sm:pr-3 shadow-xs">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-indigo-700 text-white flex items-center justify-center font-bold text-xs shadow-sm ring-2 ring-white shrink-0">
              {initialsOf(displayName)}
            </div>
            <div className="hidden md:block text-left leading-tight min-w-0">
              <p className="text-[13px] font-semibold text-slate-800 truncate max-w-[150px]">{displayName}</p>
              <p className="text-[11px] text-slate-500 truncate max-w-[150px]">{user.email}</p>
            </div>
            <span
              className={`hidden sm:inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold ring-1 ring-inset whitespace-nowrap ${
                roleTone[user.role] ?? roleTone.USER
              }`}
            >
              {roleLabel(user.role)}
            </span>
          </div>
        )}
      </div>
    </header>
  );
};
