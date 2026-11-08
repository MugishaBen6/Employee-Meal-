import React, { useCallback, useEffect, useState } from 'react';
import {
  Trash2,
  RotateCcw,
  Search,
  ChevronLeft,
  ChevronRight,
  Users,
  UtensilsCrossed,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Loader2,
  Inbox,
} from 'lucide-react';
import { trashApi, TrashItem, TrashItemType, TrashSummary, RestoreResult } from '../api/trashApi';
import { PageHero, HeroStat, Toolbar, fmtNumber, heroBtn } from '../components/dashboard/DashboardKit';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { Skeleton } from '../components/common/Skeleton';
import { ToastContainer, ToastMessage } from '../components/common/Toast';

type Tab = 'ALL' | TrashItemType;

const TABS: { key: Tab; label: string }[] = [
  { key: 'ALL', label: 'All items' },
  { key: 'EMPLOYEE', label: 'Employees' },
  { key: 'MEAL_RECORD', label: 'Meal records' },
];

const fmtDay = (iso: string | null) => {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

const fmtWhen = (iso: string) => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

const money = (v: number) => `${Number(v || 0).toLocaleString()} RWF`;

/** ADMIN Recycle Bin: review, restore, or permanently delete removed employees and meal records. */
export const TrashPage: React.FC = () => {
  const [tab, setTab] = useState<Tab>('ALL');
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [page, setPage] = useState(0);
  const size = 20;

  const [items, setItems] = useState<TrashItem[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [summary, setSummary] = useState<TrashSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [selectingAll, setSelectingAll] = useState(false);

  const [busy, setBusy] = useState<'restore' | 'purge' | null>(null);
  const [purgeTarget, setPurgeTarget] = useState<number[] | null>(null);
  const [confirmText, setConfirmText] = useState('');
  const [restoreResult, setRestoreResult] = useState<RestoreResult | null>(null);
  const [emptyingAll, setEmptyingAll] = useState(false);
  const [loadingAllIds, setLoadingAllIds] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const toast = (type: ToastMessage['type'], message: string) =>
    setToasts((t) => [...t, { id: `${Date.now()}-${Math.random()}`, type, message }]);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 300);
    return () => clearTimeout(t);
  }, [query]);

  // New filters: back to page 1 and an empty selection (never act on hidden items).
  useEffect(() => {
    setPage(0);
    setSelected(new Set());
  }, [tab, debounced]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [list, sum] = await Promise.all([
        trashApi.list({ type: tab, query: debounced, page, size }),
        trashApi.summary(),
      ]);
      setItems(list.content);
      setTotal(list.totalElements);
      setTotalPages(list.totalPages);
      setSummary(sum);
    } catch (e: any) {
      toast('error', e.response?.data?.message || 'Could not load Trash');
    } finally {
      setLoading(false);
    }
  }, [tab, debounced, page]);

  useEffect(() => {
    load();
  }, [load]);

  const allOnPage = items.length > 0 && items.every((i) => selected.has(i.id));
  const togglePage = () => setSelected(allOnPage ? new Set() : new Set(items.map((i) => i.id)));
  const toggle = (id: number) =>
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  const selectAllMatching = async () => {
    setSelectingAll(true);
    try {
      setSelected(new Set(await trashApi.ids({ type: tab, query: debounced })));
    } catch {
      toast('error', 'Could not select all items');
    } finally {
      setSelectingAll(false);
    }
  };

  const trashCount = summary ? summary.employees + summary.mealRecords : 0;

  /** Empty Trash: every item, whatever tab or search is active. */
  const openEmptyTrash = async () => {
    setLoadingAllIds(true);
    try {
      const all = await trashApi.ids({});
      if (all.length === 0) {
        toast('info', 'Trash is already empty.');
        return;
      }
      setEmptyingAll(true);
      setPurgeTarget(all);
    } catch {
      toast('error', 'Could not read the Trash contents.');
    } finally {
      setLoadingAllIds(false);
    }
  };

  const restore = async (ids: number[]) => {
    setBusy('restore');
    try {
      const res = await trashApi.restore(ids);
      if (res.data.failed > 0) {
        setRestoreResult(res.data);
      } else {
        toast('success', res.message);
      }
      setSelected(new Set());
      load();
    } catch (e: any) {
      toast('error', e.response?.data?.message || 'Restore failed. Nothing was changed.');
    } finally {
      setBusy(null);
    }
  };

  const purge = async () => {
    if (!purgeTarget) return;
    setBusy('purge');
    try {
      const res = await trashApi.purge(purgeTarget);
      toast('success', res.message);
      setPurgeTarget(null);
      setEmptyingAll(false);
      setConfirmText('');
      setSelected(new Set());
      load();
    } catch (e: any) {
      toast('error', e.response?.data?.message || 'Permanent delete failed. Nothing was deleted.');
    } finally {
      setBusy(null);
    }
  };

  const typeBadge = (i: TrashItem) =>
    i.itemType === 'EMPLOYEE' ? (
      <Badge variant="info">Employee</Badge>
    ) : (
      <Badge variant="warning">Meal record</Badge>
    );

  const details = (i: TrashItem) =>
    i.itemType === 'EMPLOYEE' ? (
      <>
        <span>{[i.position, i.department].filter(Boolean).join(' · ') || '—'}</span>
        <span className="block text-slate-400">
          {i.mealCount} meal record{i.mealCount === 1 ? '' : 's'} · {money(i.mealAmount)}
        </span>
      </>
    ) : (
      <>
        <span>
          {fmtDay(i.mealDate)} ·{' '}
          <b className={i.mealStatus === 'ATE' ? 'text-emerald-700' : 'text-rose-600'}>{i.mealStatus === 'ATE' ? 'ATE' : 'DID NOT EAT'}</b> ·{' '}
          {money(i.mealAmount)}
        </span>
        <span className="block text-slate-400">Recorded by {i.recordedBy ?? '—'}</span>
        {i.employeeMissing && <span className="block text-amber-700 font-medium">Employee is not in the list: restore it first</span>}
      </>
    );

  const rowActions = (i: TrashItem) => (
    <div className="flex items-center justify-end gap-1.5">
      <Button size="sm" variant="outline" onClick={() => restore([i.id])} disabled={busy !== null} className="gap-1">
        <RotateCcw className="h-3.5 w-3.5" /> Restore
      </Button>
      <button
        type="button"
        onClick={() => setPurgeTarget([i.id])}
        disabled={busy !== null}
        title="Delete permanently"
        aria-label="Delete permanently"
        className="p-2 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 disabled:opacity-50"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  );

  return (
    <div className="space-y-5 sm:space-y-6">
      <PageHero
        icon={Trash2}
        eyebrow="Administration · Recovery"
        title="Trash"
        subtitle="Deleted employees and meal records are kept here. Restore puts them back exactly as they were; nothing in Trash counts in lists, reports or totals."
        actions={
          <button
            type="button"
            onClick={openEmptyTrash}
            disabled={trashCount === 0 || busy !== null || loadingAllIds}
            className={`${heroBtn.ghost} disabled:opacity-50 disabled:cursor-not-allowed`}
          >
            {loadingAllIds ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
            Empty Trash
          </button>
        }
        stats={
          <>
            <HeroStat label="Employees" value={summary ? fmtNumber(summary.employees) : '—'} sub={summary ? `${fmtNumber(summary.mealsInsideEmployees)} meals inside` : ''} />
            <HeroStat label="Meal records" value={summary ? fmtNumber(summary.mealRecords) : '—'} />
            <HeroStat label="ATE amount" value={summary ? fmtNumber(summary.ateAmount) : '—'} sub="RWF in Trash" />
          </>
        }
      />

      <Toolbar>
        <div className="flex flex-col lg:flex-row lg:items-center gap-3">
          <div className="inline-flex rounded-xl bg-slate-100 p-1 self-start">
            {TABS.map((t) => (
              <button
                key={t.key}
                type="button"
                onClick={() => setTab(t.key)}
                className={`h-8 rounded-lg px-3 text-xs sm:text-sm font-semibold transition-colors ${
                  tab === t.key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by employee name, ID or who deleted it"
              className="w-full h-10 rounded-xl border border-slate-300 pl-9 pr-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>
      </Toolbar>

      <Card className="p-0 overflow-hidden">
        {selected.size > 0 && (
          <div className="bg-indigo-50/90 border-b border-indigo-100 px-4 py-3 flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2 text-xs sm:text-sm text-indigo-950">
              <span className="inline-flex min-w-[22px] justify-center rounded-full bg-indigo-600 px-2 text-xs font-bold text-white">
                {selected.size}
              </span>
              <span className="font-semibold">selected</span>
              {total > items.length && selected.size < total && (
                <button
                  type="button"
                  onClick={selectAllMatching}
                  disabled={selectingAll}
                  className="font-bold text-indigo-700 underline hover:text-indigo-900"
                >
                  {selectingAll ? 'Selecting…' : `Select all ${total.toLocaleString()} items`}
                </button>
              )}
              <button type="button" onClick={() => setSelected(new Set())} className="text-indigo-600 underline">
                Clear
              </button>
            </div>
            <div className="flex items-center gap-2">
              <Button size="sm" onClick={() => restore(Array.from(selected))} disabled={busy !== null} className="gap-1.5">
                {busy === 'restore' ? <Loader2 className="h-4 w-4 animate-spin" /> : <RotateCcw className="h-4 w-4" />}
                Restore ({selected.size})
              </Button>
              <Button size="sm" variant="danger" onClick={() => setPurgeTarget(Array.from(selected))} disabled={busy !== null} className="gap-1.5">
                <Trash2 className="h-4 w-4" /> Delete permanently
              </Button>
            </div>
          </div>
        )}

        {loading ? (
          <div className="p-4 space-y-2.5">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-14 w-full rounded-xl" />
            ))}
          </div>
        ) : items.length === 0 ? (
          <div className="py-16 text-center">
            <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100">
              <Inbox className="h-6 w-6 text-slate-400" />
            </div>
            <p className="text-sm font-semibold text-slate-700">Trash is empty</p>
            <p className="mt-1 text-xs text-slate-500">Deleted employees and meal records will appear here.</p>
          </div>
        ) : (
          <>
            {/* Desktop table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50/80">
                  <tr className="text-left border-b border-slate-100">
                    <th className="px-4 py-3 w-10">
                      <input type="checkbox" checked={allOnPage} onChange={togglePage} aria-label="Select all on this page" className="h-4 w-4 accent-indigo-600" />
                    </th>
                    <th className="px-3 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Type</th>
                    <th className="px-3 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Employee</th>
                    <th className="px-3 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Details</th>
                    <th className="px-3 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">Deleted</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {items.map((i) => (
                    <tr key={i.id} className={selected.has(i.id) ? 'bg-indigo-50/40' : 'hover:bg-slate-50/60'}>
                      <td className="px-4 py-3">
                        <input type="checkbox" checked={selected.has(i.id)} onChange={() => toggle(i.id)} aria-label="Select item" className="h-4 w-4 accent-indigo-600" />
                      </td>
                      <td className="px-3 py-3">{typeBadge(i)}</td>
                      <td className="px-3 py-3">
                        <p className="font-semibold text-slate-900">{i.employeeName || '—'}</p>
                        <p className="text-xs text-slate-500">{i.employeeCode}</p>
                      </td>
                      <td className="px-3 py-3 text-xs text-slate-600">{details(i)}</td>
                      <td className="px-3 py-3 text-xs">
                        <p className="font-semibold text-slate-800">{i.deletedBy}</p>
                        <p className="text-slate-500">{fmtWhen(i.deletedAt)}</p>
                      </td>
                      <td className="px-4 py-3">{rowActions(i)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Mobile cards */}
            <div className="md:hidden p-3 space-y-2.5">
              <label className="flex items-center gap-2.5 px-1 text-xs font-semibold text-slate-500">
                <input type="checkbox" checked={allOnPage} onChange={togglePage} className="h-[18px] w-[18px] accent-indigo-600" />
                Select all on this page
              </label>
              {items.map((i) => (
                <div key={i.id} className={`rounded-2xl ring-1 p-3.5 ${selected.has(i.id) ? 'ring-indigo-300 bg-indigo-50/40' : 'ring-slate-200/80 bg-white'}`}>
                  <div className="flex items-start gap-3">
                    <input type="checkbox" checked={selected.has(i.id)} onChange={() => toggle(i.id)} className="mt-1 h-[18px] w-[18px] accent-indigo-600" />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <p className="truncate font-semibold text-slate-900">{i.employeeName || '—'}</p>
                        {typeBadge(i)}
                      </div>
                      <p className="text-xs text-slate-500">{i.employeeCode}</p>
                      <div className="mt-1.5 text-xs text-slate-600">{details(i)}</div>
                      <p className="mt-1.5 text-[11px] text-slate-500">
                        Deleted by <b className="text-slate-700">{i.deletedBy}</b> · {fmtWhen(i.deletedAt)}
                      </p>
                      <div className="mt-2.5">{rowActions(i)}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-xs text-slate-500">
              <span>
                Page {page + 1} of {totalPages} ({total.toLocaleString()} item{total === 1 ? '' : 's'})
              </span>
              <div className="flex gap-1.5">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="h-8 w-8 rounded-lg ring-1 ring-slate-200 flex items-center justify-center disabled:opacity-40"
                  aria-label="Previous page"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="h-8 w-8 rounded-lg ring-1 ring-slate-200 flex items-center justify-center disabled:opacity-40"
                  aria-label="Next page"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          </>
        )}
      </Card>

      <p className="flex items-center gap-2 text-xs text-slate-500">
        <Users className="h-3.5 w-3.5" /> Restoring an employee also restores the meal records deleted with it.
        <UtensilsCrossed className="h-3.5 w-3.5 ml-2" /> A meal record is restored only if that employee has no other record on the same day.
      </p>

      {/* Permanent delete confirmation */}
      <Modal
        isOpen={!!purgeTarget}
        onClose={() => {
          if (busy) return;
          setPurgeTarget(null);
          setEmptyingAll(false);
          setConfirmText('');
        }}
        title={emptyingAll ? 'Empty Trash?' : 'Delete permanently?'}
        maxWidth="md"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-xl bg-rose-50 ring-1 ring-rose-200 p-3.5 text-sm text-rose-900">
            <AlertTriangle className="h-5 w-5 shrink-0 text-rose-600 mt-0.5" />
            <div>
              <p className="font-bold">
                {emptyingAll && summary
                  ? `Everything in Trash will be destroyed forever: ${summary.employees.toLocaleString()} employee(s) with ${summary.mealsInsideEmployees.toLocaleString()} meal record(s) inside, and ${summary.mealRecords.toLocaleString()} meal record(s).`
                  : `${purgeTarget?.length.toLocaleString()} item${purgeTarget && purgeTarget.length > 1 ? 's' : ''} will be destroyed forever.`}
              </p>
              <p className="mt-1 text-xs text-rose-700">
                Unlike moving to Trash, this cannot be undone. Employees are destroyed together with the meal records inside them.
                The action is recorded in the Activity Logs.
              </p>
            </div>
          </div>
          <label className="block text-xs font-semibold text-slate-700">
            Type DELETE to confirm
            <input
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              placeholder="DELETE"
              className="mt-1 w-full h-10 rounded-xl border border-slate-300 px-3 text-sm font-semibold tracking-wider"
            />
          </label>
          <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
            <Button variant="outline" onClick={() => { setPurgeTarget(null); setEmptyingAll(false); setConfirmText(''); }} disabled={busy !== null}>
              Cancel
            </Button>
            <Button variant="danger" onClick={purge} disabled={confirmText !== 'DELETE' || busy !== null} isLoading={busy === 'purge'} className="gap-1.5">
              <Trash2 className="h-4 w-4" /> {emptyingAll ? 'Empty Trash' : 'Delete permanently'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Restore results when some items could not be restored */}
      <Modal isOpen={!!restoreResult} onClose={() => setRestoreResult(null)} title="Restore results" maxWidth="lg">
        {restoreResult && (
          <div className="space-y-3">
            <p className="text-sm text-slate-700">
              <b className="text-emerald-700">{restoreResult.restored} restored</b> ·{' '}
              <b className="text-rose-600">{restoreResult.failed} not restored</b> (still in Trash)
            </p>
            <ul className="max-h-72 overflow-y-auto divide-y divide-slate-100 rounded-xl ring-1 ring-slate-200">
              {restoreResult.results.map((r) => (
                <li key={r.id} className="flex items-start gap-2 px-3 py-2 text-xs">
                  {r.ok ? <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" /> : <XCircle className="h-4 w-4 shrink-0 text-rose-600" />}
                  <span className={r.ok ? 'text-slate-600' : 'text-slate-900'}>{r.message}</span>
                </li>
              ))}
            </ul>
            <div className="flex justify-end">
              <Button onClick={() => setRestoreResult(null)}>Close</Button>
            </div>
          </div>
        )}
      </Modal>

      <ToastContainer toasts={toasts} onClose={(id) => setToasts((t) => t.filter((x) => x.id !== id))} />
    </div>
  );
};

export default TrashPage;
