import React from 'react';
import { Link } from 'react-router-dom';
import { ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import {
  ArrowUpRight,
  ArrowDownRight,
  Minus,
  UtensilsCrossed,
  PencilLine,
  Trash2,
  LogIn,
  UserPlus,
  Settings2,
  ShieldCheck,
  Activity,
  FileSpreadsheet,
} from 'lucide-react';
import { AuditLog } from '../../types';
import { initialsOf } from '../layout/roleLabels';

/* =====================================================================
   Tones: one palette for icons, accents, sparklines and progress bars
   ===================================================================== */
export type Tone = 'indigo' | 'emerald' | 'rose' | 'amber' | 'sky' | 'violet' | 'slate';

export const TONES: Record<Tone, { tile: string; ring: string; text: string; stroke: string; soft: string; bar: string }> = {
  indigo: { tile: 'from-indigo-500 to-indigo-600', ring: 'ring-indigo-500/20', text: 'text-indigo-700', stroke: '#3F4FE2', soft: 'bg-indigo-50', bar: 'bg-indigo-500' },
  emerald: { tile: 'from-emerald-500 to-emerald-600', ring: 'ring-emerald-500/20', text: 'text-emerald-700', stroke: '#10B981', soft: 'bg-emerald-50', bar: 'bg-emerald-500' },
  rose: { tile: 'from-rose-500 to-rose-600', ring: 'ring-rose-500/20', text: 'text-rose-700', stroke: '#F43F5E', soft: 'bg-rose-50', bar: 'bg-rose-500' },
  amber: { tile: 'from-amber-400 to-amber-500', ring: 'ring-amber-500/20', text: 'text-amber-700', stroke: '#F59E0B', soft: 'bg-amber-50', bar: 'bg-amber-400' },
  sky: { tile: 'from-sky-500 to-sky-600', ring: 'ring-sky-500/20', text: 'text-sky-700', stroke: '#0EA5E9', soft: 'bg-sky-50', bar: 'bg-sky-500' },
  violet: { tile: 'from-violet-500 to-violet-600', ring: 'ring-violet-500/20', text: 'text-violet-700', stroke: '#8B5CF6', soft: 'bg-violet-50', bar: 'bg-violet-500' },
  slate: { tile: 'from-slate-600 to-slate-700', ring: 'ring-slate-500/20', text: 'text-slate-700', stroke: '#64748B', soft: 'bg-slate-100', bar: 'bg-slate-400' },
};

export const CHART = {
  ate: '#10B981',
  dne: '#F43F5E',
  cost: '#3F4FE2',
  grid: '#EEF1F6',
  axis: '#94A3B8',
};

/* =====================================================================
   Formatting
   ===================================================================== */
export const fmtNumber = (n: number | null | undefined): string =>
  n === null || n === undefined || Number.isNaN(n) ? '—' : Number(n).toLocaleString('en-US');

export const fmtMoney = (n: number | null | undefined, currency = 'RWF'): string =>
  n === null || n === undefined || Number.isNaN(n) ? '—' : `${Number(n).toLocaleString('en-US')} ${currency}`;

/** Compact money for axes: 12500 -> "12.5k". */
export const fmtCompact = (n: number): string =>
  Math.abs(n) >= 1_000_000 ? `${+(n / 1_000_000).toFixed(1)}M` : Math.abs(n) >= 1000 ? `${+(n / 1000).toFixed(1)}k` : `${n}`;

export const pct = (part: number, whole: number): number => (whole > 0 ? Math.round((part / whole) * 100) : 0);

/** "2026-11-04" -> "Wed, 4 Nov" (date-only values, no timezone shift). */
export const fmtDayLabel = (iso?: string, withYear = false): string => {
  if (!iso) return '—';
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d));
  return dt.toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    ...(withYear ? { year: 'numeric' } : {}),
    timeZone: 'UTC',
  });
};

export const greeting = (): string => {
  const hour = Number(
    new Intl.DateTimeFormat('en-GB', { hour: 'numeric', hour12: false, timeZone: 'Africa/Kigali' }).format(new Date())
  );
  return hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
};

/* =====================================================================
   Sparkline (pure SVG, no chart runtime)
   ===================================================================== */
export const Sparkline: React.FC<{ data: number[]; color: string; height?: number; id: string }> = ({
  data,
  color,
  height = 40,
  id,
}) => {
  if (!data || data.length < 2) return <div style={{ height }} />;
  const w = 120;
  const max = Math.max(...data, 1);
  const min = Math.min(...data, 0);
  const span = max - min || 1;
  const pts = data.map((v, i) => [(i / (data.length - 1)) * w, height - 4 - ((v - min) / span) * (height - 8)]);
  const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p[0].toFixed(1)},${p[1].toFixed(1)}`).join(' ');
  const area = `${line} L${w},${height} L0,${height} Z`;
  return (
    <svg viewBox={`0 0 ${w} ${height}`} preserveAspectRatio="none" className="w-full" style={{ height }} aria-hidden="true">
      <defs>
        <linearGradient id={`spark-${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.28" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#spark-${id})`} />
      <path d={line} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" vectorEffect="non-scaling-stroke" />
      <circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r="2.6" fill="#fff" stroke={color} strokeWidth="1.8" vectorEffect="non-scaling-stroke" />
    </svg>
  );
};

/* =====================================================================
   Delta chip: change vs a previous value
   ===================================================================== */
export const Delta: React.FC<{ current: number; previous: number | undefined; goodWhenUp?: boolean; label?: string }> = ({
  current,
  previous,
  goodWhenUp = true,
  label = 'vs prev. day',
}) => {
  if (previous === undefined) return null;
  if (previous === 0 && current === 0)
    return (
      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-500">
        <Minus className="w-3 h-3" /> No change <span className="font-medium text-slate-400">{label}</span>
      </span>
    );
  const change = previous === 0 ? 100 : Math.round(((current - previous) / previous) * 100);
  const up = change > 0;
  const flat = change === 0;
  const good = flat ? null : up === goodWhenUp;
  const color = flat ? 'text-slate-500 bg-slate-100' : good ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50';
  const Icon = flat ? Minus : up ? ArrowUpRight : ArrowDownRight;
  return (
    <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold">
      <span className={`inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 ${color}`}>
        <Icon className="w-3 h-3" />
        {Math.abs(change)}%
      </span>
      <span className="font-medium text-slate-400">{label}</span>
    </span>
  );
};

/* =====================================================================
   KPI card
   ===================================================================== */
export const KpiCard: React.FC<{
  label: string;
  value: React.ReactNode;
  unit?: string;
  icon: React.ComponentType<{ className?: string }>;
  tone?: Tone;
  hint?: React.ReactNode;
  spark?: number[];
  sparkId?: string;
  footer?: React.ReactNode;
  className?: string;
}> = ({ label, value, unit, icon: Icon, tone = 'indigo', hint, spark, sparkId, footer, className = '' }) => {
  const t = TONES[tone];
  return (
    <div
      className={`group relative overflow-hidden rounded-2xl bg-white ring-1 ring-slate-200/70 shadow-card p-4 sm:p-5 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-card-hover ${className}`}
    >
      <div className="flex items-start justify-between gap-3">
        <p className="text-[12.5px] font-semibold text-slate-500 leading-tight">{label}</p>
        <span
          className={`flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br ${t.tile} text-white shadow-sm ring-4 ${t.ring} shrink-0 transition-transform duration-300 group-hover:scale-105`}
        >
          <Icon className="h-[18px] w-[18px]" />
        </span>
      </div>
      <p className="mt-1 font-display text-[26px] sm:text-[28px] font-extrabold tracking-tight text-slate-900 leading-none tabular-nums">
        {value}
        {unit && <span className="ml-1.5 text-sm font-semibold text-slate-400 tracking-normal">{unit}</span>}
      </p>
      {hint && <div className="mt-2 text-xs text-slate-500">{hint}</div>}
      {spark && spark.length > 1 && (
        <div className="mt-3 -mx-1">
          <Sparkline data={spark} color={t.stroke} id={sparkId ?? label.replace(/\W+/g, '')} height={36} />
        </div>
      )}
      {footer && <div className="mt-3 pt-3 border-t border-slate-100 text-xs text-slate-500">{footer}</div>}
    </div>
  );
};

/* =====================================================================
   Panels and section headings
   ===================================================================== */
export const Panel: React.FC<{
  title: string;
  subtitle?: string;
  icon?: React.ComponentType<{ className?: string }>;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  bodyClassName?: string;
}> = ({ title, subtitle, icon: Icon, action, children, className = '', bodyClassName = '' }) => (
  <section className={`rounded-2xl bg-white ring-1 ring-slate-200/70 shadow-card flex flex-col min-w-0 ${className}`}>
    <header className="flex items-start justify-between gap-3 px-5 sm:px-6 pt-5 pb-4">
      <div className="flex items-start gap-3 min-w-0">
        {Icon && (
          <span className="mt-0.5 flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-500 shrink-0">
            <Icon className="h-4 w-4" />
          </span>
        )}
        <div className="min-w-0">
          <h3 className="font-display text-[15px] sm:text-base font-bold text-slate-900 leading-tight">{title}</h3>
          {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </header>
    <div className={`px-5 sm:px-6 pb-5 sm:pb-6 flex-1 ${bodyClassName}`}>{children}</div>
  </section>
);

export const LegendDot: React.FC<{ color: string; label: string; value?: React.ReactNode }> = ({ color, label, value }) => (
  <span className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-600">
    <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: color }} />
    {label}
    {value !== undefined && <span className="font-semibold text-slate-900 tabular-nums">{value}</span>}
  </span>
);

/* =====================================================================
   Chart tooltip (white card)
   ===================================================================== */
export const ChartTooltip: React.FC<any> = ({ active, payload, label, currency, labelMap }) => {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="rounded-xl bg-white/95 backdrop-blur px-3.5 py-2.5 shadow-elevated ring-1 ring-slate-900/5 min-w-[150px]">
      <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 mb-1.5">{label}</p>
      <div className="space-y-1">
        {payload.map((p: any) => (
          <div key={p.dataKey} className="flex items-center justify-between gap-4 text-[13px]">
            <span className="inline-flex items-center gap-1.5 text-slate-600">
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: p.color || p.fill || p.stroke }} />
              {labelMap?.[p.dataKey] ?? p.name}
            </span>
            <span className="font-semibold text-slate-900 tabular-nums">
              {p.dataKey === 'amount' || p.dataKey === 'totalAmount' ? fmtMoney(p.value, currency) : fmtNumber(p.value)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

/* =====================================================================
   Participation ring (donut with centre value)
   ===================================================================== */
export const ParticipationRing: React.FC<{
  segments: { name: string; value: number; color: string }[];
  centerValue: React.ReactNode;
  centerLabel: string;
  size?: number;
}> = ({ segments, centerValue, centerLabel, size = 200 }) => {
  const total = segments.reduce((s, x) => s + x.value, 0);
  const data = total > 0 ? segments : [{ name: 'empty', value: 1, color: '#EEF1F6' }];
  return (
    <div className="relative mx-auto" style={{ width: size, height: size }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            innerRadius="72%"
            outerRadius="100%"
            startAngle={90}
            endAngle={-270}
            paddingAngle={total > 0 && segments.filter((s) => s.value > 0).length > 1 ? 2 : 0}
            cornerRadius={6}
            stroke="none"
            isAnimationActive
            animationDuration={700}
          >
            {data.map((s) => (
              <Cell key={s.name} fill={s.color} />
            ))}
          </Pie>
        </PieChart>
      </ResponsiveContainer>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
        <span className="font-display text-3xl font-extrabold text-slate-900 tabular-nums leading-none">{centerValue}</span>
        <span className="mt-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-400">{centerLabel}</span>
      </div>
    </div>
  );
};

/* =====================================================================
   Segmented progress bar (ATE / DID NOT EAT / not recorded)
   ===================================================================== */
export const StackBar: React.FC<{ parts: { value: number; className: string; label: string }[]; total: number; height?: string }> = ({
  parts,
  total,
  height = 'h-2',
}) => (
  <div className={`flex w-full overflow-hidden rounded-full bg-slate-100 ${height}`} role="img" aria-label={parts.map((p) => `${p.label} ${p.value}`).join(', ')}>
    {parts.map((p) =>
      p.value > 0 ? (
        <div
          key={p.label}
          className={`${p.className} h-full transition-[width] duration-700 ease-out first:rounded-l-full last:rounded-r-full`}
          style={{ width: `${total > 0 ? (p.value / total) * 100 : 0}%` }}
          title={`${p.label}: ${p.value}`}
        />
      ) : null
    )}
  </div>
);

/* =====================================================================
   Activity timeline
   ===================================================================== */
const activityStyle = (action: string): { icon: React.ComponentType<{ className?: string }>; dot: string; chip: string } => {
  const a = action.toUpperCase();
  if (a.includes('DELETE') || a.includes('DEACTIVATE') || a.includes('REJECT'))
    return { icon: Trash2, dot: 'bg-rose-500', chip: 'bg-rose-50 text-rose-600 ring-rose-600/10' };
  if (a.includes('UPDATE') || a.includes('EDIT') || a.includes('TOGGLE'))
    return { icon: PencilLine, dot: 'bg-amber-500', chip: 'bg-amber-50 text-amber-600 ring-amber-600/10' };
  if (a.includes('MEAL')) return { icon: UtensilsCrossed, dot: 'bg-emerald-500', chip: 'bg-emerald-50 text-emerald-600 ring-emerald-600/10' };
  if (a.includes('LOGIN')) return { icon: LogIn, dot: 'bg-indigo-500', chip: 'bg-indigo-50 text-indigo-600 ring-indigo-600/10' };
  if (a.includes('CREATE') || a.includes('REGISTER') || a.includes('APPROVE'))
    return { icon: UserPlus, dot: 'bg-sky-500', chip: 'bg-sky-50 text-sky-600 ring-sky-600/10' };
  if (a.includes('IMPORT') || a.includes('REPORT')) return { icon: FileSpreadsheet, dot: 'bg-violet-500', chip: 'bg-violet-50 text-violet-600 ring-violet-600/10' };
  if (a.includes('SETTING')) return { icon: Settings2, dot: 'bg-slate-500', chip: 'bg-slate-100 text-slate-600 ring-slate-500/10' };
  if (a.includes('ADMIN') || a.includes('PASSWORD')) return { icon: ShieldCheck, dot: 'bg-indigo-500', chip: 'bg-indigo-50 text-indigo-600 ring-indigo-600/10' };
  return { icon: Activity, dot: 'bg-slate-400', chip: 'bg-slate-100 text-slate-600 ring-slate-500/10' };
};

const prettyAction = (action: string) =>
  action
    .toLowerCase()
    .split('_')
    .map((w, i) => (i === 0 ? w.charAt(0).toUpperCase() + w.slice(1) : w))
    .join(' ');

export const ActivityTimeline: React.FC<{ logs: AuditLog[]; emptyText?: string }> = ({ logs, emptyText = 'No recent activity yet.' }) => {
  if (!logs || logs.length === 0) {
    return (
      <div className="py-8 text-center">
        <div className="mx-auto mb-2 flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-100">
          <Activity className="h-5 w-5 text-slate-400" />
        </div>
        <p className="text-sm text-slate-500">{emptyText}</p>
      </div>
    );
  }
  return (
    <ol className="relative">
      {logs.map((log, i) => {
        const s = activityStyle(log.action);
        const Icon = s.icon;
        const last = i === logs.length - 1;
        return (
          <li key={log.id} className="relative flex gap-3.5 pb-5 last:pb-0">
            {!last && <span className="absolute left-[17px] top-10 bottom-1 w-px bg-slate-200" aria-hidden="true" />}
            <span className={`relative z-10 flex h-9 w-9 items-center justify-center rounded-xl ring-1 ring-inset shrink-0 ${s.chip}`}>
              <Icon className="h-4 w-4" />
            </span>
            <div className="min-w-0 flex-1 pt-0.5">
              <div className="flex items-center justify-between gap-2">
                <p className="text-[13px] font-semibold text-slate-900 truncate">{prettyAction(log.action)}</p>
                <time className="text-[11px] font-medium text-slate-400 whitespace-nowrap tabular-nums">
                  {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </time>
              </div>
              <p className="mt-0.5 text-xs text-slate-500 leading-snug line-clamp-2" title={log.description}>
                {log.description}
              </p>
              <div className="mt-1.5 flex items-center gap-1.5">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-100 text-[9px] font-bold text-slate-600">
                  {initialsOf(log.username)}
                </span>
                <span className="text-[11px] font-medium text-slate-500">{log.username}</span>
                <span className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">· {log.userRole}</span>
              </div>
            </div>
          </li>
        );
      })}
    </ol>
  );
};

/* =====================================================================
   Quick action tiles
   ===================================================================== */
export const QuickAction: React.FC<{
  to: string;
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  text: string;
  tone?: Tone;
}> = ({ to, icon: Icon, title, text, tone = 'indigo' }) => {
  const t = TONES[tone];
  return (
    <Link
      to={to}
      className="group flex items-center gap-3.5 rounded-xl p-3 ring-1 ring-slate-200/80 bg-white hover:ring-slate-300 hover:shadow-card-hover hover:-translate-y-px transition-all"
    >
      <span className={`flex h-10 w-10 items-center justify-center rounded-xl ${t.soft} ${t.text} shrink-0 transition-transform group-hover:scale-105`}>
        <Icon className="h-[18px] w-[18px]" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-semibold text-slate-900">{title}</span>
        <span className="block text-xs text-slate-500 truncate">{text}</span>
      </span>
      <ArrowUpRight className="h-4 w-4 text-slate-300 group-hover:text-slate-600 transition-colors shrink-0" />
    </Link>
  );
};

/* =====================================================================
   Hero banner
   ===================================================================== */
export const HeroStat: React.FC<{ label: string; value: React.ReactNode; sub?: React.ReactNode }> = ({ label, value, sub }) => (
  <div className="rounded-2xl bg-white/[0.07] ring-1 ring-white/10 px-3 sm:px-4 py-3 backdrop-blur-sm min-w-0">
    <p className="text-[10px] sm:text-[11px] font-semibold uppercase tracking-wider text-white/55 truncate">{label}</p>
    <p className="mt-1 font-display text-lg sm:text-2xl font-extrabold text-white tabular-nums leading-none truncate">{value}</p>
    {sub && <p className="mt-1.5 text-[11px] text-white/60 truncate">{sub}</p>}
  </div>
);

export const Hero: React.FC<{
  eyebrow: string;
  title: React.ReactNode;
  subtitle: React.ReactNode;
  stats?: React.ReactNode;
  actions?: React.ReactNode;
  accent?: 'royal' | 'emerald' | 'amber' | 'ink';
}> = ({ eyebrow, title, subtitle, stats, actions, accent = 'royal' }) => {
  const glow = {
    royal: 'radial-gradient(600px 300px at 100% 0%, rgba(122,141,245,0.35), transparent 70%)',
    emerald: 'radial-gradient(600px 300px at 100% 0%, rgba(16,185,129,0.30), transparent 70%)',
    amber: 'radial-gradient(600px 300px at 100% 0%, rgba(245,158,11,0.30), transparent 70%)',
    ink: 'radial-gradient(600px 300px at 100% 0%, rgba(148,163,184,0.25), transparent 70%)',
  }[accent];
  return (
    <section
      className="relative overflow-hidden rounded-3xl text-white shadow-elevated"
      style={{ background: `${glow}, linear-gradient(135deg, #0F1729 0%, #1A2150 60%, #2A347D 100%)` }}
    >
      <div
        className="absolute inset-0 opacity-[0.07] pointer-events-none"
        style={{
          backgroundImage: 'linear-gradient(rgba(255,255,255,.6) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.6) 1px, transparent 1px)',
          backgroundSize: '36px 36px',
          maskImage: 'linear-gradient(90deg, transparent, black 60%)',
          WebkitMaskImage: 'linear-gradient(90deg, transparent, black 60%)',
        }}
        aria-hidden="true"
      />
      <div className="relative p-5 sm:p-7 lg:p-8 flex flex-col xl:flex-row xl:items-end justify-between gap-6">
        <div className="min-w-0 max-w-2xl">
          <span className="inline-flex items-center gap-2 rounded-full bg-white/10 ring-1 ring-white/15 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-white/85">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
            {eyebrow}
          </span>
          <h2 className="mt-3 font-display text-2xl sm:text-[28px] lg:text-[32px] font-extrabold tracking-tight leading-tight">{title}</h2>
          <div className="mt-2 text-sm sm:text-[15px] text-white/70 leading-relaxed">{subtitle}</div>
          {actions && <div className="mt-5 flex flex-wrap gap-2.5">{actions}</div>}
        </div>
        {stats && <div className="grid grid-cols-3 gap-2 sm:gap-3 xl:min-w-[440px]">{stats}</div>}
      </div>
    </section>
  );
};

export const HeroButton: React.FC<{ to: string; children: React.ReactNode; primary?: boolean }> = ({ to, children, primary }) => (
  <Link
    to={to}
    className={`inline-flex items-center gap-2 rounded-xl px-4 h-10 text-sm font-semibold transition-all hover:-translate-y-px ${
      primary
        ? 'bg-white text-slate-900 shadow-lg shadow-black/20 hover:bg-indigo-50'
        : 'bg-white/10 text-white ring-1 ring-white/20 hover:bg-white/15'
    }`}
  >
    {children}
  </Link>
);

/* =====================================================================
   PageHero: compact banner used at the top of every operational page
   ===================================================================== */
export const PageHero: React.FC<{
  icon: React.ComponentType<{ className?: string }>;
  eyebrow: string;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  actions?: React.ReactNode;
  stats?: React.ReactNode;
  className?: string;
}> = ({ icon: Icon, eyebrow, title, subtitle, actions, stats, className = '' }) => (
  <section
    className={`relative overflow-hidden rounded-3xl text-white shadow-elevated print:hidden ${className}`}
    style={{
      background:
        'radial-gradient(520px 260px at 100% 0%, rgba(122,141,245,0.32), transparent 70%), radial-gradient(420px 220px at 0% 120%, rgba(245,158,11,0.16), transparent 70%), linear-gradient(135deg, #0F1729 0%, #1A2150 60%, #2A347D 100%)',
    }}
  >
    <div
      className="absolute inset-0 opacity-[0.06] pointer-events-none"
      style={{
        backgroundImage:
          'linear-gradient(rgba(255,255,255,.6) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.6) 1px, transparent 1px)',
        backgroundSize: '32px 32px',
        maskImage: 'linear-gradient(90deg, transparent, black 70%)',
        WebkitMaskImage: 'linear-gradient(90deg, transparent, black 70%)',
      }}
      aria-hidden="true"
    />
    <div className="relative p-5 sm:p-6 lg:p-7">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-5">
        <div className="flex items-start gap-4 min-w-0">
          <span className="hidden sm:flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/15 backdrop-blur-md shrink-0">
            <Icon className="h-6 w-6 text-white" />
          </span>
          <div className="min-w-0">
            <span className="inline-flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-white/60">
              <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
              {eyebrow}
            </span>
            <h2 className="mt-1.5 font-display text-2xl sm:text-[28px] font-extrabold tracking-tight leading-tight">{title}</h2>
            {subtitle && <div className="mt-1.5 text-sm text-white/70 leading-relaxed max-w-2xl">{subtitle}</div>}
          </div>
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2 lg:justify-end shrink-0">{actions}</div>}
      </div>
      {stats && <div className="mt-5 grid grid-cols-2 sm:grid-cols-4 gap-2 sm:gap-3">{stats}</div>}
    </div>
  </section>
);

/** Button styles for use inside PageHero (keeps existing onClick handlers). */
export const heroBtn = {
  primary:
    'inline-flex items-center justify-center gap-2 rounded-xl px-4 h-10 text-sm font-semibold bg-white text-slate-900 shadow-lg shadow-black/20 hover:bg-indigo-50 hover:-translate-y-px transition-all disabled:opacity-60 disabled:cursor-not-allowed',
  ghost:
    'inline-flex items-center justify-center gap-2 rounded-xl px-4 h-10 text-sm font-semibold bg-white/10 text-white ring-1 ring-white/20 hover:bg-white/15 hover:-translate-y-px transition-all disabled:opacity-60 disabled:cursor-not-allowed',
};

/** Filter / toolbar card that sits under a PageHero. */
export const Toolbar: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => (
  <div className={`rounded-2xl bg-white ring-1 ring-slate-200/70 shadow-card p-3.5 sm:p-4 print:hidden ${className}`}>{children}</div>
);
