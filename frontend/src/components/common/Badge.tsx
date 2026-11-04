import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  size?: 'sm' | 'md';
}

/** Status pill with a small colour dot; soft tinted background and an inset ring. */
export const Badge: React.FC<BadgeProps> = ({ children, variant = 'neutral', size = 'sm' }) => {
  const styles = {
    success: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
    warning: 'bg-amber-50 text-amber-800 ring-amber-600/20',
    danger: 'bg-rose-50 text-rose-700 ring-rose-600/15',
    info: 'bg-indigo-50 text-indigo-700 ring-indigo-600/15',
    neutral: 'bg-slate-100 text-slate-700 ring-slate-500/15',
  };

  const dots = {
    success: 'bg-emerald-500',
    warning: 'bg-amber-500',
    danger: 'bg-rose-500',
    info: 'bg-indigo-500',
    neutral: 'bg-slate-400',
  };

  const sizes = {
    sm: 'px-2 py-0.5 text-[11px] font-semibold gap-1.5',
    md: 'px-2.5 py-1 text-xs font-semibold gap-1.5',
  };

  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-full ring-1 ring-inset tracking-wide ${styles[variant]} ${sizes[size]}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full shrink-0 ${dots[variant]}`} aria-hidden="true" />
      {children}
    </span>
  );
};

export default Badge;
