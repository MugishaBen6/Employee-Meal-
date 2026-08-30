import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { UtensilsCrossed, Mail, ArrowLeft, CheckCircle2, AlertCircle } from 'lucide-react';
import { authApi } from '../api/authApi';
import { ForgotPasswordRequest } from '../types';
import Button from '../components/common/Button';

const ForgotPassword: React.FC = () => {
  const [error, setError] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordRequest>();

  const onSubmit = async (data: ForgotPasswordRequest) => {
    setError(null);
    try {
      await authApi.forgotPassword(data);
      setSubmittedEmail(data.email);
      setIsSubmitted(true);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to submit password reset request.';
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
          Recover Password
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          Enter your registered email address to receive a secure recovery link
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-slate-800 py-8 px-6 shadow-2xl rounded-2xl sm:px-10 border border-slate-700">
          {isSubmitted ? (
            <div className="text-center py-4">
              <div className="mx-auto flex items-center justify-center h-14 w-14 rounded-full bg-blue-500/10 text-blue-400 mb-4 border border-blue-500/20">
                <CheckCircle2 className="h-8 w-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Check Your Email</h3>
              <p className="text-sm text-slate-300 mb-4">
                If an account exists with <strong className="text-blue-400">{submittedEmail}</strong>, a password reset link has been dispatched.
              </p>
              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-700 text-xs text-slate-400 text-left mb-6 space-y-1">
                <p>• The reset link is single-use and will expire in <strong>15 minutes</strong>.</p>
                <p>• Please check your Spam / Junk folder if you do not see it in your Inbox.</p>
              </div>

              <Link to="/login">
                <Button variant="outline" className="w-full">
                  <ArrowLeft className="w-4 h-4 mr-2" />
                  Return to Login
                </Button>
              </Link>
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
                  Registered Email Address <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    type="email"
                    {...register('email', {
                      required: 'Email address is required',
                      pattern: {
                        value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                        message: 'Invalid email address',
                      },
                    })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 pl-9"
                    placeholder="user@company.com"
                  />
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                </div>
                {errors.email && (
                  <p className="text-xs text-rose-400 mt-1">{errors.email.message}</p>
                )}
              </div>

              <div className="pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full"
                  isLoading={isSubmitting}
                >
                  Send Reset Link
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
