import React, { useState } from 'react';
import { AlertTriangle, DatabaseBackup, Download, Loader2, Search, Trash2, CheckCircle2 } from 'lucide-react';
import { mealApi, MealDataSummary } from '../../api/mealApi';
import { Card } from '../common/Card';
import { Button } from '../common/Button';

type Scope = 'all' | 'range';

const fmtDay = (iso: string | null) => {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

/**
 * ADMIN clean-start tool. Three explicit steps: check what is in scope, download a full backup,
 * then type DELETE. The server re-checks the count, so a stale screen cannot delete more.
 */
export const MealDataMaintenance: React.FC<{ onToast: (type: 'success' | 'error' | 'info', msg: string) => void }> = ({ onToast }) => {
  const [scope, setScope] = useState<Scope>('all');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [summary, setSummary] = useState<MealDataSummary | null>(null);
  const [backupFile, setBackupFile] = useState<string | null>(null);
  const [confirmText, setConfirmText] = useState('');
  const [busy, setBusy] = useState<'check' | 'backup' | 'delete' | null>(null);
  const [lastResult, setLastResult] = useState<string | null>(null);

  const range = scope === 'range' ? { startDate, endDate } : undefined;
  const rangeInvalid = scope === 'range' && (!startDate || !endDate || startDate > endDate);

  const reset = () => {
    setSummary(null);
    setBackupFile(null);
    setConfirmText('');
  };

  const check = async () => {
    setBusy('check');
    setLastResult(null);
    try {
      setSummary(await mealApi.maintenanceSummary(range));
      setBackupFile(null);
      setConfirmText('');
    } catch (e: any) {
      onToast('error', e.response?.data?.message || 'Could not load the meal record summary');
    } finally {
      setBusy(null);
    }
  };

  const backup = async () => {
    setBusy('backup');
    try {
      const name = await mealApi.downloadMaintenanceBackup(range);
      setBackupFile(name);
      onToast('success', `Backup downloaded: ${name}`);
    } catch (e: any) {
      onToast('error', 'Backup download failed. Nothing was deleted.');
    } finally {
      setBusy(null);
    }
  };

  const remove = async () => {
    if (!summary) return;
    setBusy('delete');
    try {
      const res = await mealApi.maintenanceDelete(summary.totalRecords, range);
      setLastResult(
        `${res.data.deletedRecords.toLocaleString()} meal record(s) deleted (${Number(res.data.totalAmount).toLocaleString()} RWF). Backup: ${backupFile}`
      );
      onToast('success', res.message);
      reset();
    } catch (e: any) {
      onToast('error', e.response?.data?.message || 'Delete failed. Nothing was deleted.');
    } finally {
      setBusy(null);
    }
  };

  const canDelete = !!summary && summary.totalRecords > 0 && !!backupFile && confirmText === 'DELETE' && busy === null;

  return (
    <Card className="p-0 overflow-hidden ring-1 ring-rose-100">
      <div className="flex items-start gap-3 border-b border-rose-100 bg-rose-50/60 px-5 sm:px-6 py-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-rose-600">
          <DatabaseBackup className="h-5 w-5" />
        </div>
        <div>
          <h2 className="text-base font-bold text-slate-900">Meal data maintenance</h2>
          <p className="text-xs text-slate-600 mt-0.5">
            Clear meal records before re-importing clean data. Employees, users and audit logs are not touched.
            Every step is recorded in the Activity Logs.
          </p>
        </div>
      </div>

      <div className="px-5 sm:px-6 py-5 space-y-5">
        {/* Step 1: scope */}
        <div className="space-y-3">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-500">1 · Choose records and check</p>
          <div className="flex flex-wrap gap-2">
            {(['all', 'range'] as Scope[]).map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => {
                  setScope(s);
                  reset();
                }}
                className={`h-9 rounded-xl px-3.5 text-sm font-semibold transition-colors ${
                  scope === s ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {s === 'all' ? 'All meal records' : 'Meal-date range'}
              </button>
            ))}
          </div>
          {scope === 'range' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-md">
              <label className="text-xs font-semibold text-slate-600">
                From Date
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    reset();
                  }}
                  className="mt-1 w-full h-10 rounded-xl border border-slate-300 px-3 text-sm"
                />
              </label>
              <label className="text-xs font-semibold text-slate-600">
                To Date
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    reset();
                  }}
                  className="mt-1 w-full h-10 rounded-xl border border-slate-300 px-3 text-sm"
                />
              </label>
            </div>
          )}
          <Button type="button" variant="outline" onClick={check} disabled={busy !== null || rangeInvalid} className="gap-1.5">
            {busy === 'check' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
            Check records
          </Button>

          {summary && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {[
                ['Records', summary.totalRecords.toLocaleString()],
                ['ATE / Did not eat', `${summary.ateCount.toLocaleString()} / ${summary.didNotEatCount.toLocaleString()}`],
                ['Total amount', `${Number(summary.totalAmount).toLocaleString()} RWF`],
                ['Meal dates', `${fmtDay(summary.firstMealDate)} – ${fmtDay(summary.lastMealDate)}`],
              ].map(([k, v]) => (
                <div key={k} className="rounded-xl bg-slate-50 ring-1 ring-slate-200/70 px-3 py-2.5">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500">{k}</p>
                  <p className="mt-0.5 text-sm font-bold text-slate-900 tabular-nums">{v}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Step 2: backup */}
        <div className="space-y-2">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-500">2 · Download a backup</p>
          <div className="flex flex-wrap items-center gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={backup}
              disabled={!summary || summary.totalRecords === 0 || busy !== null}
              className="gap-1.5"
            >
              {busy === 'backup' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
              Download backup (.csv)
            </Button>
            {backupFile && (
              <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700">
                <CheckCircle2 className="h-4 w-4" /> {backupFile}
              </span>
            )}
          </div>
          <p className="text-[11px] text-slate-500">Keep this file safe: it contains every record that will be deleted, with all its dates and amounts.</p>
        </div>

        {/* Step 3: confirm */}
        <div className="space-y-2">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-500">3 · Confirm and delete</p>
          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <input
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              disabled={!backupFile}
              placeholder="Type DELETE"
              className="h-10 w-full sm:w-48 rounded-xl border border-slate-300 px-3 text-sm font-semibold tracking-wider disabled:bg-slate-50"
            />
            <Button type="button" variant="danger" onClick={remove} disabled={!canDelete} className="gap-1.5">
              {busy === 'delete' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
              Delete {summary ? summary.totalRecords.toLocaleString() : ''} record(s)
            </Button>
          </div>
          <p className="flex items-start gap-1.5 text-[11px] text-rose-700">
            <AlertTriangle className="h-3.5 w-3.5 shrink-0 mt-px" />
            This cannot be undone from the app. If the number of records changes after you check, the delete is refused.
          </p>
        </div>

        {lastResult && (
          <p className="rounded-xl bg-emerald-50 px-3 py-2.5 text-sm font-medium text-emerald-800 ring-1 ring-emerald-200">{lastResult}</p>
        )}
      </div>
    </Card>
  );
};

export default MealDataMaintenance;
