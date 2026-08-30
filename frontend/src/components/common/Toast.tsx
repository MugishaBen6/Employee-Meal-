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
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm w-full">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onClose={() => onClose(toast.id)} />
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
    <div className="fixed bottom-5 right-5 z-50 max-w-sm w-full">
      <ToastItem toast={{ id: '1', type, message }} onClose={onClose} />
    </div>
  );
};

const ToastItem: React.FC<{ toast: ToastMessage; onClose: () => void }> = ({ toast, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const icons = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0" />,
    error: <AlertCircle className="w-5 h-5 text-rose-500 shrink-0" />,
    info: <Info className="w-5 h-5 text-sky-500 shrink-0" />,
  };

  const borders = {
    success: 'border-emerald-200 bg-emerald-50/90 text-emerald-900',
    error: 'border-rose-200 bg-rose-50/90 text-rose-900',
    info: 'border-sky-200 bg-sky-50/90 text-sky-900',
  };

  return (
    <div className={`flex items-center justify-between p-4 rounded-xl border shadow-lg backdrop-blur-md transition-all animate-slideUp ${borders[toast.type]}`}>
      <div className="flex items-center gap-3">
        {icons[toast.type]}
        <p className="text-sm font-medium">{toast.message}</p>
      </div>
      <button onClick={onClose} className="p-1 hover:bg-black/5 rounded-md transition-colors ml-2">
        <X className="w-4 h-4 text-slate-500" />
      </button>
    </div>
  );
};

export default Toast;
