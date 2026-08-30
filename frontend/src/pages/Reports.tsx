import React, { useEffect, useState } from 'react';
import { reportApi } from '../api/reportApi';
import { employeeApi } from '../api/employeeApi';
import { DailyReportSummary } from '../types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { Skeleton } from '../components/common/Skeleton';
import { ToastContainer, ToastMessage } from '../components/common/Toast';
import {
  FileSpreadsheet,
  FileText,
  Calendar,
  Filter,
  Download,
  Users,
  CheckCircle2,
  XCircle,
  DollarSign,
  Calculator,
} from 'lucide-react';

export const Reports: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [selectedDept, setSelectedDept] = useState<string>('');
  const [departments, setDepartments] = useState<string[]>([]);
  const [summary, setSummary] = useState<DailyReportSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [exportingExcel, setExportingExcel] = useState(false);
  const [exportingPdf, setExportingPdf] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (type: 'success' | 'error' | 'info', message: string) => {
    setToasts((prev) => [...prev, { id: Date.now().toString(), type, message }]);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  useEffect(() => {
    fetchDepartments();
  }, []);

  useEffect(() => {
    fetchReportData();
  }, [selectedDate, selectedDept]);

  const fetchDepartments = async () => {
    try {
      const list = await employeeApi.getDepartments();
      setDepartments(list);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchReportData = async () => {
    setLoading(true);
    try {
      const data = await reportApi.getDailyReport(selectedDate, selectedDept || undefined);
      setSummary(data);
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to fetch report data');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadExcel = async () => {
    setExportingExcel(true);
    try {
      await reportApi.downloadExcel(selectedDate, selectedDept || undefined);
      addToast('success', `Daily_Meal_Report_${selectedDate}.xlsx downloaded successfully.`);
    } catch (err: any) {
      addToast('error', 'Failed to generate Excel report.');
    } finally {
      setExportingExcel(false);
    }
  };

  const handleDownloadPdf = async () => {
    setExportingPdf(true);
    try {
      await reportApi.downloadPdf(selectedDate, selectedDept || undefined);
      addToast('success', `Daily_Meal_Report_${selectedDate}.pdf downloaded successfully.`);
    } catch (err: any) {
      addToast('error', 'Failed to generate PDF report.');
    } finally {
      setExportingPdf(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Reports Engine</h2>
          <p className="text-sm text-slate-500 mt-1">Generate and export official Daily, Weekly, and Monthly meal expense reports</p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            onClick={handleDownloadExcel}
            isLoading={exportingExcel}
            className="bg-emerald-600 hover:bg-emerald-700 focus:ring-emerald-500 gap-2 shadow-emerald-600/20"
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>Download Excel</span>
          </Button>

          <Button
            onClick={handleDownloadPdf}
            isLoading={exportingPdf}
            className="bg-rose-600 hover:bg-rose-700 focus:ring-rose-500 gap-2 shadow-rose-600/20"
          >
            <FileText className="w-4 h-4" />
            <span>Download PDF</span>
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <Card className="p-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-indigo-600" />
              <label className="text-xs font-semibold text-slate-700 uppercase">Select Date:</label>
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-semibold focus:ring-2 focus:ring-indigo-500 bg-white"
              />
            </div>

            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-indigo-600" />
              <label className="text-xs font-semibold text-slate-700 uppercase">Department:</label>
              <select
                value={selectedDept}
                onChange={(e) => setSelectedDept(e.target.value)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-medium focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                <option value="">All Departments</option>
                {departments.map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
            </div>
          </div>

          <span className="text-xs text-slate-400 font-mono">
            {summary?.companyName} • {summary?.formattedReportDate}
          </span>
        </div>
      </Card>

      {/* Summary Cards */}
      {loading || !summary ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <Card className="p-4 border-l-4 border-l-sky-500">
            <p className="text-xs font-medium text-slate-500 uppercase">Total Employees</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">{summary.totalEmployees}</h3>
          </Card>
          <Card className="p-4 border-l-4 border-l-emerald-500">
            <p className="text-xs font-medium text-slate-500 uppercase">Ate</p>
            <h3 className="text-2xl font-bold text-emerald-600 mt-1">{summary.ateCount}</h3>
          </Card>
          <Card className="p-4 border-l-4 border-l-rose-500">
            <p className="text-xs font-medium text-slate-500 uppercase">Did Not Eat</p>
            <h3 className="text-2xl font-bold text-rose-600 mt-1">{summary.didNotEatCount}</h3>
          </Card>
          <Card className="p-4 border-l-4 border-l-indigo-500">
            <p className="text-xs font-medium text-slate-500 uppercase">Total Expenditure</p>
            <h3 className="text-xl font-bold text-indigo-700 mt-1">{summary.totalExpenditure.toLocaleString()} {summary.currency}</h3>
          </Card>
          <Card className="p-4 border-l-4 border-l-amber-500">
            <p className="text-xs font-medium text-slate-500 uppercase">Average Meal Cost</p>
            <h3 className="text-xl font-bold text-amber-700 mt-1">{summary.averageMealCost.toLocaleString()} {summary.currency}</h3>
          </Card>
        </div>
      )}

      {/* Transactions Table */}
      <Card title={`Detailed Meal Transactions (${selectedDate})`} className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-500 uppercase text-[11px] tracking-wider font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Code</th>
                <th className="py-3.5 px-4">Employee Name</th>
                <th className="py-3.5 px-4">Department</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4 text-right">Amount ({summary?.currency || 'RWF'})</th>
                <th className="py-3.5 px-4">Recorded By</th>
                <th className="py-3.5 px-4 text-center">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading || !summary ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={7} className="p-4"><Skeleton className="h-4 w-full" /></td>
                  </tr>
                ))
              ) : summary.records.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400 text-sm">
                    No transactions found for this date.
                  </td>
                </tr>
              ) : (
                summary.records.map((r, idx) => (
                  <tr key={idx} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-bold text-indigo-700">{r.employeeCode}</td>
                    <td className="py-3.5 px-4 font-semibold text-slate-900">{r.employeeName}</td>
                    <td className="py-3.5 px-4 text-slate-600">{r.department}</td>
                    <td className="py-3.5 px-4">
                      <Badge variant={r.mealStatus === 'ATE' ? 'success' : 'danger'}>
                        {r.mealStatus}
                      </Badge>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-900">
                      {r.amount.toLocaleString()}
                    </td>
                    <td className="py-3.5 px-4 text-slate-500 text-xs font-mono">{r.recordedBy}</td>
                    <td className="py-3.5 px-4 text-center text-slate-400 text-xs font-mono">
                      {r.createdAt ? new Date(r.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  );
};

export default Reports;

