import React, { useEffect } from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info';
  message: string;
}

interface ToastProps {
  toasts: ToastMessage[];
  onClose: (id: string) => void;
}

export const ToastContainer: React.FC<ToastProps> = ({ toasts, onClose }) => {
  return (
    <div
      className="fixed bottom-4 inset-x-3 sm:inset-x-auto sm:right-6 sm:bottom-6 z-[60] flex flex-col gap-2.5 sm:w-[380px] pointer-events-none"
      aria-live="polite"
    >
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastItem toast={toast} onClose={() => onClose(toast.id)} />
        </div>
      ))}
    </div>
  );
};

export const Toast: React.FC<{ message: string; type: 'success' | 'error' | 'info'; onClose: () => void }> = ({
  message,
  type,
  onClose,
}) => {
  return (
    <div className="fixed bottom-4 inset-x-3 sm:inset-x-auto sm:right-6 sm:bottom-6 z-[60] sm:w-[380px]" aria-live="polite">
      <ToastItem toast={{ id: '1', type, message }} onClose={onClose} />
    </div>
  );
};

const ToastItem: React.FC<{ toast: ToastMessage; onClose: () => void }> = ({ toast, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const config = {
    success: {
      icon: <CheckCircle2 className="w-[18px] h-[18px]" />,
      chip: 'bg-emerald-50 text-emerald-600 ring-emerald-600/10',
      bar: 'bg-emerald-500',
      title: 'Success',
    },
    error: {
      icon: <AlertCircle className="w-[18px] h-[18px]" />,
      chip: 'bg-rose-50 text-rose-600 ring-rose-600/10',
      bar: 'bg-rose-500',
      title: 'Something went wrong',
    },
    info: {
      icon: <Info className="w-[18px] h-[18px]" />,
      chip: 'bg-indigo-50 text-indigo-600 ring-indigo-600/10',
      bar: 'bg-indigo-500',
      title: 'Notice',
    },
  }[toast.type];

  return (
    <div
      role={toast.type === 'error' ? 'alert' : 'status'}
      className="relative flex items-start gap-3 p-3.5 pr-2.5 rounded-2xl bg-white shadow-elevated ring-1 ring-slate-900/[0.06] overflow-hidden animate-slideUp"
    >
      <span className={`absolute left-0 top-0 bottom-0 w-1 ${config.bar}`} aria-hidden="true" />
      <span className={`ml-1 flex h-8 w-8 items-center justify-center rounded-xl ring-1 ring-inset shrink-0 ${config.chip}`}>
        {config.icon}
      </span>
      <div className="min-w-0 flex-1 pt-0.5">
        <p className="text-[13px] font-semibold text-slate-900 leading-tight">{config.title}</p>
        <p className="text-[13px] text-slate-600 mt-0.5 leading-snug break-words">{toast.message}</p>
      </div>
      <button
        onClick={onClose}
        aria-label="Dismiss notification"
        className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors shrink-0"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

export default Toast;
