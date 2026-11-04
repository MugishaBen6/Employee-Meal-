import React, { useEffect, useState, useCallback } from 'react';
import { dashboardApi } from '../api/dashboardApi';
import { mealApi } from '../api/mealApi';
import { employeeApi } from '../api/employeeApi';
import { Employee, MealRecord, MealStatus, QuickMealCheckResponse, UserDashboardStats } from '../types';
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
  Plus,
  Search,
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

  // "Add Meal Record" form. The amount is never typed: the backend sets ATE to the
  // configured standard price and DID_NOT_EAT to 0, and records the logged-in user as owner.
  const [addOpen, setAddOpen] = useState(false);
  const [addDate, setAddDate] = useState<string>(getKigaliToday());
  const [addStatus, setAddStatus] = useState<MealStatus>('ATE');
  const [employeeQuery, setEmployeeQuery] = useState('');
  const [suggestions, setSuggestions] = useState<Employee[]>([]);
  const [searching, setSearching] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);
  const [duplicateCheck, setDuplicateCheck] = useState<QuickMealCheckResponse | null>(null);
  const [checking, setChecking] = useState(false);
  const [savingAdd, setSavingAdd] = useState(false);

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

  // ---------- Add Meal Record ----------

  const resetAddForm = (keepDateAndStatus: boolean) => {
    setEmployeeQuery('');
    setSuggestions([]);
    setSelectedEmployee(null);
    setDuplicateCheck(null);
    if (!keepDateAndStatus) {
      setAddDate(getKigaliToday());
      setAddStatus('ATE');
    }
  };

  const openAdd = () => {
    resetAddForm(false);
    setAddOpen(true);
  };

  // Employee search (active employees only), debounced.
  useEffect(() => {
    if (!addOpen || selectedEmployee) return;
    const q = employeeQuery.trim();
    if (q.length < 2) {
      setSuggestions([]);
      return;
    }
    const timer = setTimeout(async () => {
      setSearching(true);
      try {
        const list = await employeeApi.quickSearch(q);
        setSuggestions((list ?? []).filter((e) => e.status === 'ACTIVE'));
      } catch {
        setSuggestions([]);
      } finally {
        setSearching(false);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [employeeQuery, addOpen, selectedEmployee]);

  // Duplicate check for the selected employee and date (one record per employee per day).
  useEffect(() => {
    if (!addOpen || !selectedEmployee || !addDate) {
      setDuplicateCheck(null);
      return;
    }
    let cancelled = false;
    setChecking(true);
    mealApi
      .quickCheck(selectedEmployee.id.toString(), addDate)
      .then((res) => { if (!cancelled) setDuplicateCheck(res); })
      .catch(() => { if (!cancelled) setDuplicateCheck(null); })
      .finally(() => { if (!cancelled) setChecking(false); });
    return () => { cancelled = true; };
  }, [addOpen, selectedEmployee, addDate]);

  const addPrice =
    duplicateCheck && Number(duplicateCheck.defaultMealPrice) > 0
      ? Number(duplicateCheck.defaultMealPrice)
      : standardPrice;
  const alreadyRecorded = !!duplicateCheck?.alreadyRecordedToday;
  // For USER accounts the backend only returns the existing record if this user created it.
  const ownExistingRecord = duplicateCheck?.todayRecord ?? null;

  const handleAddRecord = async (keepOpen: boolean) => {
    if (!selectedEmployee) {
      addToast('error', 'Select an employee first.');
      return;
    }
    if (!addDate || addDate > getKigaliToday()) {
      addToast('error', 'Select a meal date that is today or earlier.');
      return;
    }
    if (alreadyRecorded) {
      addToast('error', 'This employee already has a meal record for this date.');
      return;
    }
    setSavingAdd(true);
    try {
      const res = await mealApi.recordMeal({
        employeeId: selectedEmployee.id,
        mealDate: addDate,
        mealStatus: addStatus,
      });
      const saved = res.data;
      const amountText = `${Number(saved?.amount ?? 0).toLocaleString()} ${stats?.currency ?? 'RWF'}`;
      addToast(
        'success',
        `${selectedEmployee.fullName}: ${addStatus === 'ATE' ? 'ATE' : 'DID NOT EAT'} recorded for ${addDate} (${amountText}).`
      );
      fetchStats();
      if (addDate === selectedDate) {
        fetchRecords(selectedDate);
      } else {
        setSelectedDate(addDate); // shows the day that was just recorded
      }
      if (keepOpen) {
        resetAddForm(true);
      } else {
        setAddOpen(false);
      }
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to record the meal.');
    } finally {
      setSavingAdd(false);
    }
  };

  // ---------- Edit own record ----------

  const openEdit = (record: MealRecord) => {
    setEditingRecord(record);
    setEditStatus(record.mealStatus);
    setEditAmount(record.mealStatus === 'DID_NOT_EAT' ? 0 : Number(record.amount ?? 0));
  };

  // The amount follows the pricing rules (the backend enforces the same for USER accounts):
  // DID_NOT_EAT is 0; an ATE record keeps its stored amount; changing to ATE uses the standard price.
  const handleStatusChange = (status: MealStatus) => {
    setEditStatus(status);
    if (status === 'DID_NOT_EAT') {
      setEditAmount(0);
    } else if (editingRecord?.mealStatus === 'ATE' && Number(editingRecord.amount) > 0) {
      setEditAmount(Number(editingRecord.amount));
    } else {
      setEditAmount(standardPrice ?? 0);
    }
  };

  const handleSaveEdit = async () => {
    if (!editingRecord) return;
    if (editStatus === 'ATE' && (!editAmount || editAmount <= 0)) {
      addToast('error', 'The standard meal price could not be loaded. Refresh and try again.');
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
          <Button size="sm" onClick={openAdd} className="flex items-center gap-1.5">
            <Plus className="w-4 h-4" />
            Add Meal Record
          </Button>
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
            <Button size="sm" variant="secondary" onClick={openAdd} className="mt-3 flex items-center gap-1.5">
              <Plus className="w-4 h-4" />
              Add Meal Record
            </Button>
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
              type="text"
              readOnly
              disabled
              value={`${(editStatus === 'DID_NOT_EAT' ? 0 : editAmount).toLocaleString()} ${stats?.currency ?? 'RWF'}`}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm disabled:bg-slate-100"
            />
            <p className="text-[11px] text-slate-400 mt-1">Calculated automatically: standard meal price for ATE, 0 for DID NOT EAT.</p>
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

      <Modal isOpen={addOpen} onClose={() => setAddOpen(false)} title="Add Meal Record">
        <div className="space-y-4">
          <div>
            <label htmlFor="add-meal-date" className="block text-xs font-semibold text-slate-700 mb-1">Meal Date</label>
            <input
              id="add-meal-date"
              type="date"
              value={addDate}
              max={getKigaliToday()}
              onChange={(e) => setAddDate(e.target.value)}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm"
            />
          </div>

          <div>
            <label htmlFor="add-employee" className="block text-xs font-semibold text-slate-700 mb-1">Employee</label>
            {selectedEmployee ? (
              <div className="flex items-center justify-between gap-2 border border-indigo-200 bg-indigo-50/60 rounded-xl px-3 py-2">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-slate-900 truncate">{selectedEmployee.fullName}</p>
                  <p className="text-xs text-slate-500 truncate">
                    {selectedEmployee.employeeCode} • {selectedEmployee.position || 'Worker'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => resetAddForm(true)}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 shrink-0"
                >
                  Change
                </button>
              </div>
            ) : (
              <div className="relative">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  id="add-employee"
                  type="text"
                  autoComplete="off"
                  placeholder="Search by name, code or phone"
                  value={employeeQuery}
                  onChange={(e) => setEmployeeQuery(e.target.value)}
                  className="w-full border border-slate-300 rounded-xl pl-9 pr-3 py-2 text-sm"
                />
                {(searching || suggestions.length > 0 || employeeQuery.trim().length >= 2) && (
                  <div className="mt-1 max-h-56 overflow-y-auto border border-slate-200 rounded-xl divide-y divide-slate-100 bg-white">
                    {searching ? (
                      <p className="px-3 py-2 text-xs text-slate-500">Searching…</p>
                    ) : suggestions.length === 0 ? (
                      <p className="px-3 py-2 text-xs text-slate-500">No active employee found.</p>
                    ) : (
                      suggestions.map((emp) => (
                        <button
                          key={emp.id}
                          type="button"
                          onClick={() => { setSelectedEmployee(emp); setSuggestions([]); }}
                          className="w-full text-left px-3 py-2 hover:bg-slate-50"
                        >
                          <p className="text-sm font-medium text-slate-900">{emp.fullName}</p>
                          <p className="text-xs text-slate-500">{emp.employeeCode} • {emp.position || 'Worker'}</p>
                        </button>
                      ))
                    )}
                  </div>
                )}
              </div>
            )}
          </div>

          <div>
            <span className="block text-xs font-semibold text-slate-700 mb-1">Meal Status</span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setAddStatus('ATE')}
                className={`flex items-center justify-center gap-1.5 rounded-xl border px-3 py-2 text-sm font-semibold ${
                  addStatus === 'ATE'
                    ? 'bg-emerald-600 border-emerald-600 text-white'
                    : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-50'
                }`}
              >
                <CheckCircle2 className="w-4 h-4" />
                ATE
              </button>
              <button
                type="button"
                onClick={() => setAddStatus('DID_NOT_EAT')}
                className={`flex items-center justify-center gap-1.5 rounded-xl border px-3 py-2 text-sm font-semibold ${
                  addStatus === 'DID_NOT_EAT'
                    ? 'bg-rose-600 border-rose-600 text-white'
                    : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-50'
                }`}
              >
                <AlertCircle className="w-4 h-4" />
                DID NOT EAT
              </button>
            </div>
          </div>

          <div>
            <span className="block text-xs font-semibold text-slate-700 mb-1">Amount Used</span>
            <p className="w-full border border-slate-200 bg-slate-100 rounded-xl px-3 py-2 text-sm text-slate-700">
              {addStatus === 'DID_NOT_EAT'
                ? `0 ${stats?.currency ?? 'RWF'}`
                : addPrice
                ? `${addPrice.toLocaleString()} ${stats?.currency ?? 'RWF'}`
                : 'Standard meal price'}
            </p>
            <p className="text-[11px] text-slate-400 mt-1">Calculated automatically: standard meal price for ATE, 0 for DID NOT EAT.</p>
          </div>

          {selectedEmployee && checking && (
            <p className="text-xs text-slate-500">Checking existing records…</p>
          )}
          {selectedEmployee && !checking && alreadyRecorded && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
              {ownExistingRecord ? (
                <>
                  You already recorded {selectedEmployee.fullName} for {addDate} (
                  {ownExistingRecord.mealStatus === 'ATE' ? 'ATE' : 'DID NOT EAT'}).{' '}
                  <button
                    type="button"
                    className="font-semibold underline"
                    onClick={() => { setAddOpen(false); openEdit(ownExistingRecord); }}
                  >
                    Edit that record
                  </button>
                </>
              ) : (
                <>A meal for {selectedEmployee.fullName} on {addDate} was already recorded by another account.</>
              )}
            </div>
          )}

          <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setAddOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="secondary"
              onClick={() => handleAddRecord(true)}
              isLoading={savingAdd}
              disabled={!selectedEmployee || checking || alreadyRecorded}
            >
              Save &amp; Add Another
            </Button>
            <Button
              onClick={() => handleAddRecord(false)}
              isLoading={savingAdd}
              disabled={!selectedEmployee || checking || alreadyRecorded}
            >
              Save Record
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default UserDashboard;
