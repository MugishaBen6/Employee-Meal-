import React, { useEffect, useState } from 'react';
import { settingsApi } from '../api/settingsApi';
import { Setting } from '../types';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Skeleton } from '../components/common/Skeleton';
import { ToastContainer, ToastMessage } from '../components/common/Toast';
import { Settings as SettingsIcon, Save } from 'lucide-react';

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
    <div className="space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
          <SettingsIcon className="w-6 h-6 text-indigo-600" />
          System Settings
        </h2>
        <p className="text-sm text-slate-500 mt-1">Configure global application parameters and meal prices</p>
      </div>

      <Card>
        {loading ? (
          <div className="space-y-4">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {settings.map((s) => (
              <div key={s.id} className="py-4 first:pt-0 last:pb-0 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h4 className="text-sm font-semibold text-slate-800 font-mono">{s.settingKey}</h4>
                  <p className="text-xs text-slate-500 mt-0.5">{s.description || 'System configuration parameter'}</p>
                </div>
                <div className="flex items-center gap-3 w-full sm:w-auto">
                  <input
                    type="text"
                    value={formValues[s.settingKey] || ''}
                    onChange={(e) => setFormValues({ ...formValues, [s.settingKey]: e.target.value })}
                    className="flex-1 sm:w-64 px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-medium focus:ring-2 focus:ring-indigo-500 bg-white"
                  />
                  <Button
                    size="sm"
                    className="gap-1.5"
                    isLoading={updatingKey === s.settingKey}
                    onClick={() => handleUpdate(s.settingKey)}
                  >
                    <Save className="w-4 h-4" />
                    Save
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  );
};

export { SettingsPage as Settings };
export default SettingsPage;
