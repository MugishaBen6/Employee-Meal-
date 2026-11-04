import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
}

export const Card: React.FC<CardProps> = ({ children, className = '', title, subtitle, action }) => {
  return (
    <div
      className={`bg-white rounded-2xl border border-slate-200/70 shadow-card p-4 sm:p-6 max-w-full transition-shadow duration-300 hover:shadow-card-hover ${className}`}
    >
      {(title || action) && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4 pb-3.5 border-b border-slate-100">
          <div className="min-w-0">
            {title && (
              <h3 className="font-display text-[15px] sm:text-base font-bold text-slate-900 tracking-tight truncate">{title}</h3>
            )}
            {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </div>
      )}
      {children}
    </div>
  );
};

export default Card;
