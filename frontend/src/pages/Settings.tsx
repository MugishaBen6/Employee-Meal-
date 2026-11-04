import React, { useEffect, useState } from 'react';
import { settingsApi } from '../api/settingsApi';
import { Setting } from '../types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Skeleton } from '../components/common/Skeleton';
import { ToastContainer, ToastMessage } from '../components/common/Toast';
import { Settings as SettingsIcon, Save, Building2, Coins, Globe2, UtensilsCrossed, SlidersHorizontal } from 'lucide-react';

/** Friendly names and icons for known setting keys (keys and values are unchanged). */
const SETTING_META: Record<string, { label: string; icon: React.ComponentType<{ className?: string }> }> = {
  COMPANY_NAME: { label: 'Company name', icon: Building2 },
  CURRENCY: { label: 'Currency', icon: Coins },
  TIMEZONE: { label: 'Time zone', icon: Globe2 },
  STANDARD_MEAL_PRICE: { label: 'Standard meal price', icon: UtensilsCrossed },
};

export const SettingsPage: React.FC = () => {
  const [settings, setSettings] = useState<Setting[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingKey, setUpdatingKey] = useState<string | null>(null);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [formValues, setFormValues] = useState<Record<string, string>>({});

  const addToast = (type: 'success' | 'error' | 'info', message: string) => {
    setToasts((prev) => [...prev, { id: Date.now().toString(), type, message }]);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    setLoading(true);
    try {
      const data = await settingsApi.getAll();
      setSettings(data);
      const vals: Record<string, string> = {};
      data.forEach((s) => {
        vals[s.settingKey] = s.settingValue;
      });
      setFormValues(vals);
    } catch (e) {
      console.error(e);
      addToast('error', 'Failed to load system settings');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (key: string) => {
    const val = formValues[key];
    if (val === undefined) return;

    setUpdatingKey(key);
    try {
      await settingsApi.update(key, val);
      addToast('success', `Setting "${key}" updated successfully.`);
      fetchSettings();
    } catch (e: any) {
      addToast('error', e.response?.data?.message || 'Failed to update setting');
    } finally {
      setUpdatingKey(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="font-display text-2xl font-bold text-slate-900 flex items-center gap-2.5">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 ring-1 ring-inset ring-indigo-600/10">
            <SettingsIcon className="w-5 h-5 text-indigo-600" />
          </span>
          System Settings
        </h2>
        <p className="text-sm text-slate-500 mt-1.5">Configure global application parameters and meal prices</p>
      </div>

      <Card className="p-0 overflow-hidden" title={undefined}>
        <div className="flex items-center gap-2 px-5 sm:px-6 py-4 border-b border-slate-100 bg-slate-50/60">
          <SlidersHorizontal className="w-4 h-4 text-slate-400" />
          <p className="text-sm font-semibold text-slate-700">General configuration</p>
          <p className="ml-auto text-xs text-slate-400 hidden sm:block">Each value is saved individually</p>
        </div>
        {loading ? (
          <div className="p-5 sm:p-6 space-y-4">
            <Skeleton className="h-16 w-full rounded-xl" />
            <Skeleton className="h-16 w-full rounded-xl" />
            <Skeleton className="h-16 w-full rounded-xl" />
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {settings.map((s) => {
              const meta = SETTING_META[s.settingKey];
              const Icon = meta?.icon ?? SlidersHorizontal;
              const current = formValues[s.settingKey] ?? '';
              const changed = current !== s.settingValue;
              const isPrice = s.settingKey === 'STANDARD_MEAL_PRICE';
              return (
                <div
                  key={s.id}
                  className="px-5 sm:px-6 py-5 flex flex-col md:flex-row md:items-center justify-between gap-4 md:gap-8 hover:bg-slate-50/50 transition-colors"
                >
                  <div className="flex items-start gap-3.5 flex-1 min-w-0">
                    <span className="mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500 shrink-0">
                      <Icon className="w-[18px] h-[18px]" />
                    </span>
                    <div className="min-w-0">
                      <h4 className="text-[15px] font-semibold text-slate-900">{meta?.label ?? s.settingKey}</h4>
                      <p className="text-[13px] text-slate-500 mt-0.5 leading-relaxed">{s.description || 'System configuration parameter'}</p>
                      <code className="mt-1.5 inline-block rounded-md bg-slate-100 px-1.5 py-0.5 text-[11px] font-mono text-slate-500 break-all">
                        {s.settingKey}
                      </code>
                    </div>
                  </div>
                  <div className="flex items-center gap-2.5 w-full md:w-auto shrink-0">
                    <div className="relative flex-1 md:w-72">
                      <input
                        type="text"
                        aria-label={meta?.label ?? s.settingKey}
                        value={current}
                        onChange={(e) => setFormValues({ ...formValues, [s.settingKey]: e.target.value })}
                        className={`w-full h-11 px-3.5 ${isPrice ? 'pr-14' : ''} rounded-xl border bg-white text-sm font-medium text-slate-900 shadow-xs hover:border-slate-300 focus:outline-none ${
                          changed ? 'border-amber-300' : 'border-slate-200'
                        }`}
                      />
                      {isPrice && (
                        <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400">
                          {formValues['CURRENCY'] || 'RWF'}
                        </span>
                      )}
                    </div>
                    <Button
                      size="md"
                      className="gap-1.5 shrink-0"
                      isLoading={updatingKey === s.settingKey}
                      onClick={() => handleUpdate(s.settingKey)}
                    >
                      <Save className="w-4 h-4" />
                      Save
                    </Button>
                  </div>
                  {changed && (
                    <p className="md:hidden -mt-2 text-xs font-medium text-amber-700">Unsaved change</p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </Card>

      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  );
};

export { SettingsPage as Settings };
export default SettingsPage;
