import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { UtensilsCrossed, User, ArrowLeft, CheckCircle2, AlertCircle, KeyRound, ArrowRight } from 'lucide-react';
import { authApi } from '../api/authApi';
import { ForgotPasswordRequest } from '../types';
import Button from '../components/common/Button';

const ForgotPassword: React.FC = () => {
  const [error, setError] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [resetData, setResetData] = useState<{ resetToken: string; email: string; username: string } | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordRequest>();

  const onSubmit = async (data: ForgotPasswordRequest) => {
    setError(null);
    try {
      const res = await authApi.forgotPassword(data);
      if (res.data) {
        setResetData(res.data);
      }
      setIsSubmitted(true);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to submit password reset request. Please check the username/email.';
      setError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-8 sm:py-12 px-3 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="mx-auto h-14 w-14 sm:h-16 sm:w-16 bg-blue-600 rounded-2xl flex items-center justify-center shadow-lg shadow-blue-500/20">
          <UtensilsCrossed className="h-8 w-8 sm:h-9 sm:w-9 text-white" />
        </div>
        <h2 className="mt-4 sm:mt-6 text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Recover Password
        </h2>
        <p className="mt-1.5 sm:mt-2 text-xs sm:text-sm text-slate-400">
          Enter your registered username or email to reset your credentials
        </p>
      </div>

      <div className="mt-6 sm:mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-slate-800 py-6 px-4.5 sm:py-8 sm:px-10 shadow-2xl rounded-2xl border border-slate-700">
          {isSubmitted && resetData ? (
            <div className="text-center py-2">
              <div className="mx-auto flex items-center justify-center h-14 w-14 rounded-full bg-emerald-500/10 text-emerald-400 mb-4 border border-emerald-500/20">
                <CheckCircle2 className="h-8 w-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-1">Account Verified</h3>
              <p className="text-sm text-slate-300 mb-5">
                Reset authorization generated for <strong className="text-blue-400">@{resetData.username}</strong> ({resetData.email}).
              </p>

              <div className="p-4 bg-slate-900/80 rounded-xl border border-blue-500/30 text-left mb-6 space-y-3">
                <div className="flex items-center gap-2 text-xs font-semibold text-blue-400">
                  <KeyRound className="w-4 h-4 shrink-0" />
                  <span>Ready to Set New Password</span>
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Click the button below to immediately choose and confirm your new secure password.
                </p>
                <Link to={`/reset-password?token=${resetData.resetToken}`} className="block">
                  <Button variant="primary" className="w-full py-2.5 text-sm gap-2">
                    <span>Proceed to Set New Password</span>
                    <ArrowRight className="w-4 h-4" />
                  </Button>
                </Link>
              </div>

              <div className="text-center">
                <Link to="/login" className="inline-flex items-center text-xs font-medium text-slate-400 hover:text-white">
                  <ArrowLeft className="w-3.5 h-3.5 mr-1" />
                  Return to Login
                </Link>
              </div>
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
                  Username or Registered Email <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type="text"
                    {...register('email', {
                      required: 'Username or Email is required',
                    })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 pl-10"
                    placeholder="Enter your username or email"
                  />
                  <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
                {errors.email && (
                  <p className="text-xs text-rose-400 mt-1">{errors.email.message}</p>
                )}
              </div>

              <div className="pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full py-2.5 text-sm"
                  isLoading={isSubmitting}
                >
                  Verify & Reset Password
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

export default ForgotPassword;
