import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'success';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  className = '',
  disabled,
  ...props
}) => {
  const baseStyle =
    'relative inline-flex items-center justify-center font-semibold tracking-[-0.005em] rounded-xl ' +
    'transition-all duration-200 ease-out select-none whitespace-nowrap ' +
    'focus:outline-none focus-visible:ring-4 ' +
    'enabled:hover:-translate-y-px enabled:active:translate-y-0 enabled:active:scale-[0.985] ' +
    'disabled:opacity-55 disabled:cursor-not-allowed disabled:shadow-none';

  const variants = {
    primary:
      'bg-indigo-600 text-white shadow-sm shadow-indigo-600/25 shadow-inner-highlight ' +
      'enabled:hover:bg-indigo-700 enabled:hover:shadow-glow focus-visible:ring-indigo-500/25',
    secondary:
      'bg-slate-900 text-white shadow-sm shadow-slate-900/20 shadow-inner-highlight ' +
      'enabled:hover:bg-slate-800 focus-visible:ring-slate-500/25',
    outline:
      'bg-white text-slate-700 border border-slate-200 shadow-xs ' +
      'enabled:hover:bg-slate-50 enabled:hover:border-slate-300 enabled:hover:text-slate-900 focus-visible:ring-indigo-500/20',
    danger:
      'bg-rose-600 text-white shadow-sm shadow-rose-600/25 shadow-inner-highlight ' +
      'enabled:hover:bg-rose-700 focus-visible:ring-rose-500/25',
    success:
      'bg-emerald-600 text-white shadow-sm shadow-emerald-600/25 shadow-inner-highlight ' +
      'enabled:hover:bg-emerald-700 focus-visible:ring-emerald-500/25',
  };

  const sizes = {
    sm: 'px-3.5 py-1.5 text-xs gap-1.5 min-h-[36px]',
    md: 'px-4 py-2 text-sm gap-2 min-h-[42px]',
    lg: 'px-6 py-3 text-[15px] gap-2.5 min-h-[48px]',
  };

  return (
    <button
      className={`${baseStyle} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      {...props}
    >
      {isLoading && (
        <svg className="animate-spin -ml-0.5 h-4 w-4 text-current shrink-0" fill="none" viewBox="0 0 24 24" aria-hidden="true">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
      {children}
    </button>
  );
};

export default Button;
