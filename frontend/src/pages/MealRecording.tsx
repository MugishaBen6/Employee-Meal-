import React, { useEffect, useRef, useState } from 'react';
import { mealApi } from '../api/mealApi';
import { employeeApi } from '../api/employeeApi';
import { Employee, MealRecord, MealStatus, QuickMealCheckResponse } from '../types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { ToastContainer, ToastMessage } from '../components/common/Toast';
import {
  Utensils,
  Search,
  CheckCircle2,
  AlertCircle,
  Clock,
  UserCheck,
  Zap,
  RotateCcw,
} from 'lucide-react';

export const MealRecording: React.FC = () => {
  const searchInputRef = useRef<HTMLInputElement>(null);

  const [query, setQuery] = useState('');
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [mealStatus, setMealStatus] = useState<MealStatus>('ATE');
  const [amount, setAmount] = useState<number>(1500);

  const [quickCheck, setQuickCheck] = useState<QuickMealCheckResponse | null>(null);
  const [loadingCheck, setLoadingCheck] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [searchSuggestions, setSearchSuggestions] = useState<Employee[]>([]);

  const [todayRecords, setTodayRecords] = useState<MealRecord[]>([]);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (type: 'success' | 'error' | 'info', message: string) => {
    setToasts((prev) => [...prev, { id: Date.now().toString(), type, message }]);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  useEffect(() => {
    // Focus search input on mount
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
    fetchTodayRecords();
  }, [selectedDate]);

  useEffect(() => {
    if (query.trim().length >= 2) {
      const timer = setTimeout(async () => {
        try {
          const suggestions = await employeeApi.quickSearch(query.trim());
          setSearchSuggestions(suggestions);
        } catch (e) {
          console.error(e);
        }
      }, 200);
      return () => clearTimeout(timer);
    } else {
      setSearchSuggestions([]);
    }
  }, [query]);

  const fetchTodayRecords = async () => {
    try {
      const data = await mealApi.getMealRecords({
        startDate: selectedDate,
        endDate: selectedDate,
        size: 15,
      });
      setTodayRecords(data.content);
    } catch (e) {
      console.error(e);
    }
  };

  const handleSelectEmployee = async (empCodeOrId: string) => {
    setLoadingCheck(true);
    setSearchSuggestions([]);
    try {
      const result = await mealApi.quickCheck(empCodeOrId, selectedDate);
      setQuickCheck(result);
      setAmount(result.defaultMealPrice || 1500);
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Employee not found');
      setQuickCheck(null);
    } finally {
      setLoadingCheck(false);
    }
  };

  const handleRecordMeal = async () => {
    if (!quickCheck) return;

    if (quickCheck.alreadyRecordedToday) {
      addToast('error', `Meal already recorded for ${quickCheck.employee.fullName} today.`);
      return;
    }

    setSubmitting(true);
    try {
      const res = await mealApi.recordMeal({
        employeeId: quickCheck.employee.id,
        mealDate: selectedDate,
        mealStatus: mealStatus,
        amount: mealStatus === 'DID_NOT_EAT' ? 0 : amount,
      });

      addToast('success', res.message || `Meal recorded successfully for ${quickCheck.employee.fullName}`);

      // Reset state for FAST NEXT ENTRY
      setQuery('');
      setQuickCheck(null);
      setSearchSuggestions([]);
      fetchTodayRecords();

      // Auto focus search field for next employee
      if (searchInputRef.current) {
        searchInputRef.current.focus();
      }
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to record meal.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-indigo-900 via-slate-900 to-indigo-950 rounded-2xl p-4 sm:p-6 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-3 sm:gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1.5 sm:mb-2">
            <Zap className="w-4 h-4 sm:w-5 sm:h-5 text-amber-400 fill-amber-400" />
            <span className="text-[11px] sm:text-xs font-semibold text-amber-300 uppercase tracking-wider">Fast Queue Recording Mode</span>
          </div>
          <h2 className="text-xl sm:text-2xl font-bold tracking-tight">Record Employee Meal</h2>
          <p className="text-xs sm:text-sm text-slate-300 mt-1">
            Search employee by Code (e.g., EMP001) or Name to instantly record lunch attendance.
          </p>
        </div>

        {/* Date Selector */}
        <div className="bg-white/10 backdrop-blur-md px-3 py-2 sm:px-4 sm:py-2 rounded-xl border border-white/20 flex items-center gap-2.5 sm:gap-3 self-start md:self-auto shrink-0">
          <Clock className="w-4 h-4 text-indigo-300" />
          <span className="text-xs font-medium text-slate-200">Date:</span>
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="bg-transparent text-white text-xs font-mono font-semibold focus:outline-none cursor-pointer"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 sm:gap-6">
        {/* Left Column: Search & Quick Form (7 cols) */}
        <div className="lg:col-span-7 space-y-5 sm:space-y-6">
          <Card title="Quick Employee Lookup" subtitle="Type Employee Code or Name (e.g. EMP001, John Doe)">
            <div className="relative">
              <Search className="w-5 h-5 text-indigo-500 absolute left-3.5 sm:left-4 top-1/2 -translate-y-1/2" />
              <input
                ref={searchInputRef}
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && query.trim()) {
                    handleSelectEmployee(query.trim());
                  }
                }}
                placeholder="Enter Code (EMP001) or Name..."
                className="w-full pl-10 sm:pl-12 pr-20 sm:pr-24 py-3 sm:py-3.5 rounded-xl border-2 border-indigo-100 focus:border-indigo-600 focus:ring-4 focus:ring-indigo-500/10 text-sm sm:text-base font-semibold bg-white shadow-xs transition-all"
              />
              <button
                onClick={() => query.trim() && handleSelectEmployee(query.trim())}
                className="absolute right-1.5 sm:right-2 top-1/2 -translate-y-1/2 px-3 sm:px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white rounded-lg text-xs font-semibold shadow-xs transition-colors min-h-[34px]"
              >
                Search
              </button>

              {/* Instant Suggestions Dropdown */}
              {searchSuggestions.length > 0 && (
                <div className="absolute left-0 right-0 top-full mt-2 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden divide-y divide-slate-100 max-h-60 overflow-y-auto touch-scroll">
                  {searchSuggestions.map((emp) => (
                    <div
                      key={emp.id}
                      onClick={() => {
                        setQuery(emp.employeeCode);
                        handleSelectEmployee(emp.employeeCode);
                      }}
                      className="p-3 hover:bg-indigo-50/80 active:bg-indigo-100/80 cursor-pointer flex items-center justify-between gap-2 transition-colors"
                    >
                      <div className="min-w-0">
                        <span className="font-mono font-bold text-indigo-600 text-sm">{emp.employeeCode}</span>
                        <span className="font-semibold text-slate-800 ml-2.5 text-sm truncate">{emp.fullName}</span>
                      </div>
                      <span className="text-[11px] text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md font-medium shrink-0">
                        {emp.department}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Card>

          {/* Employee Meal Confirmation Card */}
          {loadingCheck ? (
            <Card className="p-8 text-center">
              <div className="inline-block animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full mb-2" />
              <p className="text-sm font-medium text-slate-500">Retrieving employee details...</p>
            </Card>
          ) : quickCheck ? (
            <Card className="border-2 border-indigo-200 bg-gradient-to-b from-white to-indigo-50/20 shadow-md">
              <div className="flex items-start justify-between pb-4 border-b border-slate-100 gap-3">
                <div className="flex items-center gap-3 sm:gap-4 min-w-0">
                  <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-bold text-lg sm:text-xl shadow-lg shadow-indigo-600/30 shrink-0">
                    {quickCheck.employee.firstName.charAt(0)}
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono font-bold text-indigo-700 text-base sm:text-lg">{quickCheck.employee.employeeCode}</span>
                      <Badge variant={quickCheck.employee.status === 'ACTIVE' ? 'success' : 'danger'}>
                        {quickCheck.employee.status}
                      </Badge>
                    </div>
                    <h3 className="text-lg sm:text-xl font-bold text-slate-900 mt-0.5 truncate">{quickCheck.employee.fullName}</h3>
                    <p className="text-xs text-slate-500 truncate">
                      {quickCheck.employee.department} • {quickCheck.employee.position}
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => {
                    setQuickCheck(null);
                    setQuery('');
                    if (searchInputRef.current) searchInputRef.current.focus();
                  }}
                  aria-label="Reset selection"
                  className="p-2 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 active:bg-slate-200 min-w-[36px] min-h-[36px] flex items-center justify-center shrink-0"
                >
                  <RotateCcw className="w-4 h-4" />
                </button>
              </div>

              {/* Duplicate Meal Warning */}
              {quickCheck.alreadyRecordedToday ? (
                <div className="mt-4 p-3.5 sm:p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-sm flex items-start sm:items-center gap-3">
                  <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5 sm:mt-0" />
                  <div>
                    <p className="font-bold">Meal already recorded for this employee today.</p>
                    <p className="text-xs text-amber-700 mt-0.5">
                      Recorded on {quickCheck.todayRecord?.mealDate} (Status: {quickCheck.todayRecord?.mealStatus}, Amount: {quickCheck.todayRecord?.amount} RWF) by {quickCheck.todayRecord?.recordedBy}.
                    </p>
                  </div>
                </div>
              ) : (
                <div className="mt-5 sm:mt-6 space-y-4 sm:space-y-5">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                        Meal Status
                      </label>
                      <select
                        value={mealStatus}
                        onChange={(e) => setMealStatus(e.target.value as MealStatus)}
                        className="w-full px-3 py-2.5 rounded-xl border border-slate-200 font-semibold text-sm focus:ring-2 focus:ring-indigo-500 bg-white min-h-[42px]"
                      >
                        <option value="ATE">ATE (Standard Meal)</option>
                        <option value="DID_NOT_EAT">DID NOT EAT</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                        Amount (RWF)
                      </label>
                      <input
                        type="number"
                        disabled={mealStatus === 'DID_NOT_EAT'}
                        value={mealStatus === 'DID_NOT_EAT' ? 0 : amount}
                        onChange={(e) => setAmount(Number(e.target.value))}
                        className="w-full px-3 py-2.5 rounded-xl border border-slate-200 font-mono font-bold text-sm focus:ring-2 focus:ring-indigo-500 bg-white disabled:bg-slate-100 min-h-[42px]"
                      />
                    </div>
                  </div>

                  <Button
                    onClick={handleRecordMeal}
                    isLoading={submitting}
                    className="w-full py-3 sm:py-3.5 text-sm sm:text-base font-bold shadow-lg shadow-indigo-600/30 gap-2 min-h-[46px]"
                  >
                    <UserCheck className="w-5 h-5" />
                    CONFIRM & RECORD MEAL
                  </Button>
                </div>
              )}
            </Card>
          ) : (
            <Card className="p-8 sm:p-12 text-center border-dashed border-2 border-slate-200">
              <Utensils className="w-10 h-10 sm:w-12 sm:h-12 text-indigo-300 mx-auto mb-3" />
              <h4 className="text-base font-bold text-slate-800">Ready to Record Meal</h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                Enter employee code in the search bar above. Details will immediately pop up for 1-click confirmation.
              </p>
            </Card>
          )}
        </div>

        {/* Right Column: Today's Recorded Meals Stream (5 cols) */}
        <div className="lg:col-span-5">
          <Card
            title="Today's Meal Records Log"
            subtitle={`Recorded transactions for ${selectedDate}`}
          >
            <div className="divide-y divide-slate-100 max-h-[500px] overflow-y-auto touch-scroll mt-2 pr-1">
              {todayRecords.length === 0 ? (
                <p className="py-8 text-center text-xs text-slate-400 italic">
                  No meals recorded yet for selected date.
                </p>
              ) : (
                todayRecords.map((m) => (
                  <div key={m.id} className="py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-1.5 sm:gap-2 text-xs hover:bg-slate-50/80 transition-colors px-2 rounded-lg">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-indigo-700">{m.employeeCode}</span>
                        <span className="font-semibold text-slate-900 truncate">{m.employeeName}</span>
                      </div>
                      <p className="text-[11px] text-slate-400 mt-0.5 truncate">
                        {m.department} • Recorded by {m.recordedBy}
                      </p>
                    </div>
                    <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-start gap-1 shrink-0">
                      <Badge variant={m.mealStatus === 'ATE' ? 'success' : 'danger'}>
                        {m.mealStatus}
                      </Badge>
                      <span className="font-mono font-bold text-slate-900">{m.amount} RWF</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>
      </div>

      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  );
};

export default MealRecording;

