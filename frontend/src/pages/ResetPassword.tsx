import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { UtensilsCrossed, Lock, ArrowLeft, CheckCircle2, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { authApi } from '../api/authApi';
import { ResetPasswordRequest } from '../types';
import Button from '../components/common/Button';

const ResetPassword: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') || '';

  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordRequest>({
    defaultValues: { token },
  });

  const newPassword = watch('newPassword');

  const onSubmit = async (data: ResetPasswordRequest) => {
    if (!token) {
      setError('Password reset token is missing from the URL. Please request a new link.');
      return;
    }
    setError(null);
    try {
      await authApi.resetPassword({ ...data, token });
      setIsSuccess(true);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to reset password. The link may have expired or was already used.';
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
          Set New Password
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          Choose a new secure password for your account
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-slate-800 py-8 px-6 shadow-2xl rounded-2xl sm:px-10 border border-slate-700">
          {!token ? (
            <div className="text-center py-4">
              <div className="mx-auto flex items-center justify-center h-14 w-14 rounded-full bg-rose-500/10 text-rose-400 mb-4 border border-rose-500/20">
                <AlertCircle className="h-8 w-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Invalid Reset Link</h3>
              <p className="text-sm text-slate-300 mb-6">
                No password reset token was provided in the link. Please request a new recovery email.
              </p>
              <Link to="/forgot-password">
                <Button variant="primary" className="w-full">
                  Request New Reset Link
                </Button>
              </Link>
            </div>
          ) : isSuccess ? (
            <div className="text-center py-4">
              <div className="mx-auto flex items-center justify-center h-14 w-14 rounded-full bg-emerald-500/10 text-emerald-400 mb-4 border border-emerald-500/20">
                <CheckCircle2 className="h-8 w-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Password Reset Successful</h3>
              <p className="text-sm text-slate-300 mb-6">
                Your password has been updated in the system. You can now log in using your new credentials.
              </p>
              <Button
                variant="primary"
                className="w-full"
                onClick={() => navigate('/login')}
              >
                Log In Now
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

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  New Password <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    {...register('newPassword', {
                      required: 'New password is required',
                      minLength: { value: 6, message: 'Password must be at least 6 characters' },
                    })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
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
                {errors.newPassword && (
                  <p className="text-xs text-rose-400 mt-1">{errors.newPassword.message}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Confirm New Password <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    {...register('confirmPassword', {
                      required: 'Please confirm your new password',
                      validate: (val) => val === newPassword || 'Passwords do not match',
                    })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
                    placeholder="••••••••"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300"
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p className="text-xs text-rose-400 mt-1">{errors.confirmPassword.message}</p>
                )}
              </div>

              <div className="pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full"
                  isLoading={isSubmitting}
                >
                  <Lock className="w-4 h-4 mr-2" />
                  Update Password
                </Button>
              </div>

              <div className="text-center pt-3 border-t border-slate-700">
                <Link
                  to="/login"
                  className="inline-flex items-center text-xs font-medium text-slate-400 hover:text-white"
                >
                  <ArrowLeft className="w-3.5 h-3.5 mr-1" />
                  Cancel and Return to Login
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default ResetPassword;
