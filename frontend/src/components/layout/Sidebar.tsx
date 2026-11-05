import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Users,
  Utensils,
  UtensilsCrossed,
  FileSpreadsheet,
  DollarSign,
  Activity,
  UserCog,
  Settings as SettingsIcon,
  LogOut,
  X,
} from 'lucide-react';
import { initialsOf, roleLabel } from './roleLabels';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

type NavItem = {
  label: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: string[];
  section: 'overview' | 'operations' | 'administration';
};

// Same destinations and role rules as before; only grouped for readability.
const NAV_ITEMS: NavItem[] = [
  { label: 'My Dashboard', path: '/user/dashboard', icon: LayoutDashboard, roles: ['USER'], section: 'overview' },
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR'], section: 'overview' },
  { label: 'Employees', path: '/employees', icon: Users, roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR'], section: 'operations' },
  { label: 'Meal Recording', path: '/meals', icon: Utensils, roles: ['ADMIN', 'HR', 'USER'], section: 'operations' },
  { label: 'Reports', path: '/reports', icon: FileSpreadsheet, roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT', 'HR'], section: 'operations' },
  { label: 'Expenses', path: '/expenses', icon: DollarSign, roles: ['ADMIN', 'MANAGING_DIRECTOR', 'ACCOUNTANT'], section: 'operations' },
  { label: 'Activity Logs', path: '/audit-logs', icon: Activity, roles: ['ADMIN', 'MANAGING_DIRECTOR'], section: 'administration' },
  { label: 'Users', path: '/users', icon: UserCog, roles: ['ADMIN'], section: 'administration' },
  { label: 'Settings', path: '/settings', icon: SettingsIcon, roles: ['ADMIN'], section: 'administration' },
];

const SECTION_LABELS: Record<NavItem['section'], string> = {
  overview: 'Overview',
  operations: 'Operations',
  administration: 'Administration',
};

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { user, logout, hasRole } = useAuth();

  const visible = NAV_ITEMS.filter((item) => hasRole(...(item.roles as any)));
  const sections = (['overview', 'operations', 'administration'] as const)
    .map((s) => ({ key: s, items: visible.filter((i) => i.section === s) }))
    .filter((s) => s.items.length > 0);

  const displayName = user?.fullName || user?.username || '';

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/60 backdrop-blur-sm lg:hidden animate-overlayIn"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed top-0 left-0 z-50 h-[100dvh] w-[280px] max-w-[86vw] lg:w-64 flex flex-col text-slate-300 transition-transform duration-300 ease-[cubic-bezier(0.22,1,0.36,1)] shadow-2xl lg:shadow-none lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{
          background:
            'radial-gradient(420px 260px at 0% 0%, rgba(85,104,238,0.22), transparent 70%), linear-gradient(180deg, #0F1729 0%, #0B1220 55%, #090F1B 100%)',
        }}
        aria-label="Main navigation"
      >
        {/* Brand */}
        <div className="h-[68px] flex items-center justify-between px-5 shrink-0">
          <div className="flex items-center gap-3 min-w-0">
            <div className="relative w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-400 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-indigo-600/40 ring-1 ring-white/20 shrink-0">
              <UtensilsCrossed className="w-5 h-5" />
              <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full bg-amber-400 ring-2 ring-[#0F1729]" aria-hidden="true" />
            </div>
            <div className="min-w-0 leading-tight">
              <span className="font-display font-extrabold text-white text-[15px] block truncate">Meal Management</span>
              <span className="text-[10.5px] text-slate-400 font-medium tracking-wide block truncate">Rwanda Plastic Industry</span>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Close navigation"
            className="lg:hidden text-slate-400 hover:text-white hover:bg-white/5 active:bg-white/10 p-2 rounded-xl transition-colors min-w-[40px] min-h-[40px] flex items-center justify-center"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mx-5 h-px bg-gradient-to-r from-white/10 via-white/5 to-transparent shrink-0" />

        {/* Navigation */}
        <nav className="flex-1 py-5 px-3 overflow-y-auto touch-scroll sidebar-scroll space-y-6">
          {sections.map((section) => (
            <div key={section.key}>
              <p className="px-3 mb-2 text-[10.5px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                {SECTION_LABELS[section.key]}
              </p>
              <div className="space-y-1">
                {section.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      onClick={onClose}
                      className={({ isActive }) =>
                        `group relative flex items-center gap-3 px-3 py-2.5 rounded-xl text-[13.5px] font-medium transition-all duration-200 ${
                          isActive
                            ? 'bg-white/[0.08] text-white shadow-inner-highlight'
                            : 'text-slate-400 hover:text-slate-100 hover:bg-white/[0.04]'
                        }`
                      }
                    >
                      {({ isActive }) => (
                        <>
                          <span
                            className={`absolute left-0 top-1/2 -translate-y-1/2 h-5 w-[3px] rounded-r-full bg-indigo-400 transition-opacity ${
                              isActive ? 'opacity-100' : 'opacity-0'
                            }`}
                            aria-hidden="true"
                          />
                          <span
                            className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors shrink-0 ${
                              isActive
                                ? 'bg-indigo-500 text-white shadow-md shadow-indigo-600/40'
                                : 'bg-white/[0.04] text-slate-400 group-hover:text-slate-200 group-hover:bg-white/[0.07]'
                            }`}
                          >
                            <Icon className="w-[17px] h-[17px]" />
                          </span>
                          <span className="truncate">{item.label}</span>
                        </>
                      )}
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* User card & sign out */}
        <div className="p-3 shrink-0 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
          <div className="rounded-2xl bg-white/[0.04] ring-1 ring-white/[0.06] p-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="h-9 w-9 rounded-full bg-gradient-to-br from-amber-300 to-amber-500 text-amber-950 flex items-center justify-center text-xs font-bold shrink-0 ring-2 ring-white/10">
                {initialsOf(displayName)}
              </div>
              <div className="min-w-0">
                <p className="text-[13px] font-semibold text-white truncate">{displayName}</p>
                <p className="text-[11px] text-slate-400 truncate">{roleLabel(user?.role)}</p>
              </div>
            </div>
            <button
              onClick={logout}
              className="mt-3 w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl text-[13px] font-semibold text-slate-300 bg-white/[0.04] hover:bg-rose-500/15 hover:text-rose-300 ring-1 ring-white/[0.06] hover:ring-rose-400/30 transition-all"
            >
              <LogOut className="w-4 h-4" />
              <span>Sign out</span>
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};
