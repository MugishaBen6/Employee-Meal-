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
import loginBg from '../assets/login-bg.jpg';

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
    <div
      className="h-screen max-h-screen relative flex flex-col justify-center items-center p-3 sm:p-4 overflow-hidden font-sans select-none selection:bg-blue-500 selection:text-white bg-cover bg-center bg-no-repeat"
      style={{ backgroundImage: `url(${loginBg})` }}
    >
      {/* Soft Elegant Backdrop Overlay */}
      <div className="absolute inset-0 bg-slate-900/15 backdrop-blur-[1px] pointer-events-none" />
      <div className="absolute inset-0 bg-gradient-to-b from-white/30 via-transparent to-slate-900/20 pointer-events-none" />

      {/* Main Container */}
      <div className="w-full max-w-[430px] z-10 flex flex-col items-center my-auto">
        {/* Top Header Section */}
        <div className="text-center space-y-1 mb-2.5 sm:mb-3">
          {/* Main App Icon */}
          <div className="mx-auto w-11 h-11 sm:w-13 sm:h-13 bg-gradient-to-b from-blue-500 to-blue-600 rounded-[18px] sm:rounded-[20px] flex items-center justify-center shadow-md shadow-blue-500/20 border border-white/40 ring-2 ring-blue-500/10 transition-transform duration-200 hover:scale-105">
            <UtensilsCrossed className="w-6 h-6 sm:w-7 sm:h-7 text-white drop-shadow-xs" />
          </div>

          {/* Company Name */}
          <h1 className="text-base sm:text-lg font-black text-slate-900 tracking-tight uppercase pt-0.5 leading-tight">
            RWANDA PLASTIC INDUSTRY
          </h1>

          {/* System Name */}
          <p className="text-[11px] sm:text-xs font-semibold text-slate-600 leading-tight">
            Employee Meal Management System
          </p>

          {/* Tagline */}
          <p className="text-[10px] sm:text-[11px] font-medium text-blue-600/90 tracking-wide leading-tight">
            Healthy Employees • Productive Workplace • A Stronger Tomorrow
          </p>
        </div>

        {/* Centered White Login Card */}
        <div className="w-full bg-white rounded-2xl sm:rounded-3xl shadow-lg shadow-slate-300/40 border border-slate-200/80 p-4 sm:py-5 sm:px-6 transition-all">
          {/* Meal Emblem Graphic */}
          <div className="flex flex-col items-center justify-center mb-2.5 sm:mb-3">
            <div className="relative w-12 h-12 sm:w-14 sm:h-14 flex items-center justify-center">
              {/* Dual-tone dynamic circular swoosh rings */}
              <svg
                className="absolute inset-0 w-full h-full -rotate-45"
                viewBox="0 0 100 100"
              >
                <path
                  d="M 50 8 A 42 42 0 0 1 92 50"
                  fill="none"
                  stroke="#2563eb"
                  strokeWidth="6"
                  strokeLinecap="round"
                />
                <path
                  d="M 50 92 A 42 42 0 0 1 8 50"
                  fill="none"
                  stroke="#f59e0b"
                  strokeWidth="6"
                  strokeLinecap="round"
                />
              </svg>

              {/* Inner Plate & Utensils Emblem */}
              <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gradient-to-b from-slate-50 to-blue-50/60 border border-slate-200 flex items-center justify-center shadow-inner">
                <Utensils className="w-4.5 h-4.5 sm:w-5 sm:h-5 text-blue-600" />
              </div>
            </div>

            {/* Welcome Back & Subtitle */}
            <h2 className="text-base sm:text-lg font-black text-slate-900 mt-1 tracking-tight leading-tight">
              Welcome Back
            </h2>
            <p className="text-[11px] text-slate-500 mt-0.5 leading-tight">
              Sign in to your account to continue
            </p>
          </div>

          {/* Initial Setup Notification if needed */}
          {setupNeeded && (
            <div className="mb-2.5 p-2.5 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <p className="text-[11px] font-bold text-emerald-900">
                  Initial Setup Required
                </p>
                <Link
                  to="/setup-admin"
                  className="text-[10px] font-bold text-emerald-700 hover:text-emerald-900 underline"
                >
                  Configure Master Admin Account &rarr;
                </Link>
              </div>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="mb-2.5 bg-rose-50 border border-rose-200 rounded-xl p-2.5 flex items-start gap-2 animate-shake">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <p className="text-[11px] font-medium text-rose-700 leading-tight">
                {error}
              </p>
            </div>
          )}

          {/* Login Form */}
          <form className="space-y-2.5 sm:space-y-3" onSubmit={handleSubmit(onSubmit)}>
            {/* Email or Username */}
            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">
                Email or Username
              </label>
              <div className="relative">
                <input
                  type="text"
                  {...register('usernameOrEmail', {
                    required: 'Email or Username is required',
                  })}
                  className="w-full bg-slate-50/50 hover:bg-white focus:bg-white border border-slate-200 hover:border-slate-300 rounded-lg px-3 py-1.5 sm:py-2 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 pl-9 transition-all shadow-xs"
                  placeholder="Enter your email or username"
                  autoComplete="username"
                />
                <User className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              </div>
              {errors.usernameOrEmail && (
                <p className="text-[10px] text-rose-500 font-medium mt-0.5">
                  {errors.usernameOrEmail.message}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  {...register('password', {
                    required: 'Password is required',
                  })}
                  className="w-full bg-slate-50/50 hover:bg-white focus:bg-white border border-slate-200 hover:border-slate-300 rounded-lg px-3 py-1.5 sm:py-2 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 pl-9 pr-9 transition-all shadow-xs"
                  placeholder="Enter your password"
                  autoComplete="current-password"
                />
                <Lock className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors p-1"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
                    <EyeOff className="w-3.5 h-3.5" />
                  ) : (
                    <Eye className="w-3.5 h-3.5" />
                  )}
                </button>
              </div>
              {errors.password && (
                <p className="text-[10px] text-rose-500 font-medium mt-0.5">
                  {errors.password.message}
                </p>
              )}
            </div>

            {/* Remember Me & Forgot Password Row */}
            <div className="flex items-center justify-between pt-0.5">
              <label className="flex items-center gap-1.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="w-3.5 h-3.5 rounded text-blue-600 border-slate-300 focus:ring-blue-500 cursor-pointer"
                />
                <span className="text-[11px] font-medium text-slate-600 hover:text-slate-900">
                  Remember me
                </span>
              </label>

              <Link
                to="/forgot-password"
                className="text-[11px] font-semibold text-blue-600 hover:text-blue-700 hover:underline transition-colors"
              >
                Forgot password?
              </Link>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2 sm:py-2.5 px-3.5 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 active:from-blue-800 active:to-blue-900 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-blue-500/20 hover:shadow-lg hover:shadow-blue-500/30 transition-all flex items-center justify-center gap-1.5 disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <span>Login</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </>
              )}
            </button>

            {/* Create Account Buttons */}
            <div className="pt-2 border-t border-slate-100 space-y-1.5">
              <p className="text-center text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
                Don't have an account?
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                <Link
                  to="/register"
                  className="flex items-center justify-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-50 hover:bg-blue-50/60 border border-slate-200/90 hover:border-blue-300 text-[11px] font-bold text-slate-700 hover:text-blue-700 transition-all text-center group shadow-2xs"
                >
                  <UserPlus className="w-3 h-3 text-blue-600 group-hover:scale-110 transition-transform shrink-0" />
                  <span className="truncate">Create Account as User</span>
                </Link>

                <Link
                  to="/register?role=ADMIN"
                  className="flex items-center justify-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-50 hover:bg-emerald-50/60 border border-slate-200/90 hover:border-emerald-300 text-[11px] font-bold text-slate-700 hover:text-emerald-700 transition-all text-center group shadow-2xs"
                >
                  <ShieldCheck className="w-3 h-3 text-emerald-600 group-hover:scale-110 transition-transform shrink-0" />
                  <span className="truncate">Create Account as Admin</span>
                </Link>
              </div>
            </div>

            {/* Trust Footer */}
            <div className="pt-0.5 text-center">
              <p className="text-[10px] font-medium text-slate-400 tracking-wide">
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

