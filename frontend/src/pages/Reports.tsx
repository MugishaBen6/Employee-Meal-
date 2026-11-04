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

/** Returns today's date as YYYY-MM-DD in the Africa/Kigali timezone (UTC+2, no DST). */
const getKigaliToday = (): string => {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Kigali' }).format(new Date());
};

/**
 * Returns true only when `value` is a non-empty, well-formed YYYY-MM-DD string
 * that also represents a real calendar date (e.g. rejects "2026-02-30").
 */
const isValidDateString = (value: string): boolean => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const d = new Date(value + 'T00:00:00'); // parse as local midnight to avoid UTC shift
  return (
    !isNaN(d.getTime()) &&
    d.getFullYear() === Number(value.slice(0, 4)) &&
    d.getMonth() + 1 === Number(value.slice(5, 7)) &&
    d.getDate() === Number(value.slice(8, 10))
  );
};

/** "2026-09-01" -> "01/09/2026" (same format as the PDF and Excel exports). */
const formatMealDate = (value?: string): string => {
  if (!value || !/^\d{4}-\d{2}-\d{2}/.test(value)) return '-';
  return `${value.slice(8, 10)}/${value.slice(5, 7)}/${value.slice(0, 4)}`;
};

/**
 * Validates the From / To period. Returns an error message, or null when valid.
 * Dates are YYYY-MM-DD strings, so plain string comparison orders them correctly.
 */
const getPeriodError = (fromDate: string, toDate: string): string | null => {
  if (!isValidDateString(fromDate)) return 'Please select a valid From Date.';
  if (!isValidDateString(toDate)) return 'Please select a valid To Date.';
  if (fromDate > toDate) return 'From Date must be on or before To Date.';
  return null;
};

export const Reports: React.FC = () => {
  // Inclusive report period. Both default to today (Kigali), i.e. a single-day report.
  const [fromDate, setFromDate] = useState<string>(getKigaliToday());
  const [toDate, setToDate] = useState<string>(getKigaliToday());
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

  // Regenerate automatically when the period or department changes. While the user is still
  // editing an invalid period, skip silently; the inline hint explains why.
  useEffect(() => {
    if (!getPeriodError(fromDate, toDate)) {
      fetchReportData();
    }
  }, [fromDate, toDate, selectedDept]);

  const periodError = getPeriodError(fromDate, toDate);

  const fetchDepartments = async () => {
    try {
      const list = await employeeApi.getDepartments();
      setDepartments(list);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchReportData = async () => {
    const error = getPeriodError(fromDate, toDate);
    if (error) {
      addToast('error', `${error} The report was not generated.`);
      return;
    }
    setLoading(true);
    try {
      const data = await reportApi.getReport(fromDate, toDate, selectedDept || undefined);
      setSummary(data);
    } catch (err: any) {
      addToast('error', err.response?.data?.message || 'Failed to fetch report data');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadExcel = async () => {
    const error = getPeriodError(fromDate, toDate);
    if (error) {
      addToast('error', `${error} Excel export was not started.`);
      return;
    }
    setExportingExcel(true);
    try {
      const filename = await reportApi.downloadExcel(fromDate, toDate, selectedDept || undefined);
      addToast('success', `${filename} downloaded successfully.`);
    } catch (err: any) {
      addToast('error', 'Failed to generate Excel report.');
    } finally {
      setExportingExcel(false);
    }
  };

  const handleDownloadPdf = async () => {
    const error = getPeriodError(fromDate, toDate);
    if (error) {
      addToast('error', `${error} PDF export was not started.`);
      return;
    }
    setExportingPdf(true);
    try {
      const filename = await reportApi.downloadPdf(fromDate, toDate, selectedDept || undefined);
      addToast('success', `${filename} downloaded successfully.`);
    } catch (err: any) {
      addToast('error', 'Failed to generate PDF report.');
    } finally {
      setExportingPdf(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  // Label of the period currently shown, e.g. "01/09/2026" or "01/09/2026 - 30/09/2026".
  const formattedDisplayDate =
    summary?.formattedReportDate || (fromDate === toDate ? fromDate : `${fromDate} - ${toDate}`);
  const isSingleDay = summary ? summary.singleDay !== false : fromDate === toDate;

  return (
    <div className="space-y-5 sm:space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4 print:hidden">
        <div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900">Reports Engine</h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">Generate and export official meal expense reports for a selected date range</p>
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
        <div className="flex flex-col gap-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-[1fr_1fr_1.15fr_auto] gap-2.5 sm:gap-3 w-full">
            <div className={`flex items-center gap-2 bg-slate-50 border rounded-xl px-3 min-h-[42px] min-w-0 focus-within:bg-white focus-within:border-indigo-400 focus-within:ring-4 focus-within:ring-indigo-500/10 transition-all ${periodError ? 'border-rose-300' : 'border-slate-200'}`}>
              <Calendar className="w-4 h-4 text-indigo-600 shrink-0" />
              <label htmlFor="report-from-date" className="text-xs font-semibold text-slate-700 uppercase whitespace-nowrap">From Date:</label>
              <input
                id="report-from-date"
                type="date"
                value={fromDate}
                max={toDate || undefined}
                onChange={(e) => setFromDate(e.target.value)}
                className="bg-transparent text-sm font-semibold text-slate-800 focus:outline-none focus:shadow-none border-0 p-0 cursor-pointer w-full min-w-0"
              />
            </div>

            <div className={`flex items-center gap-2 bg-slate-50 border rounded-xl px-3 min-h-[42px] min-w-0 focus-within:bg-white focus-within:border-indigo-400 focus-within:ring-4 focus-within:ring-indigo-500/10 transition-all ${periodError ? 'border-rose-300' : 'border-slate-200'}`}>
              <Calendar className="w-4 h-4 text-indigo-600 shrink-0" />
              <label htmlFor="report-to-date" className="text-xs font-semibold text-slate-700 uppercase whitespace-nowrap">To Date:</label>
              <input
                id="report-to-date"
                type="date"
                value={toDate}
                min={fromDate || undefined}
                onChange={(e) => setToDate(e.target.value)}
                className="bg-transparent text-sm font-semibold text-slate-800 focus:outline-none focus:shadow-none border-0 p-0 cursor-pointer w-full min-w-0"
              />
            </div>

            <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 min-h-[42px] min-w-0 focus-within:bg-white focus-within:border-indigo-400 focus-within:ring-4 focus-within:ring-indigo-500/10 transition-all">
              <Filter className="w-4 h-4 text-indigo-600 shrink-0" />
              <label className="text-xs font-semibold text-slate-700 uppercase whitespace-nowrap">Dept:</label>
              <select
                value={selectedDept}
                onChange={(e) => setSelectedDept(e.target.value)}
                className="bg-transparent text-sm font-medium text-slate-800 focus:outline-none focus:shadow-none border-0 p-0 w-full min-w-0 cursor-pointer"
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
              className="gap-1.5 text-sm min-h-[42px] sm:col-span-2 xl:col-span-1"
            >
              <Search className="w-4 h-4 shrink-0" />
              <span>Generate report</span>
            </Button>
          </div>

          <div className="self-start flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs font-medium text-slate-500 bg-slate-100/70 px-3 py-1.5 rounded-lg ring-1 ring-inset ring-slate-200">
            <span className="font-bold text-slate-800">{summary?.companyName || 'RWANDA PLASTIC INDUSTRY'}</span>
            <span>•</span>
            <span className="font-mono text-indigo-700 font-semibold">{formattedDisplayDate}</span>
          </div>
        </div>
        {periodError && (
          <p className="mt-2 text-xs font-medium text-rose-600" role="alert">{periodError}</p>
        )}
      </Card>

      {/* Official Print Header */}
      <div className="hidden print:block text-center border-b pb-4 mb-4">
        <h1 className="text-2xl font-bold text-slate-900 tracking-wide uppercase">{summary?.companyName || 'RWANDA PLASTIC INDUSTRY'}</h1>
        <h2 className="text-base font-semibold text-slate-700 mt-1 uppercase">
          {isSingleDay ? 'DAILY EMPLOYEE MEAL REPORT' : 'EMPLOYEE MEAL REPORT'}
        </h2>
        <p className="text-xs text-slate-500 font-mono mt-1">
          {isSingleDay ? 'Reporting Date' : 'Reporting Period'}: {formattedDisplayDate}
        </p>
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
        {/* Mobile: one card per record (same records as the table and the exports) */}
        <div className="md:hidden px-3.5 pb-3.5 pt-1 print:hidden">
          {loading || !summary ? (
            <div className="space-y-2.5">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-[88px] w-full rounded-2xl" />
              ))}
            </div>
          ) : summary.records.length === 0 ? (
            <div className="py-10 text-center">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100">
                <FileText className="w-6 h-6 text-slate-400" />
              </div>
              <p className="text-sm font-semibold text-slate-700">No meal records found for {formattedDisplayDate}.</p>
              <p className="mt-1 text-xs text-slate-500">
                {isSingleDay
                  ? 'Select another date or record meals for this date to see data.'
                  : 'Select another period or record meals within this period to see data.'}
              </p>
            </div>
          ) : (
            <ul className="space-y-2.5">
              {summary.records.map((r, idx) => (
                <li key={r.id ?? idx} className="rounded-2xl ring-1 ring-slate-200/80 bg-white p-3.5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-semibold text-slate-900 truncate">{r.employeeName}</p>
                      <p className="text-xs text-slate-500 truncate">
                        <span className="font-mono font-semibold text-indigo-700">{r.employeeCode}</span> · {r.position || 'Worker'} · {r.department}
                      </p>
                    </div>
                    <p className="font-mono font-bold text-sm text-slate-900 whitespace-nowrap">
                      {r.amount.toLocaleString()} {summary.currency}
                    </p>
                  </div>
                  <div className="mt-2.5 flex items-center justify-between gap-2 text-xs text-slate-500">
                    <Badge variant={r.mealStatus === 'ATE' ? 'success' : 'danger'}>{r.mealStatus}</Badge>
                    <span className="font-mono">
                      {formatMealDate(r.mealDate)}
                      {r.createdAt ? ` · ${new Date(r.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : ''}
                      {` · ${r.recordedBy}`}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="hidden md:block print:block overflow-x-auto touch-scroll">
          <table className="w-full text-left text-sm min-w-[800px]">
            <thead className="bg-slate-50 text-slate-600 uppercase text-[11px] tracking-wider font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">Date</th>
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
                    <td colSpan={9} className="p-4"><Skeleton className="h-4 w-full" /></td>
                  </tr>
                ))
              ) : summary.records.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500 text-sm">
                    <p className="font-semibold text-slate-700">No meal records found for {formattedDisplayDate}.</p>
                    <p className="text-xs text-slate-400 mt-1">
                      {isSingleDay
                        ? 'Select another date or record meals for this date to see data.'
                        : 'Select another period or record meals within this period to see data.'}
                    </p>
                  </td>
                </tr>
              ) : (
                summary.records.map((r, idx) => (
                  <tr key={r.id ?? idx} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 text-slate-600 text-xs font-mono whitespace-nowrap">{formatMealDate(r.mealDate)}</td>
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

