import React, { useEffect } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl' | '5xl';
}

/**
 * Dialog. On phones it is a bottom sheet (full width, rounded top, thumb-reachable);
 * from the `sm` breakpoint up it is a centred card.
 */
export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  maxWidth = 'md',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.style.overflow = 'unset';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const widthClasses = {
    sm: 'sm:max-w-sm',
    md: 'sm:max-w-md',
    lg: 'sm:max-w-lg',
    xl: 'sm:max-w-xl',
    '2xl': 'sm:max-w-2xl',
    '3xl': 'sm:max-w-3xl',
    '4xl': 'sm:max-w-4xl',
    '5xl': 'sm:max-w-5xl',
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4 bg-slate-950/55 backdrop-blur-sm animate-overlayIn">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`bg-white w-full ${widthClasses[maxWidth]} max-h-[92dvh] sm:max-h-[88vh] flex flex-col overflow-hidden rounded-t-3xl sm:rounded-2xl shadow-elevated ring-1 ring-slate-900/5 animate-scaleUp`}
      >
        {/* Grab handle (phones) */}
        <div className="sm:hidden flex justify-center pt-2.5 pb-0.5 shrink-0" aria-hidden="true">
          <span className="h-1.5 w-10 rounded-full bg-slate-200" />
        </div>
        <div className="flex items-center justify-between gap-3 px-5 sm:px-6 pt-2.5 pb-3.5 sm:py-4 border-b border-slate-100 shrink-0">
          <h3 className="font-display text-base sm:text-lg font-bold text-slate-900 tracking-tight truncate">{title}</h3>
          <button
            onClick={onClose}
            aria-label="Close dialog"
            className="text-slate-400 hover:text-slate-700 hover:bg-slate-100 active:bg-slate-200 p-2 rounded-xl transition-colors min-w-[38px] min-h-[38px] flex items-center justify-center shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="px-5 py-5 sm:p-6 overflow-y-auto touch-scroll flex-1 pb-[max(1.25rem,env(safe-area-inset-bottom))]">
          {children}
        </div>
      </div>
    </div>
  );
};

export default Modal;
