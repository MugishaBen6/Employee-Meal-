import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { dashboardApi } from '../api/dashboardApi';
import { DashboardStats } from '../types';
import { Card } from '../components/common/Card';
import { Skeleton } from '../components/common/Skeleton';
import {
  Users,
  CheckCircle2,
  XCircle,
  DollarSign,
  Calendar,
  TrendingUp,
  Activity,
  ArrowUpRight,
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
  Legend,
} from 'recharts';

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

  const formatCurrency = (amount: number, curr = 'RWF') => {
    return `${amount.toLocaleString()} ${curr}`;
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-28 w-full rounded-2xl" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Skeleton className="h-32 rounded-xl" />
          <Skeleton className="h-32 rounded-xl" />
          <Skeleton className="h-32 rounded-xl" />
          <Skeleton className="h-32 rounded-xl" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-80 rounded-xl" />
          <Skeleton className="h-80 rounded-xl" />
        </div>
      </div>
    );
  }

  if (error || !stats) {
    return (
      <div className="p-8 text-center bg-white rounded-2xl border border-rose-200 text-rose-600">
        <p className="font-semibold">{error || 'Unable to display dashboard statistics.'}</p>
        <button onClick={fetchDashboardStats} className="mt-4 px-4 py-2 bg-rose-600 text-white rounded-lg text-sm">
          Retry Loading
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-5 sm:space-y-6 animate-fadeIn">
      {/* Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-4 sm:p-6 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-3 sm:gap-4">
        <div>
          <span className="px-2.5 py-0.5 sm:px-3 sm:py-1 bg-indigo-500/20 text-indigo-300 rounded-full text-[10px] sm:text-xs font-semibold tracking-wider uppercase border border-indigo-500/30 mb-2 inline-block">
            {user?.role} Overview
          </span>
          <h2 className="text-xl sm:text-2xl font-bold tracking-tight">Welcome back, {user?.fullName || user?.username}!</h2>
          <p className="text-xs sm:text-sm text-slate-300 mt-1">Here is today's meal status and expenditure analytics for Kigali Factory.</p>
        </div>
        <div className="bg-white/10 backdrop-blur-md px-3 py-2 sm:px-4 sm:py-2.5 rounded-xl border border-white/10 text-xs font-mono flex items-center gap-2 self-start md:self-auto shrink-0">
          <Calendar className="w-4 h-4 text-indigo-400" />
          <span>{new Date().toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' })}</span>
        </div>
      </div>

      {/* Top Cards Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3 sm:gap-4">
        {/* Total Active Employees */}
        <Card className="p-3.5 sm:p-4 border-l-4 border-l-sky-500">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider truncate">Total Staff</p>
              <h3 className="text-lg sm:text-2xl font-bold text-slate-900 mt-0.5 sm:mt-1">{stats.totalEmployees}</h3>
            </div>
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center shrink-0">
              <Users className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
          </div>
        </Card>

        {/* Ate Today */}
        <Card className="p-3.5 sm:p-4 border-l-4 border-l-emerald-500">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider truncate">Ate Today</p>
              <h3 className="text-lg sm:text-2xl font-bold text-emerald-600 mt-0.5 sm:mt-1">{stats.ateToday}</h3>
            </div>
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
              <CheckCircle2 className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
          </div>
        </Card>

        {/* Did Not Eat */}
        <Card className="p-3.5 sm:p-4 border-l-4 border-l-rose-500">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider truncate">Did Not Eat</p>
              <h3 className="text-lg sm:text-2xl font-bold text-rose-600 mt-0.5 sm:mt-1">{stats.didNotEatToday}</h3>
            </div>
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
              <XCircle className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
          </div>
        </Card>

        {/* Today's Cost */}
        <Card className="p-3.5 sm:p-4 border-l-4 border-l-indigo-500">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider truncate">Today's Cost</p>
              <h3 className="text-sm sm:text-lg font-bold text-indigo-700 mt-0.5 sm:mt-1 truncate">{formatCurrency(stats.todayTotalCost, stats.currency)}</h3>
            </div>
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
              <DollarSign className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
          </div>
        </Card>

        {/* This Week */}
        <Card className="p-3.5 sm:p-4 border-l-4 border-l-amber-500">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider truncate">This Week</p>
              <h3 className="text-sm sm:text-lg font-bold text-amber-700 mt-0.5 sm:mt-1 truncate">{formatCurrency(stats.thisWeekTotalCost, stats.currency)}</h3>
            </div>
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
              <TrendingUp className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
          </div>
        </Card>

        {/* This Month */}
        <Card className="p-3.5 sm:p-4 border-l-4 border-l-purple-500">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase tracking-wider truncate">This Month</p>
              <h3 className="text-sm sm:text-lg font-bold text-purple-700 mt-0.5 sm:mt-1 truncate">{formatCurrency(stats.thisMonthTotalCost, stats.currency)}</h3>
            </div>
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0">
              <Calendar className="w-4 h-4 sm:w-5 sm:h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 sm:gap-6">
        {/* Attendance Bar Chart */}
        <Card title="Daily Attendance (ATE vs DID NOT EAT)" subtitle="Last 7 Days Meal Participation">
          <div className="h-64 sm:h-72 w-full mt-2 sm:mt-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={stats.dailyExpenditures} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="formattedDate" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderRadius: '12px', color: '#fff', border: 'none', fontSize: '12px' }}
                />
                <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
                <Bar dataKey="ateCount" name="Ate" fill="#10b981" radius={[4, 4, 0, 0]} />
                <Bar dataKey="didNotEatCount" name="Did Not Eat" fill="#f43f5e" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Expenditure Area Chart */}
        <Card title="Daily Expense Trend" subtitle={`Expenditure over last 7 days (${stats.currency})`}>
          <div className="h-64 sm:h-72 w-full mt-2 sm:mt-4">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={stats.dailyExpenditures} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="formattedDate" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} />
                <Tooltip
                  formatter={(val: any) => [`${val} ${stats.currency}`, 'Amount']}
                  contentStyle={{ backgroundColor: '#0f172a', borderRadius: '12px', color: '#fff', border: 'none', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="amount" stroke="#6366f1" strokeWidth={3} fillOpacity={1} fill="url(#colorAmount)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      {/* Bottom Grid: Department Breakdown & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 sm:gap-6">
        {/* Department Stats */}
        <Card title="Department Statistics (Today)" className="lg:col-span-2">
          <div className="overflow-x-auto touch-scroll mt-2">
            <table className="w-full text-left text-sm min-w-[480px]">
              <thead className="bg-slate-50 text-slate-500 uppercase text-[11px] tracking-wider font-semibold border-y border-slate-200">
                <tr>
                  <th className="py-3 px-3 sm:px-4">Department</th>
                  <th className="py-3 px-3 sm:px-4 text-center">Total Staff</th>
                  <th className="py-3 px-3 sm:px-4 text-center">Ate</th>
                  <th className="py-3 px-3 sm:px-4 text-center">Did Not Eat</th>
                  <th className="py-3 px-3 sm:px-4 text-right">Cost ({stats.currency})</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {stats.departmentStats.map((dept) => (
                  <tr key={dept.department} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-3 sm:px-4 font-semibold text-slate-800">{dept.department}</td>
                    <td className="py-3 px-3 sm:px-4 text-center font-medium">{dept.totalEmployees}</td>
                    <td className="py-3 px-3 sm:px-4 text-center font-semibold text-emerald-600">{dept.ateCount}</td>
                    <td className="py-3 px-3 sm:px-4 text-center font-semibold text-rose-500">{dept.didNotEatCount}</td>
                    <td className="py-3 px-3 sm:px-4 text-right font-mono font-bold text-slate-900">
                      {formatCurrency(dept.totalAmount, stats.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        {/* Recent Activities */}
        <Card title="Recent System Activities" subtitle="Latest Audit Trail Logs">
          <div className="space-y-3 sm:space-y-4 mt-2">
            {stats.recentActivities.length === 0 ? (
              <p className="text-xs text-slate-400 italic">No recent activity logs.</p>
            ) : (
              stats.recentActivities.map((log) => (
                <div key={log.id} className="flex items-start gap-3 p-2.5 rounded-xl hover:bg-slate-50 transition-colors border border-slate-100">
                  <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0 mt-0.5">
                    <Activity className="w-4 h-4" />
                  </div>
                  <div className="flex-1 min-w-0 text-xs">
                    <p className="font-semibold text-slate-800 truncate">{log.description}</p>
                    <div className="flex items-center justify-between text-[10px] text-slate-400 mt-1">
                      <span className="font-mono">{log.username} ({log.userRole})</span>
                      <span>{new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;

