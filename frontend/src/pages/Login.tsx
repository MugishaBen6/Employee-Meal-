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
  Utensils,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/authApi';
import Button from '../components/common/Button';

interface LoginFormInputs {
  usernameOrEmail: string;
  password: string;
}

const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [setupNeeded, setSetupNeeded] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormInputs>();

  useEffect(() => {
    let isMounted = true;
    const checkSetup = async () => {
      try {
        const res = await authApi.getSetupStatus();
        if (isMounted) setSetupNeeded(res.setupNeeded);
      } catch (err) {
        // Backend not reachable or error
      }
    };
    checkSetup();
    return () => {
      isMounted = false;
    };
  }, []);

  const onSubmit = async (data: LoginFormInputs) => {
    setError(null);
    try {
      const user = await login(data.usernameOrEmail, data.password);
      const target =
        location.state?.from?.pathname ||
        (user?.role === 'ADMIN'
          ? '/admin/dashboard'
          : user?.role === 'MANAGING_DIRECTOR'
          ? '/director/dashboard'
          : user?.role === 'ACCOUNTANT'
          ? '/accountant/dashboard'
          : user?.role === 'HR'
          ? '/hr/dashboard'
          : '/dashboard');
      navigate(target, { replace: true });
    } catch (err: any) {
      const msg =
        err.response?.data?.message || 'Invalid username/email or password';
      setError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-[#f1f5f9] relative flex flex-col justify-center items-center py-8 sm:py-12 px-4 sm:px-6 lg:px-8 overflow-hidden font-sans select-none selection:bg-blue-500 selection:text-white">
      {/* Subtle Background Decorative Food Watermarks */}
      <div className="absolute -top-12 -left-12 w-64 h-64 sm:w-80 sm:h-80 pointer-events-none opacity-[0.07] text-blue-900 rotate-[-15deg]">
        <UtensilsCrossed className="w-full h-full stroke-[1.2]" />
      </div>

      <div className="absolute -bottom-16 -left-10 w-72 h-72 sm:w-96 sm:h-96 pointer-events-none opacity-[0.06] text-blue-900 rotate-[25deg]">
        <Utensils className="w-full h-full stroke-[1.2]" />
      </div>

      <div className="absolute top-1/4 -right-16 w-80 h-80 sm:w-[420px] sm:h-[420px] pointer-events-none opacity-[0.05] text-blue-900 rotate-[-30deg]">
        <UtensilsCrossed className="w-full h-full stroke-[1.2]" />
      </div>

      {/* Soft Radial Ambient Glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[350px] bg-blue-400/10 blur-[120px] rounded-full pointer-events-none" />

      {/* Main Container */}
      <div className="w-full max-w-[480px] z-10 flex flex-col items-center">
        {/* Top Header Section */}
        <div className="text-center space-y-2 mb-6 sm:mb-8">
          {/* Main App Icon */}
          <div className="mx-auto w-16 h-16 sm:w-20 sm:h-20 bg-gradient-to-b from-blue-500 to-blue-600 rounded-[22px] sm:rounded-[26px] flex items-center justify-center shadow-lg shadow-blue-500/25 border border-white/40 ring-4 ring-blue-500/10 transition-transform duration-300 hover:scale-105">
            <UtensilsCrossed className="w-9 h-9 sm:w-11 sm:h-11 text-white drop-shadow-xs" />
          </div>

          {/* Company Name */}
          <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight uppercase pt-1">
            RWANDA PLASTIC INDUSTRY
          </h1>

          {/* System Name */}
          <p className="text-xs sm:text-sm font-semibold text-slate-600">
            Employee Meal Management System
          </p>

          {/* Tagline */}
          <p className="text-[11px] sm:text-xs font-medium text-blue-600/90 tracking-wide">
            Healthy Employees • Productive Workplace • A Stronger Tomorrow
          </p>
        </div>

        {/* Centered White Login Card */}
        <div className="w-full bg-white rounded-3xl shadow-xl shadow-slate-300/40 border border-slate-200/80 p-6 sm:p-9 transition-all">
          {/* Meal Emblem Graphic */}
          <div className="flex flex-col items-center justify-center mb-5">
            <div className="relative w-20 h-20 sm:w-22 sm:h-22 flex items-center justify-center">
              {/* Dual-tone dynamic circular swoosh rings */}
              <svg
                className="absolute inset-0 w-full h-full -rotate-45"
                viewBox="0 0 100 100"
              >
                <path
                  d="M 50 8 A 42 42 0 0 1 92 50"
                  fill="none"
                  stroke="#2563eb"
                  strokeWidth="5.5"
                  strokeLinecap="round"
                />
                <path
                  d="M 50 92 A 42 42 0 0 1 8 50"
                  fill="none"
                  stroke="#f59e0b"
                  strokeWidth="5.5"
                  strokeLinecap="round"
                />
              </svg>

              {/* Inner Plate & Utensils Emblem */}
              <div className="w-14 h-14 sm:w-15 sm:h-15 rounded-full bg-gradient-to-b from-slate-50 to-blue-50/60 border border-slate-200 flex items-center justify-center shadow-inner">
                <div className="flex items-center gap-1 text-blue-600">
                  <Utensils className="w-7 h-7 sm:w-8 sm:h-8 text-blue-600" />
                </div>
              </div>
            </div>

            {/* Welcome Back & Subtitle */}
            <h2 className="text-xl sm:text-2xl font-black text-slate-900 mt-2 tracking-tight">
              Welcome Back
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
              Sign in to your account to continue
            </p>
          </div>

          {/* Initial Setup Notification if needed */}
          {setupNeeded && (
            <div className="mb-5 p-3.5 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-start gap-3">
              <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-bold text-emerald-900">
                  Initial Setup Required
                </p>
                <p className="text-xs text-emerald-700 mt-0.5">
                  No administrator account exists yet.
                </p>
                <Link
                  to="/setup-admin"
                  className="inline-block mt-1.5 text-xs font-bold text-emerald-700 hover:text-emerald-900 underline"
                >
                  Configure Master Admin Account &rarr;
                </Link>
              </div>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="mb-5 bg-rose-50 border border-rose-200 rounded-2xl p-3.5 flex items-start gap-3 animate-shake">
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <p className="text-xs sm:text-sm font-medium text-rose-700">
                {error}
              </p>
            </div>
          )}

          {/* Login Form */}
          <form className="space-y-4 sm:space-y-4.5" onSubmit={handleSubmit(onSubmit)}>
            {/* Email or Username */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">
                Email or Username
              </label>
              <div className="relative">
                <input
                  type="text"
                  {...register('usernameOrEmail', {
                    required: 'Email or Username is required',
                  })}
                  className="w-full bg-slate-50/50 hover:bg-white focus:bg-white border border-slate-200 hover:border-slate-300 rounded-xl px-3.5 py-2.5 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 pl-10 transition-all shadow-xs"
                  placeholder="Enter your email or username"
                  autoComplete="username"
                />
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              </div>
              {errors.usernameOrEmail && (
                <p className="text-xs text-rose-500 font-medium mt-1">
                  {errors.usernameOrEmail.message}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  {...register('password', {
                    required: 'Password is required',
                  })}
                  className="w-full bg-slate-50/50 hover:bg-white focus:bg-white border border-slate-200 hover:border-slate-300 rounded-xl px-3.5 py-2.5 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 pl-10 pr-10 transition-all shadow-xs"
                  placeholder="Enter your password"
                  autoComplete="current-password"
                />
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors p-1"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
                    <EyeOff className="w-4 h-4" />
                  ) : (
                    <Eye className="w-4 h-4" />
                  )}
                </button>
              </div>
              {errors.password && (
                <p className="text-xs text-rose-500 font-medium mt-1">
                  {errors.password.message}
                </p>
              )}
            </div>

            {/* Remember Me & Forgot Password Row */}
            <div className="flex items-center justify-between pt-1">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="w-4 h-4 rounded text-blue-600 border-slate-300 focus:ring-blue-500 cursor-pointer"
                />
                <span className="text-xs font-medium text-slate-600 hover:text-slate-900">
                  Remember me
                </span>
              </label>

              <Link
                to="/forgot-password"
                className="text-xs font-semibold text-blue-600 hover:text-blue-700 hover:underline transition-colors"
              >
                Forgot password?
              </Link>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2.5 sm:py-3 px-4 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 active:from-blue-800 active:to-blue-900 text-white font-bold text-sm sm:text-base rounded-xl shadow-md shadow-blue-500/20 hover:shadow-lg hover:shadow-blue-500/30 transition-all flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <span>Login</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            {/* Create Account Buttons */}
            <div className="pt-4 border-t border-slate-100 space-y-2.5">
              <p className="text-center text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Don't have an account?
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <Link
                  to="/register"
                  className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-slate-50 hover:bg-blue-50/60 border border-slate-200/90 hover:border-blue-300 text-xs font-bold text-slate-700 hover:text-blue-700 transition-all text-center group shadow-2xs"
                >
                  <UserPlus className="w-3.5 h-3.5 text-blue-600 group-hover:scale-110 transition-transform shrink-0" />
                  <span className="truncate">Create Account as User</span>
                </Link>

                <Link
                  to="/register?role=ADMIN"
                  className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl bg-slate-50 hover:bg-emerald-50/60 border border-slate-200/90 hover:border-emerald-300 text-xs font-bold text-slate-700 hover:text-emerald-700 transition-all text-center group shadow-2xs"
                >
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 group-hover:scale-110 transition-transform shrink-0" />
                  <span className="truncate">Create Account as Admin</span>
                </Link>
              </div>
            </div>

            {/* Trust Footer */}
            <div className="pt-2 text-center">
              <p className="text-[11px] font-medium text-slate-400 tracking-wide">
                Secure • Reliable • Efficient
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Login;

