import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/dashboardApi';
import { mealApi } from '../api/mealApi';
import { MealRecord, MealStatus, UserDashboardStats } from '../types';
import { getKigaliToday } from '../utils/dateUtils';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { Skeleton } from '../components/common/Skeleton';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { ToastContainer, ToastMessage } from '../components/common/Toast';
import {
  LayoutDashboard,
  Utensils,
  CheckCircle2,
  AlertCircle,
  DollarSign,
  List,
  ArrowRight,
  RefreshCw,
  Edit3,
} from 'lucide-react';

const UserDashboard: React.FC = () => {
  const [stats, setStats] = useState<UserDashboardStats | null>(null);
  const [records, setRecords] = useState<MealRecord[]>([]);
  const [selectedDate, setSelectedDate] = useState<string>(getKigaliToday());
  const [loadingStats, setLoadingStats] = useState(true);
  const [loadingRecords, setLoadingRecords] = useState(true);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [editingRecord, setEditingRecord] = useState<MealRecord | null>(null);
  const [editStatus, setEditStatus] = useState<MealStatus>('ATE');
  const [editAmount, setEditAmount] = useState<number>(0);
  const [savingEdit, setSavingEdit] = useState(false);
  const [statsError, setStatsError] = useState(false);

  const addToast = (type: ToastMessage['type'], message: string) => {
    const id = Date.now().toString();
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 5000);
  };

  // The configured standard meal price comes from /dashboard/my-stats, because
  // USER accounts are not allowed to read /settings. It is never hardcoded:
  // while loading or after a failure it is undefined, and the save guard below
  // refuses an ATE edit without a positive amount instead of guessing.
  const standardPrice =
    stats?.standardMealPrice != null && Number(stats.standardMealPrice) > 0
      ? Number(stats.standardMealPrice)
      : undefined;

  const fetchStats = useCallback(async () => {
    setLoadingStats(true);
    try {
      const data = await dashboardApi.getMyStats();
      setStats(data);
      setStatsError(false);
    } catch {
      setStats(null);
      setStatsError(true);
      addToast('error', 'Failed to load dashboard statistics.');
    } finally {
      setLoadingStats(false);
    }
  }, []);

  const fetchRecords = useCallback(async (date: string) => {
    setLoadingRecords(true);
    try {
      const data = await mealApi.getMealRecords({
        startDate: date,
        endDate: date,
        size: 100,
      });
      setRecords(data?.content ?? []);
    } catch {
      addToast('error', 'Failed to load meal records for the selected date.');
      setRecords([]);
    } finally {
      setLoadingRecords(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    if (selectedDate) {
      fetchRecords(selectedDate);
    }
  }, [selectedDate, fetchRecords]);

  const openEdit = (record: MealRecord) => {
    setEditingRecord(record);
    setEditStatus(record.mealStatus);
    setEditAmount(record.mealStatus === 'DID_NOT_EAT' ? 0 : Number(record.amount ?? 0));
  };

  const handleStatusChange = (status: MealStatus) => {
    setEditStatus(status);
    if (status === 'DID_NOT_EAT') {
      setEditAmount(0);
    } else if (editAmount <= 0) {
      // Use the admin-configured standard price (loaded from Settings API).
      // If still loading (undefined) leave at 0 — the save guard below will
      // catch it and show an error rather than sending a wrong amount.
      if (standardPrice !== undefined && standardPrice > 0) {
        setEditAmount(standardPrice);
      }
      // else: leave editAmount at 0; handleSaveEdit will reject with a clear message
    }
  };

  const handleSaveEdit = async () => {
    if (!editingRecord) return;
    if (editStatus === 'ATE' && (!editAmount || editAmount <= 0)) {
      addToast('error', 'ATE records must have a valid positive amount.');
      return;
    }

    setSavingEdit(true);
    try {
      await mealApi.updateMealRecord(editingRecord.id, {
        mealStatus: editStatus,
        amount: editStatus === 'DID_NOT_EAT' ? 0 : Number(editAmount),
      });
      addToast('success', 'Meal record updated successfully.');
      setEditingRecord(null);
      fetchStats();
      fetchRecords(selectedDate);
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to update meal record.');
    } finally {
      setSavingEdit(false);
    }
  };

  const dayAteCount = records.filter((r) => r.mealStatus === 'ATE').length;
  const dayDidNotEatCount = records.filter((r) => r.mealStatus === 'DID_NOT_EAT').length;
  const dayTotal = records
    .filter((r) => r.mealStatus === 'ATE')
    .reduce((sum, r) => sum + (Number(r.amount) || 0), 0);

  const statCards = [
    {
      label: 'Total Records',
      value: stats ? stats.totalRecords : '—',
      icon: List,
      color: 'text-indigo-600',
      bg: 'bg-indigo-50',
    },
    {
      label: 'ATE',
      value: stats ? stats.ateCount : '—',
      icon: CheckCircle2,
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
    {
      label: 'DID NOT EAT',
      value: stats ? stats.didNotEatCount : '—',
      icon: AlertCircle,
      color: 'text-rose-600',
      bg: 'bg-rose-50',
    },
    {
      label: `Total Amount (${stats?.currency ?? 'RWF'})`,
      value: stats
        ? Number(stats.totalAmount).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 })
        : '—',
      icon: DollarSign,
      color: 'text-amber-600',
      bg: 'bg-amber-50',
    },
  ];

  return (
    <div className="flex flex-col gap-6 p-4 sm:p-6">
      <ToastContainer toasts={toasts} onClose={(id) => setToasts((prev) => prev.filter((t) => t.id !== id))} />

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/15 flex items-center justify-center">
            <LayoutDashboard className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900">My Dashboard</h1>
            <p className="text-sm text-slate-500">Your personal meal recording summary</p>
          </div>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => { fetchStats(); fetchRecords(selectedDate); }}
            className="flex items-center gap-1.5"
          >
            <RefreshCw className="w-4 h-4" />
            Refresh
          </Button>
          <Link to="/meals">
            <Button size="sm" className="flex items-center gap-1.5">
              <Utensils className="w-4 h-4" />
              Record Meal
              <ArrowRight className="w-4 h-4" />
            </Button>
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <Card key={card.label} className="p-4">
              {loadingStats ? (
                <div className="flex flex-col gap-2">
                  <Skeleton className="h-8 w-8 rounded-lg" />
                  <Skeleton className="h-6 w-16" />
                  <Skeleton className="h-4 w-24" />
                </div>
              ) : (
                <div className="flex flex-col gap-2" title={statsError ? 'Statistics could not be loaded' : undefined}>
                  <div className={`w-9 h-9 rounded-xl ${card.bg} flex items-center justify-center`}>
                    <Icon className={`w-5 h-5 ${card.color}`} />
                  </div>
                  <p className="text-2xl font-bold text-slate-900">{card.value}</p>
                  <p className="text-xs text-slate-500 font-medium">{card.label}</p>
                </div>
              )}
            </Card>
          );
        })}
      </div>

      <Card className="p-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
          <h2 className="text-base font-semibold text-slate-900 flex items-center gap-2">
            <List className="w-4 h-4 text-indigo-600" />
            My Recorded Meals
          </h2>
          <div className="flex items-center gap-2">
            <label htmlFor="record-date" className="text-sm text-slate-400 whitespace-nowrap">
              Filter by date:
            </label>
            <input
              id="record-date"
              type="date"
              value={selectedDate}
              max={getKigaliToday()}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="px-3 py-1.5 rounded-lg bg-white border border-slate-300 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>

        {!loadingRecords && records.length > 0 && (
          <p className="text-xs text-slate-400 mb-3">
            {selectedDate}: {records.length} record{records.length === 1 ? '' : 's'} · {dayAteCount} ATE · {dayDidNotEatCount} DID NOT EAT · {dayTotal.toLocaleString()} {stats?.currency ?? 'RWF'}
          </p>
        )}

        {loadingRecords ? (
          <div className="flex flex-col gap-3">
            {[...Array(4)].map((_, i) => (
              <Skeleton key={i} className="h-10 w-full rounded-lg" />
            ))}
          </div>
        ) : records.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-slate-500">
            <Utensils className="w-8 h-8 mb-2 opacity-40" />
            <p className="text-sm">No meal records found for this date.</p>
            <Link to="/meals" className="mt-3">
              <Button size="sm" variant="secondary" className="flex items-center gap-1.5">
                <Utensils className="w-4 h-4" />
                Record a Meal
              </Button>
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-slate-500 border-b border-slate-200">
                  <th className="pb-3 text-left font-medium">Names</th>
                  <th className="pb-3 text-left font-medium">Position</th>
                  <th className="pb-3 text-left font-medium">Meal Status</th>
                  <th className="pb-3 text-right font-medium">Amount Used</th>
                  <th className="pb-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {records.map((rec) => (
                  <tr key={rec.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-3 text-slate-900 font-medium">{rec.employeeName ?? '—'}</td>
                    <td className="py-3 text-slate-500">{rec.position ?? '—'}</td>
                    <td className="py-3">
                      <Badge variant={rec.mealStatus === 'ATE' ? 'success' : 'danger'}>
                        {rec.mealStatus === 'ATE' ? 'ATE' : 'DID NOT EAT'}
                      </Badge>
                    </td>
                    <td className="py-3 text-right text-slate-700">
                      {rec.mealStatus === 'ATE'
                        ? `${Number(rec.amount ?? 0).toLocaleString()} ${stats?.currency ?? 'RWF'}`
                        : `0 ${stats?.currency ?? 'RWF'}`}
                    </td>
                    <td className="py-3 text-right">
                      <button
                        onClick={() => openEdit(rec)}
                        className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-800 text-xs font-semibold"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal
        isOpen={!!editingRecord}
        onClose={() => setEditingRecord(null)}
        title={`Edit meal: ${editingRecord?.employeeName ?? ''}`}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Meal Status</label>
            <select
              value={editStatus}
              onChange={(e) => handleStatusChange(e.target.value as MealStatus)}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm"
            >
              <option value="ATE">ATE</option>
              <option value="DID_NOT_EAT">DID NOT EAT</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Amount Used</label>
            <input
              type="number"
              disabled={editStatus === 'DID_NOT_EAT'}
              value={editStatus === 'DID_NOT_EAT' ? 0 : editAmount}
              onChange={(e) => setEditAmount(Number(e.target.value))}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm disabled:bg-slate-100"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setEditingRecord(null)}>
              Cancel
            </Button>
            <Button onClick={handleSaveEdit} isLoading={savingEdit}>
              Save Changes
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default UserDashboard;
