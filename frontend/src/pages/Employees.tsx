import React, { useEffect, useState, useCallback } from 'react';
import { employeeApi } from '../api/employeeApi';
import { mealApi } from '../api/mealApi';
import {
  EmployeeAttendance,
  EmployeeAttendanceSummary,
  EmployeeStatus,
  MealRecord,
  CreateEmployeeRequest,
  UpdateEmployeeRequest,
} from '../types';
import { useAuth } from '../context/AuthContext';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import Modal from '../components/common/Modal';
import Skeleton from '../components/common/Skeleton';
import Toast from '../components/common/Toast';
import {
  Users,
  Search,
  Plus,
  Edit2,
  UserX,
  History,
  Calendar,
  CheckCircle2,
  XCircle,
  Clock,
  Coins,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';

export const Employees: React.FC = () => {
  const { hasRole } = useAuth();

  // Selected date defaults to today (YYYY-MM-DD)
  const todayStr = new Date().toISOString().split('T')[0];
  const [selectedDate, setSelectedDate] = useState<string>(todayStr);

  // Filters & Pagination State
  const [query, setQuery] = useState('');
  const [department, setDepartment] = useState('');
  const [mealStatusFilter, setMealStatusFilter] = useState('');
  const [employeeStatusFilter, setEmployeeStatusFilter] = useState<EmployeeStatus | ''>('ACTIVE');
  const [page, setPage] = useState(0);
  const [size] = useState(10);

  // Data State
  const [attendanceList, setAttendanceList] = useState<EmployeeAttendance[]>([]);
  const [summary, setSummary] = useState<EmployeeAttendanceSummary>({
    totalActiveEmployees: 0,
    ateCount: 0,
    didNotEatCount: 0,
    notRecordedCount: 0,
    totalMealCost: 0,
    currency: 'RWF',
  });
  const [departments, setDepartments] = useState<string[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Modals & History State
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeactivateModalOpen, setIsDeactivateModalOpen] = useState(false);
  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<EmployeeAttendance | null>(null);
  const [mealHistory, setMealHistory] = useState<MealRecord[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Toast
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  // Add Employee Form State
  const [addForm, setAddForm] = useState<CreateEmployeeRequest>({
    employeeCode: '',
    employeeName: '',
    phone: '',
    position: '',
    mealStatus: 'ATE',
    amount: 1500,
  });

  // Edit Employee Form State
  const [editForm, setEditForm] = useState<UpdateEmployeeRequest>({
    employeeName: '',
    position: '',
    phone: '',
    status: 'ACTIVE',
  });

  const fetchAttendance = useCallback(async () => {
    setLoading(true);
    try {
      const data = await employeeApi.getAttendance({
        date: selectedDate,
        query: query || undefined,
        department: department || undefined,
        mealStatus: mealStatusFilter || undefined,
        status: (employeeStatusFilter as EmployeeStatus) || undefined,
        page,
        size,
      });

      setSummary(data.summary);
      setAttendanceList(data.employees.content);
      setTotalPages(data.employees.totalPages);
      setTotalElements(data.employees.totalElements);
    } catch (e: any) {
      setToast({ message: 'Failed to load employee attendance data', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [selectedDate, query, department, mealStatusFilter, employeeStatusFilter, page, size]);

  useEffect(() => {
    fetchAttendance();
  }, [fetchAttendance]);

  useEffect(() => {
    const fetchDepts = async () => {
      try {
        const depts = await employeeApi.getDepartments();
        setDepartments(depts);
      } catch (e) {
        // Silent fallback
      }
    };
    fetchDepts();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();

    // Frontend validation
    if (addForm.mealStatus === 'DID_NOT_EAT' && addForm.amount > 0) {
      setToast({ message: 'Amount must be 0 RWF when Meal Status is DID NOT EAT', type: 'error' });
      return;
    }
    if (addForm.mealStatus === 'ATE' && addForm.amount <= 0) {
      setToast({ message: 'Amount must be greater than 0 RWF for ATE status', type: 'error' });
      return;
    }

    setSubmitting(true);
    try {
      await employeeApi.create({
        ...addForm,
        mealDate: selectedDate,
      });
      setToast({ message: `Employee ${addForm.employeeCode} (${addForm.employeeName}) created successfully!`, type: 'success' });
      setIsAddModalOpen(false);
      setAddForm({
        employeeCode: '',
        employeeName: '',
        phone: '',
        position: '',
        mealStatus: 'ATE',
        amount: 1500,
      });
      fetchAttendance();
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Failed to create employee', type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEmployee) return;
    setSubmitting(true);
    try {
      await employeeApi.update(selectedEmployee.id, editForm);
      setToast({ message: `Employee ${selectedEmployee.employeeCode} updated successfully!`, type: 'success' });
      setIsEditModalOpen(false);
      fetchAttendance();
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Failed to update employee', type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async () => {
    if (!selectedEmployee) return;
    setSubmitting(true);
    try {
      await employeeApi.deactivate(selectedEmployee.id);
      setToast({ message: `Employee ${selectedEmployee.employeeCode} deactivated`, type: 'success' });
      setIsDeactivateModalOpen(false);
      fetchAttendance();
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Failed to deactivate employee', type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const openEditModal = (emp: EmployeeAttendance) => {
    setSelectedEmployee(emp);
    setEditForm({
      employeeName: emp.fullName,
      position: emp.position || '',
      phone: emp.telephone || '',
      status: emp.status,
    });
    setIsEditModalOpen(true);
  };

  const openDeactivateModal = (emp: EmployeeAttendance) => {
    setSelectedEmployee(emp);
    setIsDeactivateModalOpen(true);
  };

  const openHistoryModal = async (emp: EmployeeAttendance) => {
    setSelectedEmployee(emp);
    setIsHistoryModalOpen(true);
    setLoadingHistory(true);
    try {
      const history = await mealApi.getEmployeeMealHistory(emp.id);
      setMealHistory(history);
    } catch (err) {
      setMealHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  };

  const formatCurrency = (val: number | null | undefined, curr: string) => {
    if (val === null || val === undefined) return '—';
    return `${val.toLocaleString()} ${curr}`;
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Users className="w-7 h-7 text-indigo-600" />
            Employees & Daily Meal Attendance
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Monitor daily employee meal participation, statuses, and costs for any selected date
          </p>
        </div>

        {hasRole('ADMIN', 'HR') && (
          <Button variant="primary" onClick={() => setIsAddModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" />
            Add New Employee
          </Button>
        )}
      </div>

      {/* Top Summary Cards (Calculated for Selected Date) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
        {/* Total Active Employees */}
        <Card className="p-4 border-l-4 border-l-sky-500 bg-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Active Staff</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{summary.totalActiveEmployees}</h3>
            </div>
            <div className="w-10 h-10 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
          </div>
        </Card>

        {/* Ate */}
        <Card className="p-4 border-l-4 border-l-emerald-500 bg-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Ate Today</p>
              <h3 className="text-2xl font-bold text-emerald-600 mt-1">{summary.ateCount}</h3>
            </div>
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </div>
        </Card>

        {/* Did Not Eat */}
        <Card className="p-4 border-l-4 border-l-rose-500 bg-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Did Not Eat</p>
              <h3 className="text-2xl font-bold text-rose-600 mt-1">{summary.didNotEatCount}</h3>
            </div>
            <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
              <XCircle className="w-5 h-5" />
            </div>
          </div>
        </Card>

        {/* Not Recorded */}
        <Card className="p-4 border-l-4 border-l-amber-500 bg-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Not Recorded</p>
              <h3 className="text-2xl font-bold text-amber-600 mt-1">{summary.notRecordedCount}</h3>
            </div>
            <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
              <Clock className="w-5 h-5" />
            </div>
          </div>
        </Card>

        {/* Total Meal Cost */}
        <Card className="p-4 border-l-4 border-l-indigo-600 bg-white sm:col-span-2 lg:col-span-1">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Meal Cost</p>
              <h3 className="text-lg font-bold text-indigo-700 mt-1 truncate">
                {summary.totalMealCost.toLocaleString()} {summary.currency}
              </h3>
            </div>
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Coins className="w-5 h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filter Toolbar */}
      <Card className="p-4">
        <div className="flex flex-col lg:flex-row gap-3 items-stretch lg:items-center justify-between">
          {/* Date Selector */}
          <div className="flex items-center gap-2 bg-slate-50 border border-slate-300 rounded-xl px-3 py-1.5 shrink-0">
            <Calendar className="w-4 h-4 text-indigo-600 shrink-0" />
            <label className="text-xs font-bold text-slate-700 whitespace-nowrap">Meal Date:</label>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => {
                setSelectedDate(e.target.value);
                setPage(0);
              }}
              className="bg-transparent text-sm font-semibold text-slate-800 focus:outline-none cursor-pointer"
            />
          </div>

          {/* Search Box */}
          <div className="relative flex-1 min-w-[200px]">
            <input
              type="text"
              placeholder="Search by ID, name, or phone..."
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 pl-9"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          </div>

          {/* Department Filter */}
          <div className="w-full lg:w-44">
            <select
              value={department}
              onChange={(e) => {
                setDepartment(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">All Departments</option>
              {departments.map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </div>

          {/* Meal Status Filter */}
          <div className="w-full lg:w-40">
            <select
              value={mealStatusFilter}
              onChange={(e) => {
                setMealStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">All Meal Status</option>
              <option value="ATE">ATE</option>
              <option value="DID_NOT_EAT">DID NOT EAT</option>
              <option value="NOT_RECORDED">NOT RECORDED</option>
            </select>
          </div>

          {/* Employee Status Filter */}
          <div className="w-full lg:w-36">
            <select
              value={employeeStatusFilter}
              onChange={(e) => {
                setEmployeeStatusFilter(e.target.value as any);
                setPage(0);
              }}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">All Staff</option>
              <option value="ACTIVE">Active Staff</option>
              <option value="INACTIVE">Inactive Staff</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Main Employee Attendance Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-700">
            <thead className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
              <tr>
                <th className="px-4 py-3.5">Employee ID</th>
                <th className="px-4 py-3.5">Employee Name</th>
                <th className="px-4 py-3.5">Telephone</th>
                <th className="px-4 py-3.5">Position</th>
                <th className="px-4 py-3.5 text-center">Meal Status</th>
                <th className="px-4 py-3.5 text-right">Amount Used</th>
                <th className="px-4 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={7} className="px-4 py-4">
                      <Skeleton className="h-4 w-full" />
                    </td>
                  </tr>
                ))
              ) : attendanceList.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-slate-400 text-sm">
                    <Users className="w-10 h-10 mx-auto mb-2 text-slate-300" />
                    No employees found matching the filter criteria.
                  </td>
                </tr>
              ) : (
                attendanceList.map((emp) => (
                  <tr key={emp.id} className="hover:bg-slate-50/80 transition-colors">
                    {/* Employee ID */}
                    <td className="px-4 py-3.5 font-mono font-bold text-indigo-700">
                      {emp.employeeCode}
                    </td>

                    {/* Employee Name */}
                    <td className="px-4 py-3.5">
                      <div className="font-semibold text-slate-900">{emp.fullName}</div>
                    </td>

                    {/* Telephone */}
                    <td className="px-4 py-3.5 text-slate-600 font-mono text-xs">
                      {emp.telephone || '—'}
                    </td>

                    {/* Position */}
                    <td className="px-4 py-3.5 text-slate-600 text-xs">
                      {emp.position || '—'}
                    </td>

                    {/* Meal Status Badge */}
                    <td className="px-4 py-3.5 text-center">
                      {emp.mealStatus === 'ATE' ? (
                        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                          ATE
                        </span>
                      ) : emp.mealStatus === 'DID_NOT_EAT' ? (
                        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
                          <XCircle className="w-3.5 h-3.5 mr-1" />
                          DID NOT EAT
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-500 border border-slate-200">
                          <Clock className="w-3.5 h-3.5 mr-1" />
                          NOT RECORDED
                        </span>
                      )}
                    </td>

                    {/* Amount Used */}
                    <td className="px-4 py-3.5 text-right font-mono font-bold text-slate-900">
                      {emp.mealStatus === 'ATE'
                        ? formatCurrency(emp.amount, emp.currency)
                        : emp.mealStatus === 'DID_NOT_EAT'
                        ? `0 ${emp.currency}`
                        : '—'}
                    </td>

                    {/* Actions */}
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* View Meal History */}
                        <button
                          onClick={() => openHistoryModal(emp)}
                          title="View Historical Meal Records"
                          className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
                        >
                          <History className="w-4 h-4" />
                        </button>

                        {/* Edit Employee */}
                        {hasRole('ADMIN', 'HR') && (
                          <>
                            <button
                              onClick={() => openEditModal(emp)}
                              title="Edit Employee Details"
                              className="p-1.5 rounded-lg text-slate-500 hover:text-blue-600 hover:bg-blue-50 transition-colors"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>

                            {/* Deactivate Employee */}
                            {emp.status === 'ACTIVE' && (
                              <button
                                onClick={() => openDeactivateModal(emp)}
                                title="Deactivate Employee"
                                className="p-1.5 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                              >
                                <UserX className="w-4 h-4" />
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 bg-slate-50 border-t border-slate-200 text-xs text-slate-500">
            <span>
              Page {page + 1} of {totalPages} ({totalElements} total employees)
            </span>
            <div className="flex items-center gap-1.5">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Modal: Add New Employee (Updated with exact 6 fields) */}
      <Modal isOpen={isAddModalOpen} onClose={() => setIsAddModalOpen(false)} title="Add New Employee" maxWidth="md">
        <form onSubmit={handleCreate} className="space-y-4">
          {/* Field 1: Employee ID */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Employee ID *</label>
            <input
              type="text"
              required
              placeholder="e.g. EMP001"
              value={addForm.employeeCode}
              onChange={(e) => setAddForm({ ...addForm, employeeCode: e.target.value })}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          {/* Field 2: Employee Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Employee Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. John Doe"
              value={addForm.employeeName}
              onChange={(e) => setAddForm({ ...addForm, employeeName: e.target.value })}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          {/* Field 3 & 4: Telephone & Position */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Telephone *</label>
              <input
                type="text"
                required
                placeholder="0788123456"
                value={addForm.phone}
                onChange={(e) => setAddForm({ ...addForm, phone: e.target.value })}
                className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Position *</label>
              <input
                type="text"
                required
                placeholder="Machine Operator"
                value={addForm.position}
                onChange={(e) => setAddForm({ ...addForm, position: e.target.value })}
                className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Field 5 & 6: Meal Status & Amount Used */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Meal Status *</label>
              <select
                value={addForm.mealStatus}
                onChange={(e) => {
                  const status = e.target.value as 'ATE' | 'DID_NOT_EAT';
                  setAddForm({
                    ...addForm,
                    mealStatus: status,
                    amount: status === 'DID_NOT_EAT' ? 0 : 1500,
                  });
                }}
                className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
              >
                <option value="ATE">ATE</option>
                <option value="DID_NOT_EAT">DID NOT EAT</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Amount Used *</label>
              <div className="relative">
                <input
                  type="number"
                  required
                  min="0"
                  disabled={addForm.mealStatus === 'DID_NOT_EAT'}
                  placeholder="1500"
                  value={addForm.amount}
                  onChange={(e) => setAddForm({ ...addForm, amount: parseFloat(e.target.value) || 0 })}
                  className={`w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none pr-14 ${
                    addForm.mealStatus === 'DID_NOT_EAT' ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''
                  }`}
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  {summary.currency || 'RWF'}
                </span>
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t border-slate-200">
            <Button type="button" variant="outline" onClick={() => setIsAddModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={submitting}>
              Save Employee
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal: Edit Employee */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title={`Edit Employee: ${selectedEmployee?.employeeCode}`} maxWidth="md">
        <form onSubmit={handleUpdate} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Employee Name *</label>
            <input
              type="text"
              required
              value={editForm.employeeName}
              onChange={(e) => setEditForm({ ...editForm, employeeName: e.target.value })}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Telephone *</label>
              <input
                type="text"
                required
                value={editForm.phone}
                onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Position *</label>
              <input
                type="text"
                required
                value={editForm.position}
                onChange={(e) => setEditForm({ ...editForm, position: e.target.value })}
                className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Status *</label>
            <select
              value={editForm.status}
              onChange={(e) => setEditForm({ ...editForm, status: e.target.value as any })}
              className="w-full px-3.5 py-2 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-white"
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-200">
            <Button type="button" variant="outline" onClick={() => setIsEditModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={submitting}>
              Update Employee
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal: Deactivate Confirmation */}
      <Modal isOpen={isDeactivateModalOpen} onClose={() => setIsDeactivateModalOpen(false)} title="Deactivate Employee" maxWidth="sm">
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Are you sure you want to deactivate <span className="font-bold text-slate-900">{selectedEmployee?.fullName} ({selectedEmployee?.employeeCode})</span>?
          </p>
          <p className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded-lg border border-amber-200">
            Deactivated employees will no longer be eligible for new daily meal recordings. Historical records are preserved.
          </p>
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={() => setIsDeactivateModalOpen(false)}>Cancel</Button>
            <Button variant="danger" isLoading={submitting} onClick={handleDeactivate}>Deactivate</Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Employee Meal History */}
      <Modal isOpen={isHistoryModalOpen} onClose={() => setIsHistoryModalOpen(false)} title={`Meal History: ${selectedEmployee?.fullName}`} maxWidth="lg">
        <div className="space-y-3">
          {loadingHistory ? (
            <div className="space-y-2 p-4">
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-6 w-full" />
            </div>
          ) : mealHistory.length === 0 ? (
            <p className="text-center py-6 text-sm text-slate-400">No historical meal records found for this employee.</p>
          ) : (
            <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto pr-1">
              {mealHistory.map((m) => (
                <div key={m.id} className="py-2.5 flex items-center justify-between text-sm hover:bg-slate-50 px-2 rounded-lg">
                  <div>
                    <span className="font-semibold text-slate-800">{m.mealDate}</span>
                    <span className="text-xs text-slate-400 ml-2">Recorded by {m.recordedBy}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Badge variant={m.mealStatus === 'ATE' ? 'success' : 'danger'}>{m.mealStatus}</Badge>
                    <span className="font-mono font-bold text-slate-900">{m.amount} {summary.currency}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};

export default Employees;
