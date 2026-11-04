import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  UtensilsCrossed,
  Lock,
  User,
  AlertCircle,
  ShieldCheck,
  UserPlus,
  Eye,
  EyeOff,
  ArrowRight,
  CalendarCheck2,
  FileBarChart2,
  Fingerprint,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/authApi';
import loginBg from '../assets/login-bg.jpg';

interface LoginFormInputs {
  usernameOrEmail: string;
  password: string;
}

const HIGHLIGHTS = [
  { icon: CalendarCheck2, title: 'Daily meal attendance', text: 'Record ATE / DID NOT EAT in seconds, for any date.' },
  { icon: FileBarChart2, title: 'Accurate reports', text: 'Date-range reports with PDF and Excel exports.' },
  { icon: Fingerprint, title: 'Secure by role', text: 'Every change audited, every role in its lane.' },
];

const Login: React.FC = () => {
  const { login, getDashboardPath } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [setupNeeded, setSetupNeeded] = useState(false);
  const [slowServerWarning, setSlowServerWarning] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormInputs>();

  useEffect(() => {
    let isMounted = true;
    // Proactively warm up backend container immediately on login screen visit
    authApi.ping();

    const checkSetup = async () => {
      try {
        const res = await authApi.getSetupStatus();
        if (isMounted) setSetupNeeded(res.setupNeeded);
      } catch (err) {
        // Backend not reachable or waking up
      }
    };
    checkSetup();
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let timer: any;
    if (isSubmitting) {
      timer = setTimeout(() => {
        setSlowServerWarning(true);
      }, 2500);
    } else {
      setSlowServerWarning(false);
    }
    return () => clearTimeout(timer);
  }, [isSubmitting]);

  const onSubmit = async (data: LoginFormInputs) => {
    setError(null);
    try {
      const user = await login(data.usernameOrEmail, data.password);
      const fromPath = location.state?.from?.pathname as string | undefined;
      const target =
        fromPath && fromPath !== '/login'
          ? fromPath
          : getDashboardPath() || (user ? '/dashboard' : '/login');
      navigate(target, { replace: true });
    } catch (err: any) {
      const msg =
        err.response?.data?.message || 'Invalid username/email or password';
      setError(msg);
    }
  };

  const inputClass =
    'w-full h-12 rounded-xl border bg-white pl-11 pr-3.5 text-[15px] text-slate-900 placeholder:text-slate-400 shadow-xs ' +
    'hover:border-slate-300 focus:outline-none';

  return (
    <div className="min-h-[100dvh] w-full flex flex-col lg:flex-row bg-white font-sans">
      {/* ---------------- Brand panel (photo) ---------------- */}
      <aside
        className="relative lg:w-[52%] xl:w-[55%] h-56 sm:h-64 lg:h-auto lg:min-h-[100dvh] shrink-0 overflow-hidden bg-slate-900 bg-cover bg-center"
        style={{ backgroundImage: `url(${loginBg})` }}
      >
        <div className="absolute inset-0 bg-gradient-to-br from-[#0B1220]/90 via-[#1B2156]/80 to-[#3440C7]/55" />
        <div
          className="absolute inset-0 opacity-60"
          style={{ background: 'radial-gradient(600px 400px at 85% 110%, rgba(245,158,11,0.28), transparent 70%)' }}
        />

        <div className="relative z-10 h-full flex flex-col justify-between p-6 sm:p-8 lg:p-12 xl:p-14 text-white">
          {/* Brand */}
          <div className="flex items-center gap-3">
            <div className="relative w-11 h-11 lg:w-12 lg:h-12 rounded-2xl bg-white/10 ring-1 ring-white/25 backdrop-blur-md flex items-center justify-center shadow-lg">
              <UtensilsCrossed className="w-5 h-5 lg:w-6 lg:h-6" />
              <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full bg-amber-400 ring-2 ring-[#1B2156]" aria-hidden="true" />
            </div>
            <div className="leading-tight">
              <p className="font-display font-extrabold tracking-[0.1em] text-[15px] lg:text-base">ISAHANE</p>
              <p className="text-[11px] lg:text-xs text-white/70 font-medium tracking-wide">RWANDA PLASTIC INDUSTRY</p>
            </div>
          </div>

          {/* Headline (desktop) */}
          <div className="hidden lg:block max-w-lg">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 ring-1 ring-white/20 px-3 py-1 text-xs font-semibold tracking-wide text-white/90 backdrop-blur-md">
              <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
              Employee Meal Management System
            </span>
            <h1 className="mt-6 font-display text-4xl xl:text-5xl font-extrabold leading-[1.08] tracking-tight">
              Every meal,
              <br />
              <span className="bg-gradient-to-r from-white to-indigo-200 bg-clip-text text-transparent">accounted for.</span>
            </h1>
            <p className="mt-5 text-base xl:text-lg text-white/75 leading-relaxed">
              Track daily meal attendance and costs for every employee, with clear reports your whole team can trust.
            </p>

            <ul className="mt-10 space-y-4">
              {HIGHLIGHTS.map(({ icon: Icon, title, text }) => (
                <li key={title} className="flex items-start gap-3.5">
                  <span className="mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 ring-1 ring-white/15 backdrop-blur-md shrink-0">
                    <Icon className="h-5 w-5 text-amber-300" />
                  </span>
                  <div>
                    <p className="font-semibold text-[15px]">{title}</p>
                    <p className="text-sm text-white/65">{text}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          {/* Tagline (mobile / tablet) */}
          <div className="lg:hidden pb-6">
            <h1 className="font-display text-2xl sm:text-3xl font-extrabold leading-tight tracking-tight">Every meal, accounted for.</h1>
            <p className="mt-1 text-sm text-white/75">Employee Meal Management System</p>
          </div>

          <p className="hidden lg:block text-xs text-white/55 tracking-wide">
            © {new Date().getFullYear()} RWANDA PLASTIC INDUSTRY · Secure • Reliable • Efficient
          </p>
        </div>
      </aside>

      {/* ---------------- Sign-in panel ---------------- */}
      <main className="relative flex-1 flex items-start lg:items-center justify-center -mt-6 lg:mt-0 rounded-t-[28px] lg:rounded-none bg-white px-5 sm:px-8 pt-8 pb-10 lg:py-12 z-10">
        <div className="w-full max-w-[420px] animate-fadeIn">
          <div className="mb-7">
            <div className="hidden lg:flex mb-6 h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-indigo-700 text-white shadow-glow ring-4 ring-indigo-100">
              <UtensilsCrossed className="h-7 w-7" />
            </div>
            <h2 className="font-display text-[28px] sm:text-3xl font-extrabold text-slate-900 tracking-tight">Welcome back</h2>
            <p className="mt-1.5 text-[15px] text-slate-500">Sign in to your account to continue.</p>
          </div>

          {/* Initial Setup Notification if needed */}
          {setupNeeded && (
            <div className="mb-5 flex items-start gap-3 rounded-2xl bg-emerald-50 ring-1 ring-inset ring-emerald-600/15 p-4">
              <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-semibold text-emerald-900">Initial setup required</p>
                <Link
                  to="/setup-admin"
                  className="mt-0.5 inline-flex items-center gap-1 text-sm font-semibold text-emerald-700 hover:text-emerald-900"
                >
                  Configure the master admin account <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="mb-5 flex items-start gap-3 rounded-2xl bg-rose-50 ring-1 ring-inset ring-rose-600/15 p-4 animate-shake" role="alert">
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <p className="text-sm font-medium text-rose-800 leading-snug">{error}</p>
            </div>
          )}

          {/* Login Form */}
          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
            {/* Email or Username */}
            <div>
              <label htmlFor="login-identifier" className="block text-sm font-semibold text-slate-700 mb-1.5">
                Email or username
              </label>
              <div className="relative">
                <User className="w-[18px] h-[18px] text-slate-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  id="login-identifier"
                  type="text"
                  {...register('usernameOrEmail', {
                    required: 'Email or Username is required',
                  })}
                  className={`${inputClass} ${errors.usernameOrEmail ? 'border-rose-300' : 'border-slate-200'}`}
                  placeholder="you@company.com or username"
                  autoComplete="username"
                  aria-invalid={!!errors.usernameOrEmail}
                />
              </div>
              {errors.usernameOrEmail && (
                <p className="mt-1.5 flex items-center gap-1 text-xs font-medium text-rose-600">
                  <AlertCircle className="w-3.5 h-3.5" /> {errors.usernameOrEmail.message}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label htmlFor="login-password" className="block text-sm font-semibold text-slate-700">
                  Password
                </label>
                <Link
                  to="/forgot-password"
                  className="text-sm font-semibold text-indigo-600 hover:text-indigo-800 transition-colors"
                >
                  Forgot password?
                </Link>
              </div>
              <div className="relative">
                <Lock className="w-[18px] h-[18px] text-slate-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  {...register('password', {
                    required: 'Password is required',
                  })}
                  className={`${inputClass} pr-12 ${errors.password ? 'border-rose-300' : 'border-slate-200'}`}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  aria-invalid={!!errors.password}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 h-9 w-9 flex items-center justify-center rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="w-[18px] h-[18px]" /> : <Eye className="w-[18px] h-[18px]" />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1.5 flex items-center gap-1 text-xs font-medium text-rose-600">
                  <AlertCircle className="w-3.5 h-3.5" /> {errors.password.message}
                </p>
              )}
            </div>

            {/* Remember Me */}
            <label className="flex items-center gap-2.5 cursor-pointer select-none w-fit">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="w-[18px] h-[18px] rounded-md text-indigo-600 border-slate-300 focus:ring-indigo-500 cursor-pointer"
              />
              <span className="text-sm font-medium text-slate-600">Remember me</span>
            </label>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="group w-full h-12 px-4 rounded-xl bg-indigo-600 text-white text-[15px] font-semibold shadow-glow shadow-inner-highlight hover:bg-indigo-700 active:scale-[0.99] transition-all flex items-center justify-center gap-2 disabled:opacity-75 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin shrink-0" />
                  <span>{slowServerWarning ? 'Connecting / Waking server...' : 'Signing in...'}</span>
                </>
              ) : (
                <>
                  <span>Sign in</span>
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
                </>
              )}
            </button>
          </form>

          {/* Create Account */}
          <div className="mt-8">
            <div className="relative flex items-center justify-center">
              <span className="absolute inset-x-0 h-px bg-slate-200" />
              <span className="relative bg-white px-3 text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
                Don't have an account?
              </span>
            </div>
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <Link
                to="/register"
                className="group flex items-center justify-center gap-2 h-11 rounded-xl ring-1 ring-slate-200 bg-white hover:bg-indigo-50/60 hover:ring-indigo-300 text-sm font-semibold text-slate-700 hover:text-indigo-700 transition-all"
              >
                <UserPlus className="w-4 h-4 text-indigo-600 transition-transform group-hover:scale-110" />
                Create account as User
              </Link>
              <Link
                to="/register?role=ADMIN"
                className="group flex items-center justify-center gap-2 h-11 rounded-xl ring-1 ring-slate-200 bg-white hover:bg-emerald-50/60 hover:ring-emerald-300 text-sm font-semibold text-slate-700 hover:text-emerald-700 transition-all"
              >
                <ShieldCheck className="w-4 h-4 text-emerald-600 transition-transform group-hover:scale-110" />
                Create account as Admin
              </Link>
            </div>
          </div>

          <p className="mt-8 text-center text-xs font-medium text-slate-400 tracking-wide lg:hidden">
            RWANDA PLASTIC INDUSTRY · Secure • Reliable • Efficient
          </p>
        </div>
      </main>
    </div>
  );
};

export default Login;
