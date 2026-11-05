import React, { useEffect, useMemo, useState } from 'react';
import { expenseApi } from '../api/expenseApi';
import { Skeleton } from '../components/common/Skeleton';
import {
  Receipt,
  CalendarDays,
  CalendarRange,
  Wallet,
  Coins,
  TrendingUp,
  ClipboardList,
  RefreshCw,
  AlertTriangle,
  FileSpreadsheet,
} from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Link } from 'react-router-dom';
import {
  PageHero,
  HeroStat,
  heroBtn,
  KpiCard,
  Panel,
  ChartTooltip,
  CHART,
  fmtMoney,
  fmtNumber,
  fmtCompact,
  fmtDayLabel,
} from '../components/dashboard/DashboardKit';

export const Expenses: React.FC = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchExpenses();
  }, []);

  const fetchExpenses = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await expenseApi.getSummary();
      setData(res);
    } catch (e: any) {
      console.error(e);
      setError(e?.response?.data?.message || 'Failed to load expense summary');
    } finally {
      setLoading(false);
    }
  };

  // Derived from the real 30-day series returned by the API.
  const series: { date: string; formattedDate: string; amount: number; ateCount?: number; didNotEatCount?: number }[] =
    data?.chartData ?? [];
  const derived = useMemo(() => {
    const amounts = series.map((d) => Number(d.amount) || 0);
    const total = amounts.reduce((s, x) => s + x, 0);
    const activeDays = amounts.filter((x) => x > 0).length;
    const peak = series.reduce<(typeof series)[number] | undefined>((m, d) => (!m || d.amount > m.amount ? d : m), undefined);
    return {
      amounts,
      total,
      activeDays,
      avgPerActiveDay: activeDays ? Math.round(total / activeDays) : 0,
      peak,
      meals: series.reduce((s, d) => s + (d.ateCount || 0), 0),
    };
  }, [data]);

  if (loading) {
    return (
      <div className="space-y-5 sm:space-y-6">
        <Skeleton className="h-[176px] rounded-3xl" />
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-[140px] rounded-2xl" />
          ))}
        </div>
        <Skeleton className="h-[360px] rounded-2xl" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="space-y-6">
        <PageHero icon={Receipt} eyebrow="Finance" title="Expense Management" subtitle="Financial audit of employee meal costs and company expenditures" />
        <div className="mx-auto max-w-md rounded-3xl bg-white ring-1 ring-rose-200/70 shadow-card p-8 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-600">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <p className="text-rose-700 font-medium">{error || 'Failed to load expense data.'}</p>
          <button
            onClick={fetchExpenses}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 h-10 text-sm font-semibold text-white shadow-glow hover:bg-indigo-700 transition-colors"
          >
            <RefreshCw className="h-4 w-4" /> Retry Loading Expenses
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5 sm:space-y-6">
      <PageHero
        icon={Receipt}
        eyebrow="Finance · Meal expenditure"
        title="Expense Management"
        subtitle="Financial audit of employee meal costs and company expenditures, from the amounts stored on each meal record."
        actions={
          <Link to="/reports" className={heroBtn.primary}>
            <FileSpreadsheet className="h-4 w-4" /> Reports & exports
          </Link>
        }
        stats={
          <>
            <HeroStat label="30-day total" value={fmtCompact(derived.total)} sub="RWF" />
            <HeroStat label="Meals (30 days)" value={fmtNumber(derived.meals)} />
            <HeroStat label="Avg / active day" value={fmtCompact(derived.avgPerActiveDay)} sub="RWF" />
            <HeroStat
              label="Peak day"
              value={derived.peak && derived.peak.amount > 0 ? fmtCompact(derived.peak.amount) : '—'}
              sub={derived.peak && derived.peak.amount > 0 ? fmtDayLabel(derived.peak.date) : 'no spending'}
            />
          </>
        }
      />

      {/* Expense Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard label="Today's Expense" value={fmtNumber(data.todayExpense || 0)} unit="RWF" icon={CalendarDays} tone="sky" hint="Meals recorded for today" />
        <KpiCard label="Weekly Expense" value={fmtNumber(data.weeklyExpense || 0)} unit="RWF" icon={CalendarRange} tone="indigo" hint="Current week" />
        <KpiCard label="Monthly Expense" value={fmtNumber(data.monthlyExpense || 0)} unit="RWF" icon={Wallet} tone="amber" hint="Current month" />
        <KpiCard
          label="All-Time Total"
          value={fmtNumber(data.totalExpense || 0)}
          unit="RWF"
          icon={Coins}
          tone="emerald"
          spark={derived.amounts}
          sparkId="exp-total"
          hint="Last 30 days trend"
        />
      </div>

      {/* Chart */}
      <Panel
        title="Expense Trend (Last 30 Days)"
        subtitle="Daily company expenditure calculation"
        icon={TrendingUp}
        action={
          <span className="text-xs text-slate-500">
            Total <b className="text-slate-900 tabular-nums">{fmtMoney(derived.total)}</b>
          </span>
        }
      >
        <div className="h-[300px] sm:h-[340px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={series} margin={{ top: 8, right: 6, left: -6, bottom: 0 }}>
              <defs>
                <linearGradient id="colorExp" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={CHART.cost} stopOpacity={0.3} />
                  <stop offset="100%" stopColor={CHART.cost} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid vertical={false} stroke={CHART.grid} />
              <XAxis dataKey="formattedDate" tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} dy={6} minTickGap={16} />
              <YAxis tickFormatter={fmtCompact} tickLine={false} axisLine={false} tick={{ fill: CHART.axis, fontSize: 11 }} width={44} />
              <Tooltip
                cursor={{ stroke: CHART.cost, strokeOpacity: 0.25, strokeWidth: 1.5, strokeDasharray: '4 4' }}
                content={<ChartTooltip currency="RWF" labelMap={{ amount: 'Amount' }} />}
              />
              <Area
                type="monotone"
                dataKey="amount"
                name="Amount"
                stroke={CHART.cost}
                strokeWidth={2.5}
                fill="url(#colorExp)"
                activeDot={{ r: 5, strokeWidth: 2, stroke: '#fff', fill: CHART.cost }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </Panel>

      {/* Daily ledger (same 30-day series) */}
      <Panel title="Daily breakdown" subtitle="Last 30 days, most recent first" icon={ClipboardList} bodyClassName="px-0 sm:px-0 pb-2 sm:pb-2">
        <div className="max-h-[420px] overflow-y-auto touch-scroll">
          <table className="w-full text-sm">
            <thead className="sticky top-0 z-10 bg-slate-50/95 backdrop-blur">
              <tr className="border-y border-slate-100 text-left">
                <th className="px-5 sm:px-6 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Date</th>
                <th className="px-3 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500 hidden sm:table-cell">Ate</th>
                <th className="px-3 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500 hidden sm:table-cell">Did not eat</th>
                <th className="px-5 sm:px-6 py-2.5 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {[...series].reverse().map((d) => {
                const share = derived.peak && derived.peak.amount > 0 ? (d.amount / derived.peak.amount) * 100 : 0;
                return (
                  <tr key={d.date} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-5 sm:px-6 py-2.5">
                      <p className="font-medium text-slate-700">{fmtDayLabel(d.date, true)}</p>
                      <div className="mt-1 h-1 w-full max-w-[160px] rounded-full bg-slate-100 overflow-hidden">
                        <div className="h-full rounded-full bg-indigo-400" style={{ width: `${share}%` }} />
                      </div>
                    </td>
                    <td className="px-3 py-2.5 text-right tabular-nums font-semibold text-emerald-700 hidden sm:table-cell">
                      {d.ateCount !== undefined ? fmtNumber(d.ateCount) : '—'}
                    </td>
                    <td className="px-3 py-2.5 text-right tabular-nums font-semibold text-rose-600 hidden sm:table-cell">
                      {d.didNotEatCount !== undefined ? fmtNumber(d.didNotEatCount) : '—'}
                    </td>
                    <td className={`px-5 sm:px-6 py-2.5 text-right tabular-nums font-bold ${d.amount > 0 ? 'text-slate-900' : 'text-slate-400'}`}>
                      {fmtMoney(d.amount, 'RWF')}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
};

export default Expenses;
