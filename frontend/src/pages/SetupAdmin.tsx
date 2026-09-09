import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { ShieldCheck, ArrowLeft, CheckCircle2, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { authApi } from '../api/authApi';
import { SetupAdminRequest } from '../types';
import Button from '../components/common/Button';

const SetupAdmin: React.FC = () => {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [isInitialSetup, setIsInitialSetup] = useState(true);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SetupAdminRequest>();

  useEffect(() => {
    const checkStatus = async () => {
      try {
        const res = await authApi.getSetupStatus();
        setIsInitialSetup(res.setupNeeded);
      } catch (err) {
        // Backend not ready or error, fallback safely without blocking UI
        setIsInitialSetup(false);
      }
    };
    checkStatus();
  }, []);

  const onSubmit = async (data: SetupAdminRequest) => {
    setError(null);
    try {
      await authApi.setupInitialAdmin(data);
      setIsSuccess(true);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to initialize administrator account.';
      setError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-8 sm:py-12 px-3 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="mx-auto h-14 w-14 sm:h-16 sm:w-16 bg-emerald-600 rounded-2xl flex items-center justify-center shadow-lg shadow-emerald-500/20">
          <ShieldCheck className="h-8 w-8 sm:h-9 sm:w-9 text-white" />
        </div>
        <h2 className="mt-4 sm:mt-6 text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          {isInitialSetup ? 'Initial Master Admin Setup' : 'Create Admin Account'}
        </h2>
        <p className="mt-1.5 sm:mt-2 text-xs sm:text-sm text-slate-400">
          {isInitialSetup
            ? 'Configure the primary administrator account for your company'
            : 'Register an administrator account for meal management and oversight'}
        </p>
      </div>

      <div className="mt-6 sm:mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-slate-800 py-6 px-4.5 sm:py-8 sm:px-10 shadow-2xl rounded-2xl border border-slate-700">
          {isSuccess ? (
            <div className="text-center py-4">
              <div className="mx-auto flex items-center justify-center h-14 w-14 rounded-full bg-emerald-500/10 text-emerald-400 mb-4 border border-emerald-500/20">
                <CheckCircle2 className="h-8 w-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                {isInitialSetup ? 'Master Admin Configured' : 'Admin Account Created'}
              </h3>
              <p className="text-sm text-slate-300 mb-6">
                {isInitialSetup
                  ? 'Your administrator account has been initialized in PostgreSQL. You can now log in immediately.'
                  : 'Your administrator account has been created. You can now proceed to login.'}
              </p>
              <Button
                variant="primary"
                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={() => navigate('/login')}
              >
                Proceed to Login
              </Button>
            </div>
          ) : (
            <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
              {error && (
                <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-3.5 flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                  <p className="text-sm text-rose-300">{error}</p>
                </div>
              )}

              <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-700 text-xs text-slate-400">
                This is a one-time initial setup to configure your system's master Administrator. Once created, additional users can be registered and approved.
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    First Name <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="text"
                    {...register('firstName', { required: 'First name is required' })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="System"
                  />
                  {errors.firstName && (
                    <p className="text-xs text-rose-400 mt-1">{errors.firstName.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Last Name <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="text"
                    {...register('lastName', { required: 'Last name is required' })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="Admin"
                  />
                  {errors.lastName && (
                    <p className="text-xs text-rose-400 mt-1">{errors.lastName.message}</p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Admin Username <span className="text-rose-400">*</span>
                </label>
                <input
                  type="text"
                  {...register('username', {
                    required: 'Username is required',
                    minLength: { value: 3, message: 'Minimum 3 characters' },
                  })}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  placeholder="admin"
                />
                {errors.username && (
                  <p className="text-xs text-rose-400 mt-1">{errors.username.message}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Admin Email <span className="text-rose-400">*</span>
                </label>
                <input
                  type="email"
                  {...register('email', {
                    required: 'Email is required',
                    pattern: {
                      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                      message: 'Invalid email address',
                    },
                  })}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  placeholder="admin@company.com"
                />
                {errors.email && (
                  <p className="text-xs text-rose-400 mt-1">{errors.email.message}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Admin Password <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    {...register('password', {
                      required: 'Password is required',
                      minLength: { value: 6, message: 'Password must be at least 6 characters' },
                    })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500 pr-10"
                    placeholder="••••••••"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-xs text-rose-400 mt-1">{errors.password.message}</p>
                )}
              </div>

              <div className="pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full bg-emerald-600 hover:bg-emerald-700 text-white"
                  isLoading={isSubmitting}
                >
                  <ShieldCheck className="w-4 h-4 mr-2" />
                  Initialize Master Admin
                </Button>
              </div>

              <div className="text-center pt-3 border-t border-slate-700">
                <Link
                  to="/login"
                  className="inline-flex items-center text-xs font-medium text-slate-400 hover:text-white"
                >
                  <ArrowLeft className="w-3.5 h-3.5 mr-1" />
                  Back to Login
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default SetupAdmin;
