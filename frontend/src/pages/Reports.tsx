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
  Printer,
  RefreshCw,
  Search,
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

  const handlePrint = () => {
    window.print();
  };

  const formattedDisplayDate = summary?.formattedReportDate || selectedDate;

  return (
    <div className="space-y-5 sm:space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4 print:hidden">
        <div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900">Reports Engine</h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">Generate and export official date-specific meal expense reports</p>
        </div>

        <div className="grid grid-cols-3 sm:flex items-center gap-2 sm:gap-3 w-full sm:w-auto">
          <Button
            onClick={handlePrint}
            size="sm"
            variant="outline"
            className="border-slate-300 text-slate-700 hover:bg-slate-100 gap-1.5 sm:gap-2 text-xs sm:text-sm min-h-[38px]"
          >
            <Printer className="w-4 h-4 shrink-0 text-slate-600" />
            <span className="hidden sm:inline">Print Report</span>
            <span className="sm:hidden">Print</span>
          </Button>

          <Button
            onClick={handleDownloadExcel}
            isLoading={exportingExcel}
            size="sm"
            className="bg-emerald-600 hover:bg-emerald-700 focus:ring-emerald-500 gap-1.5 sm:gap-2 shadow-emerald-600/20 text-xs sm:text-sm min-h-[38px]"
          >
            <FileSpreadsheet className="w-4 h-4 shrink-0" />
            <span className="hidden sm:inline">Download Excel</span>
            <span className="sm:hidden">Excel</span>
          </Button>

          <Button
            onClick={handleDownloadPdf}
            isLoading={exportingPdf}
            size="sm"
            className="bg-rose-600 hover:bg-rose-700 focus:ring-rose-500 gap-1.5 sm:gap-2 shadow-rose-600/20 text-xs sm:text-sm min-h-[38px]"
          >
            <FileText className="w-4 h-4 shrink-0" />
            <span className="hidden sm:inline">Download PDF</span>
            <span className="sm:hidden">PDF</span>
          </Button>
        </div>
      </div>

      {/* Filter & Generation Bar */}
      <Card className="p-3.5 sm:p-4 print:hidden">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div className="flex flex-col sm:flex-row sm:items-center gap-2.5 sm:gap-3 w-full lg:w-auto">
            <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5">
              <Calendar className="w-4 h-4 text-indigo-600 shrink-0" />
              <label className="text-xs font-semibold text-slate-700 uppercase whitespace-nowrap">Date:</label>
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="bg-transparent text-xs sm:text-sm font-semibold text-slate-800 focus:outline-none cursor-pointer w-full"
              />
            </div>

            <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5">
              <Filter className="w-4 h-4 text-indigo-600 shrink-0" />
              <label className="text-xs font-semibold text-slate-700 uppercase whitespace-nowrap">Dept:</label>
              <select
                value={selectedDept}
                onChange={(e) => setSelectedDept(e.target.value)}
                className="bg-transparent text-xs sm:text-sm font-medium text-slate-800 focus:outline-none w-full"
              >
                <option value="">All Departments</option>
                {departments.map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
            </div>

            <Button
              onClick={fetchReportData}
              isLoading={loading}
              size="sm"
              className="bg-indigo-600 hover:bg-indigo-700 focus:ring-indigo-500 gap-1.5 text-xs sm:text-sm min-h-[38px] font-semibold"
            >
              <Search className="w-4 h-4 shrink-0" />
              <span>GENERATE REPORT</span>
            </Button>
          </div>

          <div className="flex items-center gap-2 text-xs font-medium text-slate-500 bg-slate-100/70 px-3 py-1.5 rounded-lg border border-slate-200">
            <span className="font-bold text-slate-800">{summary?.companyName || 'RWANDA PLASTIC INDUSTRY'}</span>
            <span>•</span>
            <span className="font-mono text-indigo-700 font-semibold">{formattedDisplayDate}</span>
          </div>
        </div>
      </Card>

      {/* Official Print Header */}
      <div className="hidden print:block text-center border-b pb-4 mb-4">
        <h1 className="text-2xl font-bold text-slate-900 tracking-wide uppercase">{summary?.companyName || 'RWANDA PLASTIC INDUSTRY'}</h1>
        <h2 className="text-base font-semibold text-slate-700 mt-1 uppercase">DAILY EMPLOYEE MEAL REPORT</h2>
        <p className="text-xs text-slate-500 font-mono mt-1">Reporting Date: {formattedDisplayDate}</p>
      </div>

      {/* Summary Cards */}
      {loading || !summary ? (
        <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
          <Skeleton className="h-24 sm:h-28 rounded-xl" />
          <Skeleton className="h-24 sm:h-28 rounded-xl" />
          <Skeleton className="h-24 sm:h-28 rounded-xl" />
          <Skeleton className="h-24 sm:h-28 rounded-xl" />
          <Skeleton className="h-24 sm:h-28 rounded-xl col-span-2 sm:col-span-2 lg:col-span-1" />
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
          <Card className="p-3.5 sm:p-4 border-l-4 border-l-sky-500 shadow-sm">
            <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase">Total Records ({formattedDisplayDate})</p>
            <h3 className="text-lg sm:text-2xl font-bold text-slate-900 mt-0.5 sm:mt-1">{summary.records.length}</h3>
          </Card>
          <Card className="p-3.5 sm:p-4 border-l-4 border-l-emerald-500 shadow-sm">
            <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase">Ate</p>
            <h3 className="text-lg sm:text-2xl font-bold text-emerald-600 mt-0.5 sm:mt-1">{summary.ateCount}</h3>
          </Card>
          <Card className="p-3.5 sm:p-4 border-l-4 border-l-rose-500 shadow-sm">
            <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase">Did Not Eat</p>
            <h3 className="text-lg sm:text-2xl font-bold text-rose-600 mt-0.5 sm:mt-1">{summary.didNotEatCount}</h3>
          </Card>
          <Card className="p-3.5 sm:p-4 border-l-4 border-l-indigo-500 col-span-2 sm:col-span-1 shadow-sm">
            <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase">Total Expenditure</p>
            <h3 className="text-base sm:text-xl font-bold text-indigo-700 mt-0.5 sm:mt-1 truncate">{summary.totalExpenditure.toLocaleString()} {summary.currency}</h3>
          </Card>
          <Card className="p-3.5 sm:p-4 border-l-4 border-l-amber-500 col-span-2 sm:col-span-1 shadow-sm">
            <p className="text-[11px] sm:text-xs font-semibold text-slate-500 uppercase">Average Meal Cost</p>
            <h3 className="text-base sm:text-xl font-bold text-amber-700 mt-0.5 sm:mt-1 truncate">{summary.averageMealCost.toLocaleString()} {summary.currency}</h3>
          </Card>
        </div>
      )}

      {/* Transactions Table */}
      <Card title={`Detailed Meal Transactions — ${formattedDisplayDate}`} className="p-0 overflow-hidden shadow-sm">
        <div className="overflow-x-auto touch-scroll">
          <table className="w-full text-left text-sm min-w-[720px]">
            <thead className="bg-slate-50 text-slate-600 uppercase text-[11px] tracking-wider font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Code</th>
                <th className="py-3.5 px-4">Employee Name</th>
                <th className="py-3.5 px-4">Position</th>
                <th className="py-3.5 px-4">Department</th>
                <th className="py-3.5 px-4 text-center">Status</th>
                <th className="py-3.5 px-4 text-right">Amount ({summary?.currency || 'RWF'})</th>
                <th className="py-3.5 px-4">Recorded By</th>
                <th className="py-3.5 px-4 text-center">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading || !summary ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={8} className="p-4"><Skeleton className="h-4 w-full" /></td>
                  </tr>
                ))
              ) : summary.records.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500 text-sm">
                    <p className="font-semibold text-slate-700">No meal records found for {formattedDisplayDate}.</p>
                    <p className="text-xs text-slate-400 mt-1">Select another date or record meals for this date to see data.</p>
                  </td>
                </tr>
              ) : (
                summary.records.map((r, idx) => (
                  <tr key={idx} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-bold text-indigo-700">{r.employeeCode}</td>
                    <td className="py-3.5 px-4 font-semibold text-slate-900">{r.employeeName}</td>
                    <td className="py-3.5 px-4 text-slate-700 font-medium text-xs">{r.position || 'Worker'}</td>
                    <td className="py-3.5 px-4 text-slate-600 text-xs">{r.department}</td>
                    <td className="py-3.5 px-4 text-center">
                      <Badge variant={r.mealStatus === 'ATE' ? 'success' : 'danger'}>
                        {r.mealStatus}
                      </Badge>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-900">
                      {r.amount.toLocaleString()} {summary.currency}
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

