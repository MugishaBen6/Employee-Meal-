import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { UtensilsCrossed, Lock, User, AlertCircle, ShieldCheck, UserPlus, Eye, EyeOff } from 'lucide-react';
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
  const [setupNeeded, setSetupNeeded] = useState(false);

  const from = location.state?.from?.pathname || '/dashboard';

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormInputs>();

  useEffect(() => {
    const checkSetup = async () => {
      try {
        const res = await authApi.getSetupStatus();
        setSetupNeeded(res.setupNeeded);
      } catch (err) {
        // Backend not reachable or error
      }
    };
    checkSetup();
  }, []);

  const onSubmit = async (data: LoginFormInputs) => {
    setError(null);
    try {
      await login(data.usernameOrEmail, data.password);
      navigate(from, { replace: true });
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Invalid username/email or password';
      setError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="mx-auto h-16 w-16 bg-blue-600 rounded-2xl flex items-center justify-center shadow-lg shadow-blue-500/20">
          <UtensilsCrossed className="h-9 w-9 text-white" />
        </div>
        <h2 className="mt-6 text-3xl font-extrabold text-white tracking-tight">
          Employee Meal System
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          Sign in to access meal tracking & reporting
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-slate-800 py-8 px-6 shadow-2xl rounded-2xl sm:px-10 border border-slate-700">
          {setupNeeded && (
            <div className="mb-5 p-3.5 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-start gap-3">
              <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-semibold text-emerald-300">Initial Setup Required</p>
                <p className="text-xs text-slate-300 mt-0.5">
                  No administrator account exists yet.
                </p>
                <Link
                  to="/setup-admin"
                  className="inline-block mt-2 text-xs font-semibold text-emerald-400 hover:text-emerald-300 underline"
                >
                  Configure Master Admin Account &rarr;
                </Link>
              </div>
            </div>
          )}

          <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
            {error && (
              <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-3.5 flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                <p className="text-sm text-rose-300">{error}</p>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Username or Email
              </label>
              <div className="relative">
                <input
                  type="text"
                  {...register('usernameOrEmail', { required: 'Username or Email is required' })}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 pl-10"
                  placeholder="Enter your username or email"
                  autoComplete="username"
                />
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              </div>
              {errors.usernameOrEmail && (
                <p className="text-xs text-rose-400 mt-1">{errors.usernameOrEmail.message}</p>
              )}
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-300">
                  Password
                </label>
                <Link
                  to="/forgot-password"
                  className="text-xs font-medium text-blue-400 hover:text-blue-300 transition-colors"
                >
                  Forgot Password?
                </Link>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  {...register('password', { required: 'Password is required' })}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 pl-10 pr-10"
                  placeholder="Enter your password"
                  autoComplete="current-password"
                />
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="text-xs text-rose-400 mt-1">{errors.password.message}</p>
              )}
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full py-2.5 text-sm"
              isLoading={isSubmitting}
            >
              Sign In
            </Button>

            <div className="pt-4 border-t border-slate-700 text-center">
              <p className="text-xs text-slate-400">
                Don't have an account?{' '}
                <Link
                  to="/register"
                  className="font-semibold text-blue-400 hover:text-blue-300 inline-flex items-center gap-1"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  Create Account
                </Link>
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Login;
