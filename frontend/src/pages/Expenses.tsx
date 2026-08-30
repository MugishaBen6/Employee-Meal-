import React, { useEffect, useState } from 'react';
import { expenseApi } from '../api/expenseApi';
import { Card } from '../components/common/Card';
import { Skeleton } from '../components/common/Skeleton';
import { DollarSign, TrendingUp, Calendar, CreditCard } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export const Expenses: React.FC = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    fetchExpenses();
  }, []);

  const fetchExpenses = async () => {
    setLoading(true);
    try {
      const res = await expenseApi.getSummary();
      setData(res);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (val: number) => `${(val || 0).toLocaleString()} RWF`;

  if (loading || !data) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-28 rounded-2xl" />
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Expense Management</h2>
        <p className="text-sm text-slate-500 mt-1">Financial audit of employee meal costs and company expenditures</p>
      </div>

      {/* Expense Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 border-l-4 border-l-sky-500">
          <p className="text-xs font-medium text-slate-500 uppercase">Today's Expense</p>
          <h3 className="text-2xl font-bold text-slate-900 mt-1">{formatCurrency(data.todayExpense)}</h3>
        </Card>
        <Card className="p-4 border-l-4 border-l-indigo-500">
          <p className="text-xs font-medium text-slate-500 uppercase">Weekly Expense</p>
          <h3 className="text-2xl font-bold text-indigo-700 mt-1">{formatCurrency(data.weeklyExpense)}</h3>
        </Card>
        <Card className="p-4 border-l-4 border-l-amber-500">
          <p className="text-xs font-medium text-slate-500 uppercase">Monthly Expense</p>
          <h3 className="text-2xl font-bold text-amber-700 mt-1">{formatCurrency(data.monthlyExpense)}</h3>
        </Card>
        <Card className="p-4 border-l-4 border-l-emerald-500">
          <p className="text-xs font-medium text-slate-500 uppercase">All-Time Total Expense</p>
          <h3 className="text-2xl font-bold text-emerald-700 mt-1">{formatCurrency(data.totalExpense)}</h3>
        </Card>
      </div>

      {/* Chart */}
      <Card title="Expense Trend (Last 30 Days)" subtitle="Daily company expenditure calculation">
        <div className="h-80 w-full mt-4">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data.chartData} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
              <defs>
                <linearGradient id="colorExp" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#0284c7" stopOpacity={0.4} />
                  <stop offset="95%" stopColor="#0284c7" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
              <XAxis dataKey="formattedDate" stroke="#64748b" fontSize={12} />
              <YAxis stroke="#64748b" fontSize={12} />
              <Tooltip formatter={(val: any) => [`${val} RWF`, 'Amount']} />
              <Area type="monotone" dataKey="amount" stroke="#0284c7" strokeWidth={3} fillOpacity={1} fill="url(#colorExp)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </div>
  );
};

export default Expenses;

