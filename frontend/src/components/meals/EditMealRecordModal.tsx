import React, { useEffect, useMemo, useState } from 'react';
import { CalendarDays, Info, Loader2, Search, UserRound, ArrowRightLeft, X } from 'lucide-react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import { useAuth } from '../../context/AuthContext';
import { employeeApi } from '../../api/employeeApi';
import { mealApi } from '../../api/mealApi';
import { Employee, MealRecord, MealStatus, UpdateMealRecordRequest } from '../../types';
import { getKigaliToday } from '../../utils/dateUtils';

interface Props {
  record: MealRecord | null;
  onClose: () => void;
  onSaved: (updated: MealRecord, message: string) => void;
  onError: (message: string) => void;
  /** Configured standard meal price, used to show the ATE amount. */
  standardPrice?: number;
  currency?: string;
}

const fmtDay = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return d && m && y ? `${d}/${m}/${y}` : iso;
};

/**
 * Edit any meal record the caller may edit (including past dates): status, amount, meal date,
 * the employee it belongs to, and, where permitted, the employee's name and position.
 * The server enforces every rule; this dialog only hides what the caller cannot change.
 */
export const EditMealRecordModal: React.FC<Props> = ({ record, onClose, onSaved, onError, standardPrice, currency = 'RWF' }) => {
  const { user, hasRole } = useAuth();
  const isUser = hasRole('USER');

  const [status, setStatus] = useState<MealStatus>('ATE');
  const [amount, setAmount] = useState<number>(0);
  const [mealDate, setMealDate] = useState('');
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [loadingEmployee, setLoadingEmployee] = useState(false);
  const [name, setName] = useState('');
  const [position, setPosition] = useState('');
  const [reassignTo, setReassignTo] = useState<Employee | null>(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [results, setResults] = useState<Employee[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!record) return;
    setStatus(record.mealStatus);
    setAmount(record.mealStatus === 'DID_NOT_EAT' ? 0 : Number(record.amount ?? 0));
    setMealDate(record.mealDate);
    setReassignTo(null);
    setSearchOpen(false);
    setSearch('');
    setResults([]);
    setEmployee(null);
    setName(record.employeeName);
    setPosition(record.position ?? '');
    setLoadingEmployee(true);
    employeeApi
      .getById(record.employeeId)
      .then((e) => {
        setEmployee(e);
        setName(e.fullName);
        setPosition(e.position ?? '');
      })
      .catch(() => setEmployee(null))
      .finally(() => setLoadingEmployee(false));
  }, [record]);

  // Employee search for "wrong person" corrections.
  useEffect(() => {
    if (!searchOpen || search.trim().length < 2) {
      setResults([]);
      return;
    }
    const t = setTimeout(async () => {
      try {
        const found = await employeeApi.quickSearch(search.trim());
        setResults(found.filter((e) => e.id !== record?.employeeId && e.status === 'ACTIVE').slice(0, 8));
      } catch {
        setResults([]);
      }
    }, 250);
    return () => clearTimeout(t);
  }, [search, searchOpen, record]);

  // ADMIN / HR may correct any employee; a USER only employees they added themselves.
  const canCorrectEmployee =
    !reassignTo && !!employee && (hasRole('ADMIN', 'HR') || (isUser && !!user && employee.createdBy === user.username));

  // Amount shown follows the pricing rules (the server applies the same):
  // DID_NOT_EAT = 0; USER cannot type amounts; an ATE record keeps its stored amount.
  const shownAmount = useMemo(() => {
    if (status === 'DID_NOT_EAT') return 0;
    if (isUser || !amount) {
      if (record?.mealStatus === 'ATE' && Number(record.amount) > 0) return Number(record.amount);
      return standardPrice ?? 0;
    }
    return amount;
  }, [status, isUser, amount, record, standardPrice]);

  if (!record) return null;

  const changes: string[] = [];
  if (status !== record.mealStatus) changes.push(`Status ${record.mealStatus} → ${status}`);
  if (shownAmount !== Number(record.amount)) changes.push(`Amount ${Number(record.amount).toLocaleString()} → ${shownAmount.toLocaleString()} ${currency}`);
  if (mealDate !== record.mealDate) changes.push(`Meal date ${fmtDay(record.mealDate)} → ${fmtDay(mealDate)}`);
  if (reassignTo) changes.push(`Employee → ${reassignTo.employeeCode} ${reassignTo.fullName}`);
  if (canCorrectEmployee && employee && name.trim() !== employee.fullName) changes.push(`Name "${employee.fullName}" → "${name.trim()}"`);
  if (canCorrectEmployee && employee && position.trim() !== (employee.position ?? '')) changes.push(`Position "${employee.position}" → "${position.trim()}"`);

  const save = async () => {
    if (!mealDate) return onError('Choose a meal date.');
    if (canCorrectEmployee && !name.trim()) return onError('Employee name cannot be empty.');
    const body: UpdateMealRecordRequest = {
      mealStatus: status,
      amount: status === 'DID_NOT_EAT' ? 0 : isUser ? shownAmount : Number(amount) || 0,
    };
    if (mealDate !== record.mealDate) body.mealDate = mealDate;
    if (reassignTo) body.employeeId = reassignTo.id;
    if (canCorrectEmployee && employee) {
      if (name.trim() !== employee.fullName) body.employeeName = name.trim();
      if (position.trim() !== (employee.position ?? '')) body.position = position.trim();
    }
    setSaving(true);
    try {
      const updated = await mealApi.updateMealRecord(record.id, body);
      onSaved(updated, changes.length ? `Saved: ${changes.join(' · ')}` : 'No changes.');
    } catch (err: any) {
      onError(err.response?.data?.message || 'Failed to update the meal record.');
    } finally {
      setSaving(false);
    }
  };

  const inputCls =
    'w-full h-10 rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-slate-100 disabled:text-slate-500';
  const labelCls = 'block text-xs font-semibold text-slate-700 mb-1';

  return (
    <Modal isOpen={!!record} onClose={onClose} title="Edit meal record" maxWidth="lg">
      <div className="space-y-5">
        {/* Employee */}
        <section className="rounded-2xl bg-slate-50 ring-1 ring-slate-200/70 p-3.5 space-y-3">
          <div className="flex items-center justify-between gap-2">
            <p className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-500">
              <UserRound className="h-4 w-4" /> Employee
            </p>
            <span className="text-[11px] font-semibold text-slate-400">{record.employeeCode}</span>
          </div>

          {reassignTo ? (
            <div className="flex items-center justify-between gap-2 rounded-xl bg-indigo-50 ring-1 ring-indigo-200 px-3 py-2">
              <p className="text-sm text-indigo-900">
                <span className="text-slate-500 line-through mr-1.5">{record.employeeName}</span>→{' '}
                <b>{reassignTo.fullName}</b> <span className="text-xs text-indigo-700">({reassignTo.employeeCode})</span>
              </p>
              <button type="button" onClick={() => setReassignTo(null)} className="text-slate-400 hover:text-slate-700" aria-label="Undo">
                <X className="h-4 w-4" />
              </button>
            </div>
          ) : loadingEmployee ? (
            <p className="flex items-center gap-2 text-xs text-slate-500"><Loader2 className="h-3.5 w-3.5 animate-spin" /> Loading employee…</p>
          ) : canCorrectEmployee ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label className={labelCls}>
                Employee Name
                <input value={name} onChange={(e) => setName(e.target.value)} className={`${inputCls} mt-1`} maxLength={100} />
              </label>
              <label className={labelCls}>
                Position
                <input value={position} onChange={(e) => setPosition(e.target.value)} className={`${inputCls} mt-1`} maxLength={50} />
              </label>
              <p className="sm:col-span-2 -mt-1 flex items-start gap-1.5 text-[11px] text-slate-500">
                <Info className="h-3.5 w-3.5 shrink-0 mt-px" />
                Correcting the name or position updates this employee everywhere, including their other records.
              </p>
            </div>
          ) : (
            <div>
              <p className="text-sm font-semibold text-slate-900">{record.employeeName}</p>
              <p className="text-xs text-slate-500">{employee?.position ?? record.position ?? ''}</p>
              {isUser && (
                <p className="mt-1 text-[11px] text-slate-400">You can correct the name only for employees you added yourself.</p>
              )}
            </div>
          )}

          {!reassignTo && (
            <div>
              {!searchOpen ? (
                <button
                  type="button"
                  onClick={() => setSearchOpen(true)}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                >
                  <ArrowRightLeft className="h-3.5 w-3.5" /> Wrong person? Move this record to another employee
                </button>
              ) : (
                <div className="space-y-2">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      autoFocus
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      placeholder="Search the correct employee by name or ID"
                      className={`${inputCls} pl-9`}
                    />
                  </div>
                  {results.length > 0 && (
                    <ul className="max-h-44 overflow-y-auto rounded-xl bg-white ring-1 ring-slate-200 divide-y divide-slate-100">
                      {results.map((e) => (
                        <li key={e.id}>
                          <button
                            type="button"
                            onClick={() => {
                              setReassignTo(e);
                              setSearchOpen(false);
                            }}
                            className="w-full px-3 py-2 text-left text-sm hover:bg-indigo-50"
                          >
                            <b className="text-slate-900">{e.fullName}</b>{' '}
                            <span className="text-xs text-slate-500">{e.employeeCode} · {e.position}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                  <button type="button" onClick={() => setSearchOpen(false)} className="text-xs font-semibold text-slate-500 hover:text-slate-700">
                    Cancel
                  </button>
                </div>
              )}
            </div>
          )}
        </section>

        {/* Meal */}
        <section className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <label className={labelCls}>
            <span className="flex items-center gap-1"><CalendarDays className="h-3.5 w-3.5" /> Meal Date</span>
            <input
              type="date"
              value={mealDate}
              max={isUser ? getKigaliToday() : undefined}
              onChange={(e) => setMealDate(e.target.value)}
              className={`${inputCls} mt-1`}
            />
          </label>
          <label className={labelCls}>
            Meal Status
            <select
              value={status}
              onChange={(e) => {
                const s = e.target.value as MealStatus;
                setStatus(s);
                if (s === 'ATE' && !isUser && !amount) {
                  setAmount(record.mealStatus === 'ATE' && Number(record.amount) > 0 ? Number(record.amount) : standardPrice ?? 0);
                }
              }}
              className={`${inputCls} mt-1 font-semibold`}
            >
              <option value="ATE">ATE</option>
              <option value="DID_NOT_EAT">DID NOT EAT</option>
            </select>
          </label>
          <label className={labelCls}>
            Amount Used ({currency})
            {isUser || status === 'DID_NOT_EAT' ? (
              <input value={shownAmount.toLocaleString()} readOnly disabled className={`${inputCls} mt-1 tabular-nums`} />
            ) : (
              <input
                type="number"
                min={0}
                value={amount || ''}
                placeholder={standardPrice ? String(standardPrice) : 'Standard price'}
                onChange={(e) => setAmount(Number(e.target.value))}
                className={`${inputCls} mt-1 tabular-nums`}
              />
            )}
          </label>
        </section>
        <p className="-mt-3 text-[11px] text-slate-500">
          DID NOT EAT is always 0 {currency}. ATE uses the standard meal price
          {isUser ? ' (set automatically)' : ' when the amount is left empty'}.
        </p>

        {changes.length > 0 && (
          <div className="rounded-xl bg-amber-50 ring-1 ring-amber-200 px-3 py-2.5">
            <p className="text-[11px] font-bold uppercase tracking-wider text-amber-800">Changes to save</p>
            <ul className="mt-1 space-y-0.5 text-xs text-amber-900">
              {changes.map((c) => (
                <li key={c}>• {c}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 pt-1">
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={save} isLoading={saving} disabled={changes.length === 0}>
            Save changes
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default EditMealRecordModal;
