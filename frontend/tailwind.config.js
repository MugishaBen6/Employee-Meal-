/** @type {import('tailwindcss').Config} */

/*
 * Design system — Employee Meal Management (RWANDA PLASTIC INDUSTRY · ISAHANE)
 *
 * Primary  "Royal"   : refined royal blue — trust, professionalism, efficiency.
 *                       Mapped onto `indigo` AND `blue` so every existing page
 *                       picks up the same primary without per-file rewrites.
 * Accent   "Saffron" : warm amber already used for meal/cost highlights.
 * Status             : emerald = ATE / success, rose = DID NOT EAT / danger,
 *                       amber = warning, sky = info.
 * Neutrals           : Tailwind slate (cool, crisp, high legibility).
 */
const royal = {
  50: '#F1F4FF',
  100: '#E3E9FE',
  200: '#CBD5FD',
  300: '#A6B6FA',
  400: '#7A8DF5',
  500: '#5568EE',
  600: '#3F4FE2',
  700: '#3440C7',
  800: '#2D37A0',
  900: '#2A347D',
  950: '#191E49',
};

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        indigo: royal,
        blue: royal,
        brand: royal,
        ink: {
          50: '#F5F7FB',
          100: '#EBEFF6',
          900: '#0B1220',
          950: '#070C17',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        // Codes, amounts and times: the interface font with tabular digits (see index.css)
        // instead of a typewriter-style monospace.
        mono: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        '2xs': ['0.6875rem', { lineHeight: '1rem' }],
      },
      boxShadow: {
        xs: '0 1px 2px 0 rgba(16, 24, 40, 0.04)',
        '2xs': '0 1px 1px 0 rgba(16, 24, 40, 0.03)',
        card: '0 1px 2px 0 rgba(16, 24, 40, 0.04), 0 1px 3px 0 rgba(16, 24, 40, 0.05)',
        'card-hover': '0 10px 24px -8px rgba(16, 24, 40, 0.12), 0 2px 6px -2px rgba(16, 24, 40, 0.06)',
        elevated: '0 20px 48px -12px rgba(16, 24, 40, 0.22), 0 4px 12px -4px rgba(16, 24, 40, 0.08)',
        glow: '0 8px 20px -6px rgba(63, 79, 226, 0.45)',
        'inner-highlight': 'inset 0 1px 0 0 rgba(255, 255, 255, 0.18)',
      },
      backdropBlur: {
        xs: '2px',
      },
      borderRadius: {
        '4xl': '2rem',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        overlayIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        scaleUp: {
          '0%': { opacity: '0', transform: 'translateY(12px) scale(0.97)' },
          '100%': { opacity: '1', transform: 'translateY(0) scale(1)' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        shake: {
          '0%, 100%': { transform: 'translateX(0)' },
          '20%, 60%': { transform: 'translateX(-4px)' },
          '40%, 80%': { transform: 'translateX(4px)' },
        },
      },
      animation: {
        fadeIn: 'fadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1) both',
        overlayIn: 'overlayIn 0.2s ease-out both',
        scaleUp: 'scaleUp 0.28s cubic-bezier(0.22, 1, 0.36, 1) both',
        slideUp: 'slideUp 0.3s cubic-bezier(0.22, 1, 0.36, 1) both',
        shake: 'shake 0.4s ease-in-out',
      },
    },
  },
  plugins: [],
};
