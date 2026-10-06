import React, { useEffect, useRef, useState } from 'react';
import { mealApi } from '../api/mealApi';
import { employeeApi } from '../api/employeeApi';
import { Employee, MealRecord, MealStatus, QuickMealCheckResponse } from '../types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
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
  Edit3,
  Calendar,
  History,
  Plus,
  Users,
  DollarSign,
  Briefcase,
  Layers,
} from 'lucide-react';

export const MealRecording: React.FC = () => {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const modalSearchInputRef = useRef<HTMLInputElement>(null);

  // State
  const [query, setQuery] = useState('');
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [recordDate, setRecordDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [mealStatus, setMealStatus] = useState<MealStatus>('ATE');
  const [amount, setAmount] = useState<number>(600);

  // Quick check / Selected Employee
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);
  const [quickCheck, setQuickCheck] = useState<QuickMealCheckResponse | null>(null);
  const [loadingCheck, setLoadingCheck] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [isEditingExisting, setIsEditingExisting] = useState(false);

  // Employee Meal History
  const [employeeHistory, setEmployeeHistory] = useState<MealRecord[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Search Suggestions
  const [searchSuggestions, setSearchSuggestions] = useState<Employee[]>([]);

  // Today's Log
  const [todayRecords, setTodayRecords] = useState<MealRecord[]>([]);
  const [logFilterQuery, setLogFilterQuery] = useState('');

  // Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (type: 'success' | 'error' | 'info', message: string) => {
    setToasts((prev) => [...prev, { id: Date.now().toString(), type, message }]);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  // Sync recordDate with selectedDate on initial load
  useEffect(() => {
    setRecordDate(selectedDate);
    fetchTodayRecords();
  }, [selectedDate]);

  // Focus search input on mount
  useEffect(() => {
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, []);

  // Debounced Employee Search
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

  // Fetch recorded meals for the selected date
  const fetchTodayRecords = async () => {
    try {
      const data = await mealApi.getMealRecords({
        startDate: selectedDate,
        endDate: selectedDate,
        size: 100,
      });
      setTodayRecords(data.content || []);
    } catch (e) {
      console.error(e);
    }
  };

  // Fetch meal history for the selected employee
  const fetchEmployeeHistory = async (empId: number) => {
    setLoadingHistory(true);
    try {
      const history = await mealApi.getEmployeeMealHistory(empId);
      setEmployeeHistory(history || []);
    } catch (e) {
      console.error(e);
      setEmployeeHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  };

  // Select an employee from search results
  const handleSelectEmployee = async (employee: Employee) => {
    setSelectedEmployee(employee);
    setLoadingCheck(true);
    setSearchSuggestions([]);
    setQuery(employee.fullName);
    setIsEditingExisting(false);

    try {
      const result = await mealApi.quickCheck(employee.id.toString(), recordDate);
      setQuickCheck(result);

      if (result.alreadyRecordedToday && result.todayRecord) {
        setMealStatus(result.todayRecord.mealStatus);
        setAmount(result.todayRecord.amount || 600);
      } else {
        setMealStatus('ATE');
        setAmount(result.defaultMealPrice || 600);
      }

      // Fetch employee's complete meal history
      fetchEmployeeHistory(employee.id);
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to check employee status');
      setQuickCheck(null);
    } finally {
      setLoadingCheck(false);
    }
  };

  // Re-check record status when recordDate changes
  const handleRecordDateChange = async (newDate: string) => {
    setRecordDate(newDate);
    if (selectedEmployee) {
      setLoadingCheck(true);
      setIsEditingExisting(false);
      try {
        const result = await mealApi.quickCheck(selectedEmployee.id.toString(), newDate);
        setQuickCheck(result);
        if (result.alreadyRecordedToday && result.todayRecord) {
          setMealStatus(result.todayRecord.mealStatus);
          setAmount(result.todayRecord.amount || 600);
        } else {
          setMealStatus('ATE');
          setAmount(result.defaultMealPrice || 600);
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoadingCheck(false);
      }
    }
  };

  // Handle status change
  const handleStatusChange = (status: MealStatus) => {
    setMealStatus(status);
    if (status === 'DID_NOT_EAT') {
      setAmount(0);
    } else {
      if (amount === 0) {
        setAmount(quickCheck?.defaultMealPrice || 600);
      }
    }
  };

  // Save new record
  const handleSaveRecord = async () => {
    if (!selectedEmployee) {
      addToast('error', 'Please search and select an employee first.');
      return;
    }
    if (!recordDate) {
      addToast('error', 'Please select a date.');
      return;
    }
    if (mealStatus === 'ATE' && (amount === undefined || amount === null || amount <= 0)) {
      addToast('error', 'Please enter a valid meal amount greater than 0 for ATE status.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await mealApi.recordMeal({
        employeeId: selectedEmployee.id,
        mealDate: recordDate,
        mealStatus: mealStatus,
        amount: mealStatus === 'DID_NOT_EAT' ? 0 : Number(amount),
      });

      addToast('success', res.message || `Meal record saved successfully for ${selectedEmployee.fullName}`);

      // Refresh employee history and today's table
      fetchEmployeeHistory(selectedEmployee.id);
      fetchTodayRecords();

      // Refresh check status
      const updatedCheck = await mealApi.quickCheck(selectedEmployee.id.toString(), recordDate);
      setQuickCheck(updatedCheck);
      setIsEditingExisting(false);

      if (isModalOpen) {
        setIsModalOpen(false);
      }
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to save meal record.');
    } finally {
      setSubmitting(false);
    }
  };

  // Update existing record
  const handleUpdateRecord = async () => {
    if (!quickCheck?.todayRecord?.id) {
      addToast('error', 'No existing record ID found to update.');
      return;
    }

    setSubmitting(true);
    try {
      await mealApi.updateMealRecord(quickCheck.todayRecord.id, {
        mealStatus: mealStatus,
        amount: mealStatus === 'DID_NOT_EAT' ? 0 : Number(amount),
      });

      addToast('success', `Meal record updated successfully for ${selectedEmployee?.fullName || 'employee'}`);

      // Refresh data
      if (selectedEmployee) {
        fetchEmployeeHistory(selectedEmployee.id);
        const updatedCheck = await mealApi.quickCheck(selectedEmployee.id.toString(), recordDate);
        setQuickCheck(updatedCheck);
      }
      fetchTodayRecords();
      setIsEditingExisting(false);

      if (isModalOpen) {
        setIsModalOpen(false);
      }
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to update meal record.');
    } finally {
      setSubmitting(false);
    }
  };

  // Reset form
  const handleResetForm = () => {
    setSelectedEmployee(null);
    setQuickCheck(null);
    setQuery('');
    setSearchSuggestions([]);
    setEmployeeHistory([]);
    setIsEditingExisting(false);
    setMealStatus('ATE');
    setAmount(600);
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  };

  // Filtered log records
  const filteredLogRecords = todayRecords.filter((rec) => {
    if (!logFilterQuery.trim()) return true;
    const q = logFilterQuery.toLowerCase();
    return (
      rec.employeeName?.toLowerCase().includes(q) ||
      rec.employeeCode?.toLowerCase().includes(q) ||
      rec.department?.toLowerCase().includes(q) ||
      rec.recordedBy?.toLowerCase().includes(q)
    );
  });

  const ateCount = todayRecords.filter((r) => r.mealStatus === 'ATE').length;
  const didNotEatCount = todayRecords.filter((r) => r.mealStatus === 'DID_NOT_EAT').length;
  const totalAmountUsed = todayRecords
    .filter((r) => r.mealStatus === 'ATE')
    .reduce((sum, r) => sum + (Number(r.amount) || 0), 0);

  return (
    <div className="space-y-6 animate-fadeIn pb-12 font-sans">
      {/* Top Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-4 sm:p-6 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border border-indigo-900/30">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="p-1.5 bg-amber-500/20 rounded-lg text-amber-400 border border-amber-500/30">
              <Zap className="w-4 h-4 fill-amber-400" />
            </span>
            <span className="text-[11px] sm:text-xs font-bold text-amber-300 uppercase tracking-wider">
              Daily Attendance & Meal System
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black tracking-tight text-white">Manual Meal Recording</h2>
          <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-xl">
            Search any employee by name or code to instantly record or update their meal attendance for any date.
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex flex-wrap items-center gap-2.5 self-stretch sm:self-auto shrink-0">
          {/* Target Date Filter */}
          <div className="bg-white/10 backdrop-blur-md px-3 py-2 rounded-xl border border-white/20 flex items-center gap-2 text-white">
            <Calendar className="w-4 h-4 text-indigo-300" />
            <span className="text-xs font-semibold text-slate-200">Date:</span>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="bg-transparent text-white text-xs font-mono font-bold focus:outline-none cursor-pointer"
            />
          </div>

          {/* Quick Record Modal Trigger */}
          <Button
            onClick={() => {
              setIsModalOpen(true);
              setTimeout(() => modalSearchInputRef.current?.focus(), 100);
            }}
            className="bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-bold text-xs sm:text-sm px-3.5 py-2.5 rounded-xl shadow-lg shadow-emerald-600/30 flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>+ Record Meal</span>
          </Button>
        </div>
      </div>

      {/* Summary KPI Cards for Active Selected Date */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-white p-3.5 sm:p-4 rounded-xl border border-slate-200/80 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold shrink-0">
            <Users className="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Total Recorded</p>
            <p className="text-lg sm:text-xl font-black text-slate-900">{todayRecords.length}</p>
          </div>
        </div>

        <div className="bg-white p-3.5 sm:p-4 rounded-xl border border-slate-200/80 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold shrink-0">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          </div>
          <div>
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Ate Meals</p>
            <p className="text-lg sm:text-xl font-black text-emerald-600">{ateCount}</p>
          </div>
        </div>

        <div className="bg-white p-3.5 sm:p-4 rounded-xl border border-slate-200/80 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold shrink-0">
            <AlertCircle className="w-5 h-5 text-rose-600" />
          </div>
          <div>
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Did Not Eat</p>
            <p className="text-lg sm:text-xl font-black text-rose-600">{didNotEatCount}</p>
          </div>
        </div>

        <div className="bg-white p-3.5 sm:p-4 rounded-xl border border-slate-200/80 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold shrink-0">
            <DollarSign className="w-5 h-5 text-indigo-600" />
          </div>
          <div>
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Amount Used</p>
            <p className="text-lg sm:text-xl font-black text-indigo-700">{totalAmountUsed.toLocaleString()} RWF</p>
          </div>
        </div>
      </div>

      {/* Main Grid: Left Column = Manual Recording Form, Right Column = Meal Records Log */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 sm:gap-6 items-start">
        {/* Left Column: Manual Recording Form (7 Cols) */}
        <div className="lg:col-span-7 space-y-5 sm:space-y-6">
          <Card
            title="Manual Meal Recording"
            subtitle="Search employee by name (e.g. Abel) or ID to record daily meal"
          >
            {/* Employee Search Input */}
            <div className="relative mb-4">
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                Search Employee
              </label>
              <div className="relative">
                <Search className="w-5 h-5 text-indigo-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  ref={searchInputRef}
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Type employee name (e.g. Abel) or ID (EMP001)..."
                  className="w-full pl-10 pr-24 py-3 rounded-xl border-2 border-slate-200 focus:border-indigo-600 focus:ring-4 focus:ring-indigo-500/10 text-sm font-semibold bg-white shadow-xs transition-all placeholder:text-slate-400"
                />
                {query && (
                  <button
                    onClick={handleResetForm}
                    className="absolute right-2 top-1/2 -translate-y-1/2 px-2.5 py-1 text-xs font-bold text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
                  >
                    Clear
                  </button>
                )}
              </div>

              {/* Instant Search Suggestions Dropdown */}
              {searchSuggestions.length > 0 && (
                <div className="absolute left-0 right-0 top-full mt-1.5 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden divide-y divide-slate-100 max-h-64 overflow-y-auto touch-scroll">
                  {searchSuggestions.map((emp) => (
                    <div
                      key={emp.id}
                      onClick={() => handleSelectEmployee(emp)}
                      className="p-3 hover:bg-indigo-50/80 active:bg-indigo-100/80 cursor-pointer flex items-center justify-between gap-3 transition-colors group"
                    >
                      <div className="min-w-0 flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-700 font-bold text-xs flex items-center justify-center shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                          {emp.firstName.charAt(0)}
                        </div>
                        <div className="min-w-0">
                          <p className="font-bold text-slate-900 text-sm truncate">{emp.fullName}</p>
                          <p className="text-xs text-slate-500 truncate">
                            <span className="font-mono font-semibold text-indigo-600">{emp.employeeCode}</span> • {emp.position || 'Worker'}
                          </p>
                        </div>
                      </div>
                      <span className="text-[11px] font-semibold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md shrink-0">
                        {emp.department}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Selected Employee Recording Section */}
            {loadingCheck ? (
              <div className="p-8 text-center border-2 border-indigo-100 rounded-2xl bg-indigo-50/20">
                <div className="inline-block animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full mb-2" />
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Checking employee records...</p>
              </div>
            ) : selectedEmployee ? (
              <div className="border-2 border-indigo-200/80 rounded-2xl p-4 sm:p-5 bg-gradient-to-b from-white via-indigo-50/15 to-white shadow-sm space-y-4">
                {/* Employee Info Header */}
                <div className="flex items-start justify-between pb-3 border-b border-slate-100 gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-black text-lg shadow-md shadow-indigo-600/30 shrink-0">
                      {selectedEmployee.firstName.charAt(0)}
                    </div>
                    <div>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono font-bold text-indigo-700 text-sm">{selectedEmployee.employeeCode}</span>
                        <Badge variant={selectedEmployee.status === 'ACTIVE' ? 'success' : 'danger'}>
                          {selectedEmployee.status}
                        </Badge>
                      </div>
                      <h4 className="text-base sm:text-lg font-black text-slate-900 mt-0.5">{selectedEmployee.fullName}</h4>
                      <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                        <Briefcase className="w-3.5 h-3.5 text-slate-400" />
                        <span>{selectedEmployee.position || 'Worker'}</span>
                        <span>•</span>
                        <Layers className="w-3.5 h-3.5 text-slate-400" />
                        <span>{selectedEmployee.department}</span>
                      </p>
                    </div>
                  </div>

                  <button
                    onClick={handleResetForm}
                    className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg text-xs font-semibold flex items-center gap-1"
                    title="Change employee"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Change</span>
                  </button>
                </div>

                {/* Duplicate / Existing Record Notice */}
                {quickCheck?.alreadyRecordedToday && !isEditingExisting && (
                  <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-start gap-2.5">
                      <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                      <div>
                        <p className="text-xs font-bold leading-tight">
                          A meal record already exists for {selectedEmployee.fullName} on {recordDate}.
                        </p>
                        <p className="text-[11px] text-amber-700 mt-0.5">
                          Status: <span className="font-bold">{quickCheck.todayRecord?.mealStatus}</span> • Amount: <span className="font-bold">{quickCheck.todayRecord?.amount} RWF</span> • Recorded by: {quickCheck.todayRecord?.recordedBy || 'System'}
                        </p>
                      </div>
                    </div>
                    <Button
                      onClick={() => setIsEditingExisting(true)}
                      className="bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shrink-0 flex items-center gap-1"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                      <span>Edit Record</span>
                    </Button>
                  </div>
                )}

                {/* Form Fields: Date, Status, Amount */}
                {(!quickCheck?.alreadyRecordedToday || isEditingExisting) && (
                  <div className="space-y-4 pt-1">
                    {isEditingExisting && (
                      <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-xl flex items-center justify-between text-xs text-blue-800">
                        <span className="font-bold">Editing Existing Record for {recordDate}</span>
                        <button
                          onClick={() => setIsEditingExisting(false)}
                          className="text-blue-600 hover:underline font-semibold"
                        >
                          Cancel
                        </button>
                      </div>
                    )}

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      {/* Date */}
                      <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                          Date
                        </label>
                        <input
                          type="date"
                          value={recordDate}
                          onChange={(e) => handleRecordDateChange(e.target.value)}
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 font-semibold text-xs focus:ring-2 focus:ring-indigo-500 bg-white"
                        />
                      </div>

                      {/* Meal Status */}
                      <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                          Meal Status
                        </label>
                        <select
                          value={mealStatus}
                          onChange={(e) => handleStatusChange(e.target.value as MealStatus)}
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 font-bold text-xs focus:ring-2 focus:ring-indigo-500 bg-white"
                        >
                          <option value="ATE">ATE</option>
                          <option value="DID_NOT_EAT">DID NOT EAT</option>
                        </select>
                      </div>

                      {/* Amount */}
                      <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                          Amount Used (RWF)
                        </label>
                        <input
                          type="number"
                          disabled={mealStatus === 'DID_NOT_EAT'}
                          value={mealStatus === 'DID_NOT_EAT' ? 0 : amount}
                          onChange={(e) => setAmount(Number(e.target.value))}
                          placeholder="600"
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono font-bold text-xs focus:ring-2 focus:ring-indigo-500 bg-white disabled:bg-slate-100 disabled:text-slate-400"
                        />
                      </div>
                    </div>

                    {/* Action Button */}
                    <div className="pt-2">
                      {isEditingExisting ? (
                        <Button
                          onClick={handleUpdateRecord}
                          isLoading={submitting}
                          className="w-full py-3 bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white text-sm font-bold rounded-xl shadow-md shadow-amber-600/25 flex items-center justify-center gap-2"
                        >
                          <Edit3 className="w-4 h-4" />
                          <span>UPDATE RECORD</span>
                        </Button>
                      ) : (
                        <Button
                          onClick={handleSaveRecord}
                          isLoading={submitting}
                          className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white text-sm font-bold rounded-xl shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2"
                        >
                          <UserCheck className="w-4 h-4" />
                          <span>SAVE RECORD</span>
                        </Button>
                      )}
                    </div>
                  </div>
                )}

                {/* Employee Meal History Preview */}
                <div className="pt-4 border-t border-slate-100">
                  <div className="flex items-center justify-between mb-2.5">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700 uppercase tracking-wider">
                      <History className="w-4 h-4 text-indigo-600" />
                      <span>Meal History: {selectedEmployee.fullName}</span>
                    </div>
                    <span className="text-[11px] font-semibold text-slate-400">
                      {employeeHistory.length} Total Records
                    </span>
                  </div>

                  {loadingHistory ? (
                    <div className="py-4 text-center text-xs text-slate-400">Loading history...</div>
                  ) : employeeHistory.length === 0 ? (
                    <p className="text-xs text-slate-400 italic py-2">No previous meal records for this employee.</p>
                  ) : (
                    <div className="divide-y divide-slate-100 max-h-52 overflow-y-auto touch-scroll border border-slate-100 rounded-xl bg-slate-50/50 p-2">
                      {employeeHistory.map((h) => (
                        <div key={h.id} className="py-2 px-1.5 flex items-center justify-between text-xs gap-2">
                          <div>
                            <span className="font-mono font-bold text-slate-800">{h.mealDate}</span>
                            <span className="text-[11px] text-slate-400 ml-2">
                              Recorded by {h.recordedBy || 'System'}
                            </span>
                          </div>
                          <div className="flex items-center gap-2 shrink-0">
                            <Badge variant={h.mealStatus === 'ATE' ? 'success' : 'danger'}>
                              {h.mealStatus}
                            </Badge>
                            <span className="font-mono font-bold text-slate-800">{h.amount || 0} RWF</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="p-8 sm:p-10 text-center border-2 border-dashed border-slate-200 rounded-2xl bg-slate-50/50">
                <Utensils className="w-10 h-10 text-indigo-300 mx-auto mb-2.5" />
                <h4 className="text-sm font-bold text-slate-700">No Employee Selected</h4>
                <p className="text-xs text-slate-400 max-w-sm mx-auto mt-1">
                  Type an employee name (e.g. Abel) or employee ID in the search bar above to begin recording.
                </p>
              </div>
            )}
          </Card>
        </div>

        {/* Right Column: Today's Recorded Meals Stream / Table (5 Cols) */}
        <div className="lg:col-span-5 space-y-4">
          <Card
            title={`Recorded Meals Log`}
            subtitle={`Records for ${selectedDate}`}
          >
            {/* Filter Search inside log */}
            <div className="mb-3">
              <div className="relative">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={logFilterQuery}
                  onChange={(e) => setLogFilterQuery(e.target.value)}
                  placeholder="Filter log by name or department..."
                  className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>

            {/* Meal Records Stream */}
            <div className="divide-y divide-slate-100 max-h-[540px] overflow-y-auto touch-scroll pr-1">
              {filteredLogRecords.length === 0 ? (
                <p className="py-10 text-center text-xs text-slate-400 italic">
                  No meal records found for {selectedDate}.
                </p>
              ) : (
                filteredLogRecords.map((m) => (
                  <div
                    key={m.id}
                    className="py-2.5 px-2 flex items-center justify-between gap-2 text-xs hover:bg-slate-50 rounded-lg transition-colors group"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono font-bold text-indigo-700">{m.employeeCode}</span>
                        <span className="font-bold text-slate-900 truncate">{m.employeeName}</span>
                      </div>
                      <p className="text-[11px] text-slate-400 mt-0.5 truncate">
                        {m.department} • By {m.recordedBy || 'System'}
                      </p>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <div className="text-right">
                        <Badge variant={m.mealStatus === 'ATE' ? 'success' : 'danger'}>
                          {m.mealStatus}
                        </Badge>
                        <p className="font-mono font-bold text-slate-900 text-[11px] mt-0.5">{m.amount || 0} RWF</p>
                      </div>

                      <button
                        onClick={async () => {
                          try {
                            const emp = await employeeApi.getById(m.employeeId);
                            handleSelectEmployee(emp);
                          } catch (e) {
                            console.error(e);
                          }
                        }}
                        className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-indigo-50 transition-colors"
                        title="View & Edit Record"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>
      </div>

      {/* Quick Record Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Quick Record Meal"
        maxWidth="lg"
      >
        <div className="space-y-4 font-sans">
          {/* Search Bar in Modal */}
          <div className="relative">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Search Employee
            </label>
            <div className="relative">
              <Search className="w-4 h-4 text-indigo-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                ref={modalSearchInputRef}
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Type name (e.g. Abel) or ID..."
                className="w-full pl-9 pr-20 py-2.5 rounded-xl border border-slate-200 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            {searchSuggestions.length > 0 && (
              <div className="absolute left-0 right-0 top-full mt-1 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden divide-y divide-slate-100 max-h-48 overflow-y-auto">
                {searchSuggestions.map((emp) => (
                  <div
                    key={emp.id}
                    onClick={() => handleSelectEmployee(emp)}
                    className="p-2.5 hover:bg-indigo-50 cursor-pointer flex items-center justify-between text-xs"
                  >
                    <span className="font-bold text-slate-800">{emp.fullName} ({emp.employeeCode})</span>
                    <span className="text-slate-500 text-[11px]">{emp.department}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {selectedEmployee && (
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-black text-slate-900 text-sm">{selectedEmployee.fullName}</p>
                  <p className="text-slate-500">{selectedEmployee.employeeCode} • {selectedEmployee.position || 'Worker'}</p>
                </div>
                <Badge variant={selectedEmployee.status === 'ACTIVE' ? 'success' : 'danger'}>
                  {selectedEmployee.status}
                </Badge>
              </div>

              {quickCheck?.alreadyRecordedToday && (
                <div className="p-2.5 bg-amber-50 border border-amber-200 rounded-lg text-amber-800">
                  <p className="font-bold">Meal already recorded for this employee on {recordDate}:</p>
                  <p className="text-[11px] mt-0.5">
                    {quickCheck.todayRecord?.mealStatus} • {quickCheck.todayRecord?.amount} RWF • By {quickCheck.todayRecord?.recordedBy}
                  </p>
                </div>
              )}

              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="block font-bold text-slate-600 mb-0.5">Date</label>
                  <input
                    type="date"
                    value={recordDate}
                    onChange={(e) => handleRecordDateChange(e.target.value)}
                    className="w-full p-2 border border-slate-300 rounded-lg font-mono text-xs font-semibold bg-white"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-600 mb-0.5">Status</label>
                  <select
                    value={mealStatus}
                    onChange={(e) => handleStatusChange(e.target.value as MealStatus)}
                    className="w-full p-2 border border-slate-300 rounded-lg font-bold text-xs bg-white"
                  >
                    <option value="ATE">ATE</option>
                    <option value="DID_NOT_EAT">DID NOT EAT</option>
                  </select>
                </div>
                <div>
                  <label className="block font-bold text-slate-600 mb-0.5">Amount (RWF)</label>
                  <input
                    type="number"
                    disabled={mealStatus === 'DID_NOT_EAT'}
                    value={mealStatus === 'DID_NOT_EAT' ? 0 : amount}
                    onChange={(e) => setAmount(Number(e.target.value))}
                    className="w-full p-2 border border-slate-300 rounded-lg font-mono font-bold text-xs bg-white disabled:bg-slate-100"
                  />
                </div>
              </div>

              <div className="pt-2 flex gap-2">
                {quickCheck?.alreadyRecordedToday ? (
                  <Button
                    onClick={handleUpdateRecord}
                    isLoading={submitting}
                    className="flex-1 py-2 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs rounded-xl"
                  >
                    Update Record
                  </Button>
                ) : (
                  <Button
                    onClick={handleSaveRecord}
                    isLoading={submitting}
                    className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl"
                  >
                    Save Record
                  </Button>
                )}
                <Button
                  variant="secondary"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-xs"
                >
                  Close
                </Button>
              </div>
            </div>
          )}
        </div>
      </Modal>

      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  );
};

export default MealRecording;
