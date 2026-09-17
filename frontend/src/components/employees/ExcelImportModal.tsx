import React, { useState, useRef } from 'react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import {
  UploadCloud,
  FileSpreadsheet,
  Download,
  AlertCircle,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  ArrowLeft,
  ArrowRight,
  FileDown,
  Info,
  Calendar,
  Layers,
} from 'lucide-react';
import { employeeApi } from '../../api/employeeApi';
import { downloadClientExcelTemplate } from '../../utils/templateGenerator';
import { parseExcelOrCsvClient } from '../../utils/excelParser';
import {
  ExcelEmployeeRow,
  ExcelImportPreviewResponse,
  ExcelImportResultResponse,
} from '../../types';

interface ExcelImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (importedDate?: string) => void;
  defaultDate?: string;
}

type Step = 'upload' | 'preview' | 'importing' | 'result';

export const ExcelImportModal: React.FC<ExcelImportModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  defaultDate,
}) => {
  const [step, setStep] = useState<Step>('upload');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [mealDate, setMealDate] = useState<string>(
    defaultDate || new Date().toISOString().split('T')[0]
  );
  const [dragActive, setDragActive] = useState(false);
  const [previewData, setPreviewData] = useState<ExcelImportPreviewResponse | null>(null);
  const [selectedRowIndices, setSelectedRowIndices] = useState<Set<number>>(new Set());
  const [filterType, setFilterType] = useState<'ALL' | 'VALID' | 'INVALID' | 'DUPLICATE'>('ALL');
  const [importResult, setImportResult] = useState<ExcelImportResultResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const resetState = () => {
    setStep('upload');
    setSelectedFile(null);
    setPreviewData(null);
    setSelectedRowIndices(new Set());
    setFilterType('ALL');
    setImportResult(null);
    setLoading(false);
    setErrorMessage(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleClose = () => {
    resetState();
    onClose();
  };

  const handleDownloadTemplate = () => {
    setErrorMessage(null);
    try {
      downloadClientExcelTemplate();
    } catch (e: any) {
      setErrorMessage('Failed to generate Excel template');
    }
  };

  const handleFileChange = (file: File) => {
    const validExtensions = ['.xlsx', '.xls'];
    const hasValidExt = validExtensions.some((ext) =>
      file.name.toLowerCase().endsWith(ext)
    );

    if (!hasValidExt) {
      setErrorMessage('Please upload a valid Excel file (.xlsx or .xls)');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setErrorMessage('File size exceeds maximum limit of 10MB');
      return;
    }

    setErrorMessage(null);
    setSelectedFile(file);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileChange(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
  };

  const handlePreview = async () => {
    if (!selectedFile) {
      setErrorMessage('Please select an Excel file to preview');
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      let data: ExcelImportPreviewResponse | null = null;
      try {
        // Try server-side POI parser first for precise date and employee matching
        data = await employeeApi.previewExcelImport(selectedFile);
      } catch (serverErr) {
        console.warn('Server parsing error, attempting client parsing fallback:', serverErr);
        data = await parseExcelOrCsvClient(selectedFile);
      }

      if (!data || !data.rows || data.rows.length === 0) {
        throw new Error('No employee records found in the uploaded file.');
      }

      setPreviewData(data);
      // Auto-select all valid rows
      const validIndices = new Set<number>();
      data.rows.forEach((r, idx) => {
        const isRowValid = r.valid === true || r.status === 'VALID';
        if (isRowValid) {
          validIndices.add(idx);
        }
      });
      setSelectedRowIndices(validIndices);
      setStep('preview');
    } catch (err: any) {
      setErrorMessage(
        err.message || err.response?.data?.message || 'Failed to parse and validate Excel file'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleToggleRow = (index: number) => {
    const updated = new Set(selectedRowIndices);
    if (updated.has(index)) {
      updated.delete(index);
    } else {
      updated.add(index);
    }
    setSelectedRowIndices(updated);
  };

  const handleSelectAllValid = (selectAll: boolean) => {
    if (!previewData) return;
    const updated = new Set<number>();
    if (selectAll) {
      previewData.rows.forEach((r, idx) => {
        const isRowValid = r.valid === true || r.status === 'VALID';
        if (isRowValid) updated.add(idx);
      });
    }
    setSelectedRowIndices(updated);
  };

  const handleConfirmImport = async () => {
    if (!previewData) return;

    const rowsToImport = previewData.rows.filter((_, idx) =>
      selectedRowIndices.has(idx)
    );

    if (rowsToImport.length === 0) {
      setErrorMessage('No valid rows selected for import');
      return;
    }

    setStep('importing');
    setLoading(true);
    setErrorMessage(null);

    try {
      const rawRes: any = await employeeApi.confirmExcelImport({
        mealDate,
        rows: rowsToImport,
      });

      const successCount = rawRes.mealRecordsCreated ?? rawRes.importedCount ?? rawRes.successCount ?? rowsToImport.length;
      const employeesProcessed = rawRes.employeesProcessed ?? (new Set(rowsToImport.map(r => r.employeeName))).size;
      const ateCount = rawRes.ateCount ?? rowsToImport.filter(r => r.mealStatus?.toUpperCase() === 'ATE' || r.mealStatus?.toLowerCase() === 'ate').length;
      const didNotEatCount = rawRes.didNotEatCount ?? (successCount - ateCount);

      const result: ExcelImportResultResponse = {
        totalProcessed: rowsToImport.length,
        successCount,
        importedCount: successCount,
        employeesProcessed,
        mealRecordsCreated: successCount,
        ateCount,
        didNotEatCount,
        duplicateCount: rawRes.duplicateCount ?? 0,
        invalidCount: rawRes.invalidCount ?? 0,
        errorCount: rawRes.errorCount ?? 0,
        importedEmployeeIds: rawRes.importedEmployeeIds || [],
        errorRows: rawRes.errorRows || rawRes.failedRows || [],
        message: rawRes.message || `Import Completed: ${employeesProcessed} employee(s) processed, ${successCount} meal record(s) created (${ateCount} ATE, ${didNotEatCount} DID NOT EAT).`,
      };

      setImportResult(result);
      setStep('result');
    } catch (err: any) {
      setErrorMessage(
        err.response?.data?.message || err.message || 'Import failed. Please check your data.'
      );
      setStep('preview');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadErrorReport = async () => {
    const rowsWithErrors =
      importResult?.errorRows && importResult.errorRows.length > 0
        ? importResult.errorRows
        : previewData?.rows.filter((r) => {
            const isRowValid = r.valid === true || r.status === 'VALID';
            const isRowDuplicate = r.duplicate === true || r.status === 'DUPLICATE';
            return !isRowValid || isRowDuplicate;
          }) || [];

    if (rowsWithErrors.length === 0) {
      setErrorMessage('No error records to export');
      return;
    }

    try {
      const blob = await employeeApi.downloadErrorReport(rowsWithErrors);
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'employee_import_error_report.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (e: any) {
      setErrorMessage('Failed to generate error report');
    }
  };

  const filteredPreviewRows =
    previewData?.rows
      .map((row, originalIndex) => ({ row, originalIndex }))
      .filter(({ row }) => {
        const isRowValid = row.valid === true || row.status === 'VALID';
        const isRowDuplicate = row.duplicate === true || row.status === 'DUPLICATE';
        if (filterType === 'VALID') return isRowValid;
        if (filterType === 'INVALID') return !isRowValid;
        if (filterType === 'DUPLICATE') return isRowDuplicate;
        return true;
      }) || [];

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="📥 Import Employees & Attendance from Excel"
      maxWidth="5xl"
    >
      <div className="space-y-5">
        {errorMessage && (
          <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs sm:text-sm flex items-start gap-2.5 animate-fadeIn">
            <AlertCircle className="w-5 h-5 shrink-0 text-rose-600 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold">Import Alert</p>
              <p className="mt-0.5">{errorMessage}</p>
            </div>
          </div>
        )}

        {/* Step Indicator */}
        <div className="flex items-center justify-between px-2 sm:px-6 py-2 bg-slate-50 border border-slate-200/80 rounded-xl text-xs font-semibold text-slate-500">
          <div
            className={`flex items-center gap-1.5 sm:gap-2 ${
              step === 'upload' ? 'text-indigo-600 font-bold' : ''
            }`}
          >
            <span
              className={`w-6 h-6 rounded-full flex items-center justify-center text-xs ${
                step === 'upload'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'bg-slate-200 text-slate-700'
              }`}
            >
              1
            </span>
            <span>Upload File</span>
          </div>
          <ArrowRight className="w-4 h-4 text-slate-300" />
          <div
            className={`flex items-center gap-1.5 sm:gap-2 ${
              step === 'preview' ? 'text-indigo-600 font-bold' : ''
            }`}
          >
            <span
              className={`w-6 h-6 rounded-full flex items-center justify-center text-xs ${
                step === 'preview'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'bg-slate-200 text-slate-700'
              }`}
            >
              2
            </span>
            <span>Preview & Validate</span>
          </div>
          <ArrowRight className="w-4 h-4 text-slate-300" />
          <div
            className={`flex items-center gap-1.5 sm:gap-2 ${
              step === 'result' ? 'text-indigo-600 font-bold' : ''
            }`}
          >
            <span
              className={`w-6 h-6 rounded-full flex items-center justify-center text-xs ${
                step === 'result'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'bg-slate-200 text-slate-700'
              }`}
            >
              3
            </span>
            <span>Summary</span>
          </div>
        </div>

        {/* STEP 1: UPLOAD */}
        {step === 'upload' && (
          <div className="space-y-4">
            {/* Target Attendance Date & Template Link */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4 bg-slate-50/70 border border-slate-100 rounded-xl">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                  <Calendar className="w-4 h-4 text-indigo-600" />
                  Target Attendance Date
                </label>
                <input
                  type="date"
                  value={mealDate}
                  onChange={(e) => setMealDate(e.target.value)}
                  className="w-full text-sm font-medium border border-slate-300 rounded-xl px-3 py-2 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all shadow-xs"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Imported meal attendance will be recorded for this date.
                </p>
              </div>

              <div className="flex flex-col justify-between p-3 bg-white border border-indigo-100 rounded-xl">
                <div>
                  <h4 className="text-xs font-bold text-indigo-900 flex items-center gap-1.5">
                    <FileSpreadsheet className="w-4 h-4 text-indigo-600" />
                    Standard Excel Template
                  </h4>
                  <p className="text-[11px] text-slate-500 mt-1">
                    Columns: Employee Name, Telephone, Position, Meal Status, Amount Used
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleDownloadTemplate}
                  className="mt-2.5 text-xs text-indigo-700 border-indigo-200 hover:bg-indigo-50 self-start"
                >
                  <Download className="w-3.5 h-3.5 mr-1.5" />
                  Download Sample Template (.xlsx)
                </Button>
              </div>
            </div>

            {/* Drag & Drop Dropzone */}
            <div
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onClick={() => fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-2xl p-6 sm:p-8 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center gap-3 ${
                dragActive
                  ? 'border-indigo-500 bg-indigo-50/70 scale-[1.01]'
                  : selectedFile
                  ? 'border-emerald-400 bg-emerald-50/40'
                  : 'border-slate-300 hover:border-indigo-400 hover:bg-slate-50/80 bg-slate-50/40'
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx, .xls"
                className="hidden"
                onChange={(e) => {
                  if (e.target.files && e.target.files[0]) {
                    handleFileChange(e.target.files[0]);
                  }
                }}
              />

              {selectedFile ? (
                <>
                  <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center shadow-xs">
                    <FileSpreadsheet className="w-6 h-6" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-slate-800">{selectedFile.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {(selectedFile.size / 1024).toFixed(1)} KB &bull; Ready for validation
                    </p>
                  </div>
                  <span className="text-xs text-indigo-600 font-semibold underline mt-1">
                    Click or drag another file to replace
                  </span>
                </>
              ) : (
                <>
                  <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                    <UploadCloud className="w-6 h-6" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-slate-800">
                      Choose an Excel file or drag & drop here
                    </p>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Supports .xlsx and .xls up to 10MB
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="mt-1 pointer-events-none"
                  >
                    Browse Files
                  </Button>
                </>
              )}
            </div>

            {/* Note & Actions */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
              <p className="text-xs text-slate-400 flex items-center gap-1.5 self-start sm:self-center">
                <Info className="w-4 h-4 text-slate-400 shrink-0" />
                Employee IDs (EMP001, EMP002...) will be automatically generated.
              </p>

              <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">
                <Button type="button" variant="outline" onClick={handleClose}>
                  Cancel
                </Button>
                <Button
                  type="button"
                  variant="primary"
                  disabled={!selectedFile || loading}
                  onClick={handlePreview}
                >
                  {loading ? (
                    <>
                      <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                      Validating File...
                    </>
                  ) : (
                    <>
                      Preview & Validate
                      <ArrowRight className="w-4 h-4 ml-2" />
                    </>
                  )}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* STEP 2: PREVIEW & VALIDATE */}
        {step === 'preview' && previewData && (
          <div className="space-y-4">
            {/* Validation Statistics Banner */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
                  Total Read
                </span>
                <span className="text-xl font-bold text-slate-900 mt-0.5 block">
                  {previewData.totalRows}
                </span>
              </div>

              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl">
                <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider block">
                  Valid to Import
                </span>
                <span className="text-xl font-bold text-emerald-700 mt-0.5 block">
                  {previewData.validRows}
                </span>
              </div>

              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl">
                <span className="text-[10px] font-bold text-amber-700 uppercase tracking-wider block">
                  Duplicates
                </span>
                <span className="text-xl font-bold text-amber-700 mt-0.5 block">
                  {previewData.duplicateRows}
                </span>
              </div>

              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl">
                <span className="text-[10px] font-bold text-rose-700 uppercase tracking-wider block">
                  Errors / Invalid
                </span>
                <span className="text-xl font-bold text-rose-700 mt-0.5 block">
                  {previewData.invalidRows}
                </span>
              </div>
            </div>

            {/* Filter Pills & Select All */}
            <div className="flex flex-wrap items-center justify-between gap-2.5 pt-1">
              <div className="flex items-center gap-1.5 overflow-x-auto">
                <button
                  type="button"
                  onClick={() => setFilterType('ALL')}
                  className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                    filterType === 'ALL'
                      ? 'bg-slate-900 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  All ({previewData.totalRows})
                </button>
                <button
                  type="button"
                  onClick={() => setFilterType('VALID')}
                  className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                    filterType === 'VALID'
                      ? 'bg-emerald-600 text-white'
                      : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                  }`}
                >
                  Valid ({previewData.validRows})
                </button>
                {previewData.duplicateRows > 0 && (
                  <button
                    type="button"
                    onClick={() => setFilterType('DUPLICATE')}
                    className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                      filterType === 'DUPLICATE'
                        ? 'bg-amber-600 text-white'
                        : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
                    }`}
                  >
                    Duplicates ({previewData.duplicateRows})
                  </button>
                )}
                {previewData.invalidRows > 0 && (
                  <button
                    type="button"
                    onClick={() => setFilterType('INVALID')}
                    className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                      filterType === 'INVALID'
                        ? 'bg-rose-600 text-white'
                        : 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                    }`}
                  >
                    Invalid ({previewData.invalidRows})
                  </button>
                )}
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleSelectAllValid(true)}
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold underline cursor-pointer"
                >
                  Select All Valid
                </button>
                <span className="text-slate-300">|</span>
                <button
                  type="button"
                  onClick={() => handleSelectAllValid(false)}
                  className="text-xs text-slate-500 hover:text-slate-700 font-semibold underline cursor-pointer"
                >
                  Deselect All
                </button>
              </div>
            </div>

            {/* Interactive Preview Table */}
            {/* Interactive Preview Table */}
            <div className="border border-slate-200 rounded-xl overflow-hidden shadow-xs">
              <div className="max-h-72 overflow-y-auto overflow-x-auto touch-scroll">
                <table className="w-full text-left text-xs text-slate-700 divide-y divide-slate-200">
                  <thead className="bg-slate-50 text-slate-600 font-bold sticky top-0 z-10">
                    <tr>
                      <th className="p-2.5 w-10 text-center">
                        <input
                          type="checkbox"
                          checked={
                            previewData.validRows > 0 &&
                            selectedRowIndices.size === previewData.validRows
                          }
                          onChange={(e) => handleSelectAllValid(e.target.checked)}
                          className="rounded text-indigo-600 focus:ring-indigo-500 w-4 h-4 cursor-pointer"
                        />
                      </th>
                      <th className="p-2.5 w-12">#</th>
                      <th className="p-2.5 min-w-[140px]">Employee Name</th>
                      <th className="p-2.5 min-w-[100px]">Position</th>
                      <th className="p-2.5 min-w-[100px]">Date</th>
                      <th className="p-2.5 min-w-[100px]">Meal Status</th>
                      <th className="p-2.5 min-w-[90px]">Amount Used</th>
                      <th className="p-2.5 min-w-[120px]">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {filteredPreviewRows.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="p-6 text-center text-slate-400">
                          No records match the active filter.
                        </td>
                      </tr>
                    ) : (
                      filteredPreviewRows.map(({ row, originalIndex }) => {
                        const isSelected = selectedRowIndices.has(originalIndex);
                        const isRowValid = row.valid === true || row.status === 'VALID';
                        const isRowDuplicate = row.duplicate === true || row.status === 'DUPLICATE';

                        return (
                          <tr
                            key={originalIndex}
                            className={`transition-colors ${
                              !isRowValid
                                ? 'bg-rose-50/40 text-rose-900'
                                : isRowDuplicate
                                ? 'bg-amber-50/30 text-amber-900'
                                : isSelected
                                ? 'bg-indigo-50/30'
                                : 'hover:bg-slate-50/60'
                            }`}
                          >
                            <td className="p-2.5 text-center">
                              <input
                                type="checkbox"
                                checked={isSelected}
                                disabled={!isRowValid}
                                onChange={() => handleToggleRow(originalIndex)}
                                className={`rounded text-indigo-600 focus:ring-indigo-500 w-4 h-4 ${
                                  !isRowValid
                                    ? 'opacity-30 cursor-not-allowed'
                                    : 'cursor-pointer'
                                }`}
                              />
                            </td>
                            <td className="p-2.5 font-mono text-slate-400">
                              {row.rowNumber}
                            </td>
                            <td className="p-2.5 font-semibold text-slate-900">
                              {row.employeeName || (
                                <span className="text-rose-400 italic">Missing</span>
                              )}
                            </td>
                            <td className="p-2.5 text-slate-600">
                              {row.position || 'Worker'}
                            </td>
                            <td className="p-2.5 font-mono text-slate-600 text-[11px]">
                              {row.mealDate || '—'}
                            </td>
                            <td className="p-2.5">
                              {row.mealStatus?.toUpperCase() === 'ATE' ? (
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                                  ATE
                                </span>
                              ) : row.mealStatus?.toUpperCase() === 'DID_NOT_EAT' || row.mealStatus?.toLowerCase() === 'not ate' ? (
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
                                  DID NOT EAT
                                </span>
                              ) : (
                                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-200 text-slate-700">
                                  {row.mealStatus || 'DID NOT EAT'}
                                </span>
                              )}
                            </td>
                            <td className="p-2.5 font-semibold">
                              {row.amountUsed !== null && row.amountUsed !== undefined
                                ? `${row.amountUsed.toLocaleString()} RWF`
                                : '0 RWF'}
                            </td>
                            <td className="p-2.5">
                              {isRowValid ? (
                                <div className="flex items-center gap-1.5 text-emerald-700 font-medium">
                                  <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                                  <span>Ready</span>
                                </div>
                              ) : (
                                <div className="space-y-0.5">
                                  {row.errorReason ? (
                                    <div className="flex items-center gap-1 text-[11px] text-rose-600">
                                      <XCircle className="w-3 h-3 shrink-0" />
                                      <span className="truncate max-w-[200px]" title={row.errorReason}>
                                        {row.errorReason}
                                      </span>
                                    </div>
                                  ) : (
                                    <div className="flex items-center gap-1 text-[11px] text-rose-600">
                                      <XCircle className="w-3 h-3 shrink-0" />
                                      <span>Invalid record</span>
                                    </div>
                                  )}
                                </div>
                              )}
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Bottom Actions */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setStep('upload')}
                >
                  <ArrowLeft className="w-3.5 h-3.5 mr-1.5" />
                  Back
                </Button>

                {(previewData.invalidRows > 0 || previewData.duplicateRows > 0) && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleDownloadErrorReport}
                    className="text-rose-700 border-rose-200 hover:bg-rose-50"
                  >
                    <FileDown className="w-3.5 h-3.5 mr-1.5 text-rose-600" />
                    Download Error Report (.xlsx)
                  </Button>
                )}
              </div>

              <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                <Button type="button" variant="outline" onClick={handleClose}>
                  Cancel
                </Button>
                <Button
                  type="button"
                  variant="primary"
                  disabled={selectedRowIndices.size === 0 || loading}
                  onClick={handleConfirmImport}
                >
                  {loading ? (
                    <>
                      <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                      Importing...
                    </>
                  ) : (
                    <>
                      Import {selectedRowIndices.size} Meal Record(s)
                      <ArrowRight className="w-4 h-4 ml-2" />
                    </>
                  )}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* STEP: IMPORTING PROGRESS */}
        {step === 'importing' && (
          <div className="py-12 flex flex-col items-center justify-center text-center space-y-4">
            <div className="relative">
              <div className="w-16 h-16 rounded-full border-4 border-indigo-100 border-t-indigo-600 animate-spin" />
              <Layers className="w-6 h-6 text-indigo-600 absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900">
                Importing Daily Meal Records...
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm">
                Processing daily meal records, mapping employee positions, and saving attendance data.
              </p>
            </div>
          </div>
        )}

        {/* STEP 3: RESULT SUMMARY */}
        {step === 'result' && importResult && (
          <div className="space-y-4 animate-fadeIn">
            <div
              className={`p-6 border rounded-2xl text-center space-y-3 ${
                (importResult.successCount ?? 0) > 0 || (importResult.mealRecordsCreated ?? 0) > 0
                  ? 'bg-slate-50 border-slate-200/80'
                  : 'bg-rose-50/50 border-rose-200'
              }`}
            >
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center mx-auto shadow-xs ${
                  (importResult.successCount ?? 0) > 0 || (importResult.mealRecordsCreated ?? 0) > 0
                    ? 'bg-emerald-100 text-emerald-600'
                    : 'bg-rose-100 text-rose-600'
                }`}
              >
                {(importResult.successCount ?? 0) > 0 || (importResult.mealRecordsCreated ?? 0) > 0 ? (
                  <CheckCircle2 className="w-8 h-8" />
                ) : (
                  <XCircle className="w-8 h-8" />
                )}
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {(importResult.successCount ?? 0) > 0 || (importResult.mealRecordsCreated ?? 0) > 0
                    ? 'Import Completed!'
                    : 'Import Failed — No Records Saved'}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  {importResult.message}
                </p>
              </div>

              {/* Exact Stat Counters */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 max-w-2xl mx-auto pt-2">
                <div className="p-3 bg-white border border-slate-200 rounded-xl shadow-xs">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
                    Staff Processed
                  </span>
                  <span className="text-xl font-bold text-slate-900">
                    {importResult.employeesProcessed ?? importResult.totalProcessed}
                  </span>
                </div>
                <div className="p-3 bg-indigo-50 border border-indigo-200 rounded-xl shadow-xs">
                  <span className="text-[10px] font-bold text-indigo-700 uppercase tracking-wider block">
                    Records Created
                  </span>
                  <span className="text-xl font-bold text-indigo-700">
                    {importResult.mealRecordsCreated ?? importResult.importedCount ?? importResult.successCount}
                  </span>
                </div>
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl shadow-xs">
                  <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider block">
                    ATE Records
                  </span>
                  <span className="text-xl font-bold text-emerald-700">
                    {importResult.ateCount ?? 0}
                  </span>
                </div>
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl shadow-xs">
                  <span className="text-[10px] font-bold text-rose-700 uppercase tracking-wider block">
                    DID NOT EAT
                  </span>
                  <span className="text-xl font-bold text-rose-700">
                    {importResult.didNotEatCount ?? 0}
                  </span>
                </div>
              </div>
            </div>

            {/* Detailed Failed Rows List if any errors */}
            {importResult.errorRows && importResult.errorRows.length > 0 && (
              <div className="border border-rose-200 bg-rose-50/30 rounded-xl p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-rose-800 uppercase tracking-wider">
                    Error Details ({importResult.errorRows.length} failed)
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleDownloadErrorReport}
                    className="text-xs text-rose-800 border-rose-300 hover:bg-rose-100 shrink-0"
                  >
                    <FileDown className="w-3.5 h-3.5 mr-1" />
                    Download Error Excel
                  </Button>
                </div>
                <div className="max-h-40 overflow-y-auto divide-y divide-rose-100 text-xs text-rose-900 bg-white rounded-lg border border-rose-100 p-2">
                  {importResult.errorRows.map((r, i) => (
                    <div key={i} className="py-1.5 flex items-center justify-between gap-2">
                      <span className="font-semibold">{r.employeeName || `Row ${r.rowNumber}`}</span>
                      <span className="text-rose-600 text-[11px] truncate max-w-[300px]">
                        {r.errorReason || 'Server rejected creation'}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex items-center justify-between pt-2">
              {(importResult.successCount ?? 0) === 0 ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setStep('preview')}
                >
                  <ArrowLeft className="w-4 h-4 mr-1.5" />
                  Back to Preview & Retry
                </Button>
              ) : (
                <div />
              )}

              <Button
                type="button"
                variant="primary"
                onClick={() => {
                  const primaryDate = previewData?.rows?.find((r) => r.mealDate)?.mealDate || mealDate;
                  handleClose();
                  if ((importResult.successCount ?? 0) > 0) {
                    onSuccess(primaryDate);
                  }
                }}
              >
                {(importResult.successCount ?? 0) > 0
                  ? 'Done & Refresh Attendance List'
                  : 'Close'}
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default ExcelImportModal;
