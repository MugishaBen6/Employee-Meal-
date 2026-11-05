import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { dashboardApi } from '../api/dashboardApi';
import { DashboardStats } from '../types';
import { Skeleton } from '../components/common/Skeleton';
import {
  Users,
  CheckCircle2,
  XCircle,
  Wallet,
  Coins,
  CalendarDays,
  CalendarRange,
  UtensilsCrossed,
  Clock3,
  TrendingUp,
  Building2,
  Activity,
  BarChart3,
  FileSpreadsheet,
  Receipt,
  UserCog,
  Settings2,
  ClipboardList,
  Gauge,
  Sparkles,
  RefreshCw,
  AlertTriangle,
  ArrowRight,
  PieChart as PieIcon,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
  ComposedChart,
  Line,
  Cell,
  LabelList,
} from 'recharts';
import {
  KpiCard,
  Panel,
  Hero,
  HeroStat,
  HeroButton,
  ChartTooltip,
  ParticipationRing,
  StackBar,
  ActivityTimeline,
  QuickAction,
  LegendDot,
  Delta,
  CHART,
  fmtMoney,
  fmtNumber,
  fmtCompact,
  fmtDayLabel,
  pct,
  greeting,
} from '../components/dashboard/DashboardKit';

/* ---------------------------------------------------------------------
   Derived, read-only figures (all computed from the real statistics)
   --------------------------------------------------------------------- */
const useDerived = (stats: DashboardStats | null) =>
  useMemo(() => {
    if (!stats) return null;
    const days = stats.dailyExpenditures ?? [];
    const last = days[days.length - 1];
    const prev = days.length > 1 ? days[days.length - 2] : undefined;
    const staff = stats.totalEmployees ?? 0;
    const ate = stats.ateToday ?? 0;
    const dne = stats.didNotEatToday ?? 0;
    const notRecorded = Math.max(0, staff - ate - dne);
    const windowCost = days.reduce((s, d) => s + (Number(d.amount) || 0), 0);
    const windowMeals = days.reduce((s, d) => s + (d.ateCount || 0), 0);
    const peak = days.reduce<(typeof days)[number] | undefined>((m, d) => (!m || d.amount > m.amount ? d : m), undefined);
    const departments = [...(stats.departmentStats ?? [])].sort((a, b) => b.totalAmount - a.totalAmount || b.ateCount - a.ateCount);
    return {
      days,
      last,
      prev,
      staff,
      ate,
      dne,
      notRecorded,
      participation: pct(ate, staff),
      dayLabel: last ? fmtDayLabel(last.date, true) : 'today',
      dayShort: last ? fmtDayLabel(last.date) : 'Today',
      windowCost,
      windowMeals,
      avgDailyCost: days.length ? Math.round(windowCost / days.length) : 0,
      peak,
      costSpark: days.map((d) => Number(d.amount) || 0),
      ateSpark: days.map((d) => d.ateCount || 0),
      dneSpark: days.map((d) => d.didNotEatCount || 0),
      departments,
    };
  }, [stats]);

type Derived = NonNullable<ReturnType<typeof useDerived>>;

/* ---------------------------------------------------------------------
   Charts
   --------------------------------------------------------------------- */
const AttendanceChart: React.FC<{ d: Derived; height?: number }> = ({ d, height = 280 }) => (
  <div style={{ height }} className="w-full">
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={d.days} margin={{ top: 8, right: 4, left: -18, bottom: 0 }} barCategoryGap="28%">
        <CartesianGrid vertical={false} stroke={CHART.grid} />
        <XAxis dataKey="formattedDate" tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} dy={6} />
        <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} />
        <Tooltip
          cursor={{ fill: 'rgba(63,79,226,0.05)', radius: 8 }}
          content={<ChartTooltip labelMap={{ ateCount: 'Ate', didNotEatCount: 'Did not eat' }} />}
        />
        <Bar dataKey="ateCount" name="Ate" stackId="m" fill={CHART.ate} radius={[0, 0, 0, 0]} maxBarSize={34} />
        <Bar dataKey="didNotEatCount" name="Did not eat" stackId="m" fill={CHART.dne} radius={[6, 6, 0, 0]} maxBarSize={34} />
      </BarChart>
    </ResponsiveContainer>
  </div>
);

const CostTrendChart: React.FC<{ d: Derived; currency: string; height?: number; id: string }> = ({ d, currency, height = 280, id }) => (
  <div style={{ height }} className="w-full">
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={d.days} margin={{ top: 8, right: 6, left: -6, bottom: 0 }}>
        <defs>
          <linearGradient id={`cost-${id}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={CHART.cost} stopOpacity={0.32} />
            <stop offset="100%" stopColor={CHART.cost} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} stroke={CHART.grid} />
        <XAxis dataKey="formattedDate" tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} dy={6} />
        <YAxis tickFormatter={fmtCompact} tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} width={44} />
        <Tooltip
          cursor={{ stroke: CHART.cost, strokeOpacity: 0.25, strokeWidth: 1.5, strokeDasharray: '4 4' }}
          content={<ChartTooltip currency={currency} labelMap={{ amount: 'Meal cost' }} />}
        />
        <Area
          type="monotone"
          dataKey="amount"
          name="Meal cost"
          stroke={CHART.cost}
          strokeWidth={2.5}
          fill={`url(#cost-${id})`}
          activeDot={{ r: 5, strokeWidth: 2, stroke: '#fff', fill: CHART.cost }}
        />
      </AreaChart>
    </ResponsiveContainer>
  </div>
);

const MealsAndCostChart: React.FC<{ d: Derived; currency: string }> = ({ d, currency }) => (
  <div className="h-[300px] w-full">
    <ResponsiveContainer width="100%" height="100%">
      <ComposedChart data={d.days} margin={{ top: 8, right: 0, left: -18, bottom: 0 }}>
        <defs>
          <linearGradient id="exec-bars" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={CHART.ate} stopOpacity={0.95} />
            <stop offset="100%" stopColor={CHART.ate} stopOpacity={0.55} />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} stroke={CHART.grid} />
        <XAxis dataKey="formattedDate" tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} dy={6} />
        <YAxis yAxisId="meals" allowDecimals={false} tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} />
        <YAxis yAxisId="cost" orientation="right" tickFormatter={fmtCompact} tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} width={44} />
        <Tooltip
          cursor={{ fill: 'rgba(63,79,226,0.05)' }}
          content={<ChartTooltip currency={currency} labelMap={{ ateCount: 'Meals served', amount: 'Meal cost' }} />}
        />
        <Bar yAxisId="meals" dataKey="ateCount" name="Meals served" fill="url(#exec-bars)" radius={[6, 6, 0, 0]} maxBarSize={30} />
        <Line
          yAxisId="cost"
          type="monotone"
          dataKey="amount"
          name="Meal cost"
          stroke={CHART.cost}
          strokeWidth={2.5}
          dot={{ r: 3, fill: '#fff', stroke: CHART.cost, strokeWidth: 2 }}
          activeDot={{ r: 5, strokeWidth: 2, stroke: '#fff', fill: CHART.cost }}
        />
      </ComposedChart>
    </ResponsiveContainer>
  </div>
);

const DepartmentCostChart: React.FC<{ d: Derived; currency: string }> = ({ d, currency }) => {
  const data = d.departments.slice(0, 8);
  if (data.length === 0) return <EmptyNote text="No department costs for this day." />;
  const palette = ['#3F4FE2', '#5568EE', '#7A8DF5', '#A6B6FA', '#CBD5FD', '#CBD5FD', '#CBD5FD', '#CBD5FD'];
  return (
    <div style={{ height: Math.max(180, data.length * 38) }} className="w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical" margin={{ top: 0, right: 40, left: 0, bottom: 0 }} barCategoryGap="22%">
          <XAxis type="number" hide />
          <YAxis
            type="category"
            dataKey="department"
            width={96}
            tickLine={false}
            axisLine={false}
            tick={{ fill: '#475569', fontSize: 12, fontWeight: 500 }}
          />
          <Tooltip cursor={{ fill: 'rgba(63,79,226,0.05)' }} content={<ChartTooltip currency={currency} labelMap={{ totalAmount: 'Cost' }} />} />
          <Bar dataKey="totalAmount" name="Cost" radius={[0, 6, 6, 0]} maxBarSize={22} minPointSize={3}>
            {data.map((_, i) => (
              <Cell key={i} fill={palette[i] ?? palette[palette.length - 1]} />
            ))}
            <LabelList
              dataKey="totalAmount"
              position="right"
              formatter={(v: any) => fmtCompact(Number(v) || 0)}
              style={{ fill: '#334155', fontSize: 11, fontWeight: 600 }}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

/* ---------------------------------------------------------------------
   Building blocks
   --------------------------------------------------------------------- */
const EmptyNote: React.FC<{ text: string }> = ({ text }) => (
  <div className="py-10 text-center text-sm text-slate-500">{text}</div>
);

const DepartmentBreakdown: React.FC<{ d: Derived; currency: string; showCost?: boolean; rankBy?: 'cost' | 'participation' }> = ({
  d,
  currency,
  showCost = true,
  rankBy = 'cost',
}) => {
  const rows =
    rankBy === 'participation'
      ? [...d.departments].sort((a, b) => pct(b.ateCount, b.totalEmployees) - pct(a.ateCount, a.totalEmployees))
      : d.departments;
  if (rows.length === 0) return <EmptyNote text="No department data for this day." />;
  return (
    <ul className="space-y-4">
      {rows.map((dept) => {
        const notRec = Math.max(0, dept.totalEmployees - dept.ateCount - dept.didNotEatCount);
        const rate = pct(dept.ateCount, dept.totalEmployees);
        return (
          <li key={dept.department}>
            <div className="flex items-center justify-between gap-3 mb-1.5">
              <div className="flex items-center gap-2.5 min-w-0">
                <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-100 text-[11px] font-bold text-slate-600 shrink-0">
                  {dept.department.charAt(0)}
                </span>
                <span className="text-sm font-semibold text-slate-800 truncate">{dept.department}</span>
                <span className="text-xs text-slate-400 whitespace-nowrap">{dept.totalEmployees} staff</span>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className="text-xs font-semibold text-slate-500 tabular-nums">{rate}%</span>
                {showCost && (
                  <span className="text-sm font-bold text-slate-900 tabular-nums w-[92px] text-right">{fmtMoney(dept.totalAmount, currency)}</span>
                )}
              </div>
            </div>
            <StackBar
              total={Math.max(dept.totalEmployees, dept.ateCount + dept.didNotEatCount)}
              parts={[
                { value: dept.ateCount, className: 'bg-emerald-500', label: 'Ate' },
                { value: dept.didNotEatCount, className: 'bg-rose-400', label: 'Did not eat' },
                { value: notRec, className: 'bg-slate-200', label: 'Not recorded' },
              ]}
            />
          </li>
        );
      })}
    </ul>
  );
};

const AttendanceLegend: React.FC = () => (
  <div className="flex flex-wrap gap-x-4 gap-y-1">
    <LegendDot color={CHART.ate} label="Ate" />
    <LegendDot color={CHART.dne} label="Did not eat" />
  </div>
);

const ParticipationPanel: React.FC<{ d: Derived; title?: string }> = ({ d, title = 'Participation' }) => (
  <Panel title={title} subtitle={`Active staff on ${d.dayLabel}`} icon={PieIcon}>
    <div className="flex flex-col items-center">
      <ParticipationRing
        size={196}
        centerValue={`${d.participation}%`}
        centerLabel="ate"
        segments={[
          { name: 'Ate', value: d.ate, color: CHART.ate },
          { name: 'Did not eat', value: d.dne, color: CHART.dne },
          { name: 'Not recorded', value: d.notRecorded, color: '#E2E8F0' },
        ]}
      />
      <div className="mt-5 grid grid-cols-3 w-full divide-x divide-slate-100 rounded-xl ring-1 ring-slate-100">
        {[
          { label: 'Ate', value: d.ate, color: 'text-emerald-600' },
          { label: 'Did not eat', value: d.dne, color: 'text-rose-600' },
          { label: 'Not recorded', value: d.notRecorded, color: 'text-slate-500' },
        ].map((x) => (
          <div key={x.label} className="py-2.5 text-center">
            <p className={`font-display text-lg font-extrabold tabular-nums ${x.color}`}>{fmtNumber(x.value)}</p>
            <p className="text-[11px] font-medium text-slate-500">{x.label}</p>
          </div>
        ))}
      </div>
    </div>
  </Panel>
);

const SpendSummary: React.FC<{ stats: DashboardStats; d: Derived }> = ({ stats, d }) => {
  const rows = [
    { label: d.dayShort, value: stats.todayTotalCost, icon: CalendarDays },
    { label: 'This week', value: stats.thisWeekTotalCost, icon: CalendarRange },
    { label: 'This month', value: stats.thisMonthTotalCost, icon: Wallet },
  ];
  const max = Math.max(...rows.map((r) => r.value || 0), 1);
  return (
    <ul className="space-y-4">
      {rows.map((r) => (
        <li key={r.label}>
          <div className="flex items-center justify-between gap-3 mb-1.5">
            <span className="inline-flex items-center gap-2 text-sm font-medium text-slate-600">
              <r.icon className="h-4 w-4 text-slate-400" />
              {r.label}
            </span>
            <span className="text-sm font-bold text-slate-900 tabular-nums">{fmtMoney(r.value, stats.currency)}</span>
          </div>
          <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-indigo-400 transition-[width] duration-700"
              style={{ width: `${Math.max(2, ((r.value || 0) / max) * 100)}%` }}
            />
          </div>
        </li>
      ))}
      <li className="pt-3 border-t border-slate-100 flex items-center justify-between text-sm">
        <span className="text-slate-500">Average cost per meal ({d.dayShort})</span>
        <span className="font-bold text-slate-900 tabular-nums">{fmtMoney(stats.averageMealCostToday, stats.currency)}</span>
      </li>
    </ul>
  );
};

/* ---------------------------------------------------------------------
   Role layouts
   --------------------------------------------------------------------- */
interface LayoutProps {
  stats: DashboardStats;
  d: Derived;
  name: string;
}

/** ADMIN: comprehensive overview of the whole system. */
const AdminDashboard: React.FC<LayoutProps> = ({ stats, d, name }) => (
  <>
    <Hero
      eyebrow="Administrator · System overview"
      title={`${greeting()}, ${name}`}
      subtitle={<>Everything happening across meals, people and spending, with the latest attendance from <b className="text-white">{d.dayLabel}</b>.</>}
      actions={
        <>
          <HeroButton to="/meals" primary>
            <UtensilsCrossed className="h-4 w-4" /> Record meals
          </HeroButton>
          <HeroButton to="/reports">
            <FileSpreadsheet className="h-4 w-4" /> Reports
          </HeroButton>
          <HeroButton to="/users">
            <UserCog className="h-4 w-4" /> Users
          </HeroButton>
        </>
      }
      stats={
        <>
          <HeroStat label="Participation" value={`${d.participation}%`} sub={`${fmtNumber(d.ate)} of ${fmtNumber(d.staff)} ate`} />
          <HeroStat label="Day cost" value={fmtCompact(stats.todayTotalCost || 0)} sub={`${stats.currency} · ${d.dayShort}`} />
          <HeroStat label="This month" value={fmtCompact(stats.thisMonthTotalCost || 0)} sub={stats.currency} />
        </>
      }
    />

    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
      <KpiCard
        label="Total meal cost"
        value={fmtNumber(stats.totalExpenseCost ?? 0)}
        unit={stats.currency}
        icon={Wallet}
        tone="indigo"
        spark={d.costSpark}
        sparkId="admin-cost"
        hint={<Delta current={d.last?.amount ?? 0} previous={d.prev?.amount} goodWhenUp={false} />}
      />
      <KpiCard
        label="Total meals served"
        value={fmtNumber(stats.totalMealsCount ?? 0)}
        unit="meals"
        icon={UtensilsCrossed}
        tone="emerald"
        spark={d.ateSpark}
        sparkId="admin-meals"
        hint={<Delta current={d.last?.ateCount ?? 0} previous={d.prev?.ateCount} />}
      />
      <KpiCard
        label="Active staff"
        value={fmtNumber(stats.totalEmployees)}
        unit="people"
        icon={Users}
        tone="sky"
        footer={
          <div className="flex items-center justify-between">
            <span>{d.dayShort}</span>
            <span className="font-semibold text-slate-700 tabular-nums">
              {fmtNumber(d.ate)} ate · {fmtNumber(d.dne)} did not
            </span>
          </div>
        }
      />
      <KpiCard
        label="Standard meal rate"
        value={stats.standardMealPrice ? fmtNumber(stats.standardMealPrice) : '—'}
        unit={stats.currency}
        icon={Coins}
        tone="amber"
        footer={
          <div className="flex items-center justify-between">
            <span>Avg. per meal ({d.dayShort})</span>
            <span className="font-semibold text-slate-700 tabular-nums">{fmtMoney(stats.averageMealCostToday, stats.currency)}</span>
          </div>
        }
      />
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Panel
        className="xl:col-span-2"
        title="Daily attendance"
        subtitle={`Ate vs did not eat · last ${d.days.length} days`}
        icon={BarChart3}
        action={<AttendanceLegend />}
      >
        <AttendanceChart d={d} />
      </Panel>
      <ParticipationPanel d={d} />
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Panel
        className="xl:col-span-2"
        title="Meal cost trend"
        subtitle={`Daily spending · last ${d.days.length} days (${stats.currency})`}
        icon={TrendingUp}
        action={
          <span className="text-xs text-slate-500">
            {d.days.length}-day total <b className="text-slate-900 tabular-nums">{fmtMoney(d.windowCost, stats.currency)}</b>
          </span>
        }
      >
        <CostTrendChart d={d} currency={stats.currency} id="admin" />
      </Panel>
      <Panel title="Spending summary" subtitle="Recorded meal costs" icon={Receipt}>
        <SpendSummary stats={stats} d={d} />
      </Panel>
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Panel className="xl:col-span-2" title="Departments" subtitle={`Attendance and cost · ${d.dayLabel}`} icon={Building2} action={<AttendanceLegend />}>
        <DepartmentBreakdown d={d} currency={stats.currency} />
      </Panel>
      <Panel title="Recent activity" subtitle="Latest audit trail entries" icon={Activity} action={<ViewAll to="/audit-logs" />}>
        <ActivityTimeline logs={stats.recentActivities} />
      </Panel>
    </div>
  </>
);

/** HR: people and meal-recording operations. */
const HrDashboard: React.FC<LayoutProps> = ({ stats, d, name }) => (
  <>
    <Hero
      accent="emerald"
      eyebrow="Human Resources · People & meals"
      title={`${greeting()}, ${name}`}
      subtitle={
        d.notRecorded > 0 ? (
          <>
            <b className="text-white">{fmtNumber(d.notRecorded)}</b> active {d.notRecorded === 1 ? 'employee has' : 'employees have'} no meal record for{' '}
            <b className="text-white">{d.dayLabel}</b>.
          </>
        ) : (
          <>Every active employee has a meal record for <b className="text-white">{d.dayLabel}</b>.</>
        )
      }
      actions={
        <>
          <HeroButton to="/meals" primary>
            <UtensilsCrossed className="h-4 w-4" /> Record meals
          </HeroButton>
          <HeroButton to="/employees">
            <Users className="h-4 w-4" /> Employees
          </HeroButton>
        </>
      }
      stats={
        <>
          <HeroStat label="Active staff" value={fmtNumber(d.staff)} />
          <HeroStat label="Recorded" value={fmtNumber(d.ate + d.dne)} sub={d.dayShort} />
          <HeroStat label="Participation" value={`${d.participation}%`} sub="ate" />
        </>
      }
    />

    <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
      <KpiCard label="Active staff" value={fmtNumber(d.staff)} icon={Users} tone="sky" hint="Employees with active status" />
      <KpiCard
        label={`Ate · ${d.dayShort}`}
        value={fmtNumber(d.ate)}
        icon={CheckCircle2}
        tone="emerald"
        spark={d.ateSpark}
        sparkId="hr-ate"
        hint={<Delta current={d.ate} previous={d.prev?.ateCount} />}
      />
      <KpiCard
        label={`Did not eat · ${d.dayShort}`}
        value={fmtNumber(d.dne)}
        icon={XCircle}
        tone="rose"
        spark={d.dneSpark}
        sparkId="hr-dne"
        hint={<Delta current={d.dne} previous={d.prev?.didNotEatCount} goodWhenUp={false} />}
      />
      <KpiCard
        label={`Not recorded · ${d.dayShort}`}
        value={fmtNumber(d.notRecorded)}
        icon={Clock3}
        tone="amber"
        footer={
          <div className="space-y-1.5">
            <StackBar
              total={Math.max(d.staff, d.ate + d.dne)}
              parts={[
                { value: d.ate, className: 'bg-emerald-500', label: 'Ate' },
                { value: d.dne, className: 'bg-rose-400', label: 'Did not eat' },
                { value: d.notRecorded, className: 'bg-amber-300', label: 'Not recorded' },
              ]}
            />
            <span>{pct(d.ate + d.dne, d.staff)}% of staff recorded</span>
          </div>
        }
      />
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <ParticipationPanel d={d} title="Attendance split" />
      <Panel
        className="xl:col-span-2"
        title="Attendance trend"
        subtitle={`Ate vs did not eat · last ${d.days.length} days`}
        icon={BarChart3}
        action={<AttendanceLegend />}
      >
        <AttendanceChart d={d} height={300} />
      </Panel>
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Panel className="xl:col-span-2" title="Departments" subtitle={`Who has eaten · ${d.dayLabel}`} icon={Building2} action={<AttendanceLegend />}>
        <DepartmentBreakdown d={d} currency={stats.currency} showCost={false} rankBy="participation" />
      </Panel>
      <div className="flex flex-col gap-5">
        <Panel title="Quick actions" icon={Sparkles}>
          <div className="space-y-2.5">
            <QuickAction to="/meals" icon={UtensilsCrossed} title="Record meals" text="ATE / DID NOT EAT for any date" tone="emerald" />
            <QuickAction to="/employees" icon={Users} title="Employees" text="Attendance, add and edit staff" tone="sky" />
            <QuickAction to="/reports" icon={FileSpreadsheet} title="Reports" text="Date-range PDF and Excel" tone="indigo" />
          </div>
        </Panel>
        <Panel title="Recent activity" icon={Activity}>
          <ActivityTimeline logs={stats.recentActivities} />
        </Panel>
      </div>
    </div>
  </>
);

/** ACCOUNTANT: money first. */
const AccountantDashboard: React.FC<LayoutProps> = ({ stats, d, name }) => (
  <>
    <Hero
      accent="amber"
      eyebrow="Accountant · Meal expenses"
      title={
        <>
          {fmtNumber(stats.thisMonthTotalCost)} <span className="text-white/60 text-xl sm:text-2xl font-bold">{stats.currency}</span>
        </>
      }
      subtitle={<>Month-to-date meal spending, {name}. Figures are the sum of the amounts stored on each meal record.</>}
      actions={
        <>
          <HeroButton to="/reports" primary>
            <FileSpreadsheet className="h-4 w-4" /> Reports & exports
          </HeroButton>
          <HeroButton to="/expenses">
            <Receipt className="h-4 w-4" /> Expenses
          </HeroButton>
        </>
      }
      stats={
        <>
          <HeroStat label="This week" value={fmtCompact(stats.thisWeekTotalCost || 0)} sub={stats.currency} />
          <HeroStat label={d.dayShort} value={fmtCompact(stats.todayTotalCost || 0)} sub={stats.currency} />
          <HeroStat label="Per meal" value={fmtNumber(stats.averageMealCostToday)} sub={`${stats.currency} average`} />
        </>
      }
    />

    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
      <KpiCard
        label={`Cost · ${d.dayShort}`}
        value={fmtNumber(stats.todayTotalCost)}
        unit={stats.currency}
        icon={CalendarDays}
        tone="indigo"
        hint={<Delta current={d.last?.amount ?? 0} previous={d.prev?.amount} goodWhenUp={false} />}
      />
      <KpiCard label="This week" value={fmtNumber(stats.thisWeekTotalCost)} unit={stats.currency} icon={CalendarRange} tone="violet" hint="Monday to the latest day" />
      <KpiCard label="This month" value={fmtNumber(stats.thisMonthTotalCost)} unit={stats.currency} icon={Wallet} tone="amber" hint="Month to date" />
      <KpiCard
        label="All-time meal cost"
        value={fmtNumber(stats.totalExpenseCost ?? 0)}
        unit={stats.currency}
        icon={Coins}
        tone="emerald"
        spark={d.costSpark}
        sparkId="acc-cost"
        hint={`${fmtNumber(stats.totalMealsCount ?? 0)} meals served`}
      />
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Panel
        className="xl:col-span-2"
        title="Spending trend"
        subtitle={`Daily meal cost · last ${d.days.length} days (${stats.currency})`}
        icon={TrendingUp}
        action={
          d.peak ? (
            <span className="text-xs text-slate-500">
              Peak <b className="text-slate-900">{d.peak.formattedDate}</b> · <b className="text-slate-900 tabular-nums">{fmtMoney(d.peak.amount, stats.currency)}</b>
            </span>
          ) : undefined
        }
      >
        <CostTrendChart d={d} currency={stats.currency} height={300} id="acc" />
      </Panel>
      <Panel title="Cost by department" subtitle={d.dayLabel} icon={Building2}>
        <DepartmentCostChart d={d} currency={stats.currency} />
      </Panel>
    </div>

    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Panel className="xl:col-span-2" title="Daily cost ledger" subtitle={`Last ${d.days.length} days`} icon={ClipboardList} bodyClassName="px-0 sm:px-0 pb-2 sm:pb-2">
        <div className="overflow-x-auto">
          <table className="w-full text-sm min-w-[460px]">
            <thead>
              <tr className="border-y border-slate-100 bg-slate-50/70 text-left">
                <th className="px-5 sm:px-6 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Date</th>
                <th className="px-3 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500">Ate</th>
                <th className="px-3 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500">Did not eat</th>
                <th className="px-5 sm:px-6 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500">Cost</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {[...d.days].reverse().map((day) => (
                <tr key={day.date} className="hover:bg-slate-50/70 transition-colors">
                  <td className="px-5 sm:px-6 py-2.5 font-medium text-slate-700">{fmtDayLabel(day.date, true)}</td>
                  <td className="px-3 py-2.5 text-right tabular-nums text-emerald-700 font-semibold">{fmtNumber(day.ateCount)}</td>
                  <td className="px-3 py-2.5 text-right tabular-nums text-rose-600 font-semibold">{fmtNumber(day.didNotEatCount)}</td>
                  <td className="px-5 sm:px-6 py-2.5 text-right tabular-nums font-bold text-slate-900">{fmtMoney(day.amount, stats.currency)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="border-t border-slate-200">
                <td className="px-5 sm:px-6 py-3 text-xs font-semibold uppercase tracking-wider text-slate-500">Total</td>
                <td className="px-3 py-3 text-right tabular-nums font-bold text-slate-900">{fmtNumber(d.windowMeals)}</td>
                <td className="px-3 py-3 text-right tabular-nums font-bold text-slate-900">
                  {fmtNumber(d.days.reduce((s, x) => s + (x.didNotEatCount || 0), 0))}
                </td>
                <td className="px-5 sm:px-6 py-3 text-right tabular-nums font-extrabold text-indigo-700">{fmtMoney(d.windowCost, stats.currency)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </Panel>
      <div className="flex flex-col gap-5">
        <Panel title="Rates" icon={Coins}>
          <dl className="space-y-3 text-sm">
            <div className="flex items-center justify-between">
              <dt className="text-slate-500">Standard meal rate</dt>
              <dd className="font-bold text-slate-900 tabular-nums">{stats.standardMealPrice ? fmtMoney(stats.standardMealPrice, stats.currency) : '—'}</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-slate-500">Average per meal ({d.dayShort})</dt>
              <dd className="font-bold text-slate-900 tabular-nums">{fmtMoney(stats.averageMealCostToday, stats.currency)}</dd>
            </div>
            <div className="flex items-center justify-between">
              <dt className="text-slate-500">Average per day ({d.days.length} days)</dt>
              <dd className="font-bold text-slate-900 tabular-nums">{fmtMoney(d.avgDailyCost, stats.currency)}</dd>
            </div>
          </dl>
        </Panel>
        <Panel title="Quick actions" icon={Sparkles}>
          <div className="space-y-2.5">
            <QuickAction to="/reports" icon={FileSpreadsheet} title="Reports" text="From / To date · PDF & Excel" tone="indigo" />
            <QuickAction to="/expenses" icon={Receipt} title="Expenses" text="Spending analysis" tone="amber" />
            <QuickAction to="/employees" icon={Users} title="Employees" text="Daily attendance" tone="sky" />
          </div>
        </Panel>
      </div>
    </div>
  </>
);

/** MANAGING DIRECTOR: executive overview and insights. */
const DirectorDashboard: React.FC<LayoutProps> = ({ stats, d, name }) => {
  const busiest = d.departments.length ? [...d.departments].sort((a, b) => b.ateCount - a.ateCount)[0] : undefined;
  const insights = [
    {
      icon: Gauge,
      tone: 'text-emerald-300',
      text: (
        <>
          <b className="text-white">{d.participation}%</b> of active staff ate on {d.dayLabel} ({fmtNumber(d.ate)} of {fmtNumber(d.staff)}).
        </>
      ),
    },
    {
      icon: Wallet,
      tone: 'text-amber-300',
      text: (
        <>
          <b className="text-white">{fmtMoney(stats.thisMonthTotalCost, stats.currency)}</b> spent on meals this month.
        </>
      ),
    },
    busiest
      ? {
          icon: Building2,
          tone: 'text-indigo-200',
          text: (
            <>
              <b className="text-white">{busiest.department}</b> served the most meals on {d.dayShort} ({fmtNumber(busiest.ateCount)}).
            </>
          ),
        }
      : null,
  ].filter(Boolean) as { icon: React.ComponentType<{ className?: string }>; tone: string; text: React.ReactNode }[];

  return (
    <>
      <Hero
        accent="ink"
        eyebrow="Managing Director · Executive overview"
        title={`${greeting()}, ${name}`}
        subtitle={
          <ul className="mt-1 space-y-2">
            {insights.map((x, i) => (
              <li key={i} className="flex items-start gap-2.5">
                <x.icon className={`mt-0.5 h-4 w-4 shrink-0 ${x.tone}`} />
                <span>{x.text}</span>
              </li>
            ))}
          </ul>
        }
        actions={
          <>
            <HeroButton to="/reports" primary>
              <FileSpreadsheet className="h-4 w-4" /> Reports
            </HeroButton>
            <HeroButton to="/audit-logs">
              <Activity className="h-4 w-4" /> Activity logs
            </HeroButton>
          </>
        }
        stats={
          <>
            <HeroStat label="Meals served" value={fmtNumber(stats.totalMealsCount ?? 0)} sub="all time" />
            <HeroStat label="Meal spend" value={fmtCompact(stats.totalExpenseCost ?? 0)} sub={`${stats.currency} all time`} />
            <HeroStat label="Active staff" value={fmtNumber(d.staff)} />
          </>
        }
      />

      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard
          label="Participation"
          value={`${d.participation}%`}
          icon={Gauge}
          tone="emerald"
          hint={d.dayLabel}
          footer={
            <StackBar
              total={Math.max(d.staff, d.ate + d.dne)}
              parts={[
                { value: d.ate, className: 'bg-emerald-500', label: 'Ate' },
                { value: d.dne, className: 'bg-rose-400', label: 'Did not eat' },
                { value: d.notRecorded, className: 'bg-slate-200', label: 'Not recorded' },
              ]}
            />
          }
        />
        <KpiCard
          label="This month"
          value={fmtCompact(stats.thisMonthTotalCost || 0)}
          unit={stats.currency}
          icon={Wallet}
          tone="amber"
          hint={`This week ${fmtMoney(stats.thisWeekTotalCost, stats.currency)}`}
        />
        <KpiCard
          label={`Meals · ${d.dayShort}`}
          value={fmtNumber(d.ate)}
          icon={UtensilsCrossed}
          tone="indigo"
          spark={d.ateSpark}
          sparkId="md-meals"
          hint={<Delta current={d.ate} previous={d.prev?.ateCount} />}
        />
        <KpiCard
          label="Cost per meal"
          value={fmtNumber(stats.averageMealCostToday)}
          unit={stats.currency}
          icon={Coins}
          tone="violet"
          hint={`Standard rate ${stats.standardMealPrice ? fmtMoney(stats.standardMealPrice, stats.currency) : '—'}`}
        />
      </div>

      <Panel
        title="Meals served and cost"
        subtitle={`Last ${d.days.length} days · bars = meals, line = cost (${stats.currency})`}
        icon={BarChart3}
        action={
          <div className="flex flex-wrap gap-x-4 gap-y-1">
            <LegendDot color={CHART.ate} label="Meals" value={fmtNumber(d.windowMeals)} />
            <LegendDot color={CHART.cost} label="Cost" value={fmtMoney(d.windowCost, stats.currency)} />
          </div>
        }
      >
        <MealsAndCostChart d={d} currency={stats.currency} />
      </Panel>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <Panel title="Department performance" subtitle={`Ranked by participation · ${d.dayLabel}`} icon={Building2} action={<AttendanceLegend />}>
          <DepartmentBreakdown d={d} currency={stats.currency} rankBy="participation" />
        </Panel>
        <Panel title="Recent activity" subtitle="Latest audit trail entries" icon={Activity} action={<ViewAll to="/audit-logs" />}>
          <ActivityTimeline logs={stats.recentActivities} />
        </Panel>
      </div>
    </>
  );
};

const ViewAll: React.FC<{ to: string }> = ({ to }) => (
  <Link to={to} className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-800">
    View all <ArrowRight className="h-3.5 w-3.5" />
  </Link>
);

/* ---------------------------------------------------------------------
   Page
   --------------------------------------------------------------------- */
const DashboardSkeleton: React.FC = () => (
  <div className="space-y-5 sm:space-y-6" aria-busy="true" aria-label="Loading dashboard">
    <Skeleton className="h-[188px] w-full rounded-3xl" />
    <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-[148px] rounded-2xl" />
      ))}
    </div>
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
      <Skeleton className="h-[360px] rounded-2xl xl:col-span-2" />
      <Skeleton className="h-[360px] rounded-2xl" />
    </div>
  </div>
);

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const fetchDashboardStats = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await dashboardApi.getStatistics();
      setStats(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load dashboard statistics.');
    } finally {
      setLoading(false);
    }
  };

  const d = useDerived(stats);

  if (loading) return <DashboardSkeleton />;

  if (error || !stats || !d) {
    return (
      <div className="mx-auto max-w-md rounded-3xl bg-white ring-1 ring-rose-200/70 shadow-card p-8 text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-600">
          <AlertTriangle className="h-6 w-6" />
        </div>
        <h3 className="font-display text-lg font-bold text-slate-900">Dashboard unavailable</h3>
        <p className="mt-1 text-sm text-slate-500">{error || 'Unable to display dashboard statistics.'}</p>
        <button
          onClick={fetchDashboardStats}
          className="mt-5 inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 h-10 text-sm font-semibold text-white shadow-glow hover:bg-indigo-700 transition-colors"
        >
          <RefreshCw className="h-4 w-4" /> Retry loading
        </button>
      </div>
    );
  }

  const name = user?.firstName || user?.fullName || user?.username || '';
  const props = { stats, d, name };

  return (
    <div className="space-y-5 sm:space-y-6">
      {user?.role === 'HR' ? (
        <HrDashboard {...props} />
      ) : user?.role === 'ACCOUNTANT' ? (
        <AccountantDashboard {...props} />
      ) : user?.role === 'MANAGING_DIRECTOR' ? (
        <DirectorDashboard {...props} />
      ) : (
        <AdminDashboard {...props} />
      )}
    </div>
  );
};

export default Dashboard;
