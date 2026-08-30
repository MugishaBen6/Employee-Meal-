import axiosClient from './axiosClient';
import {
  ApiResponse,
  LoginResponse,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  SetupAdminRequest,
  User,
} from '../types';

export const authApi = {
  getSetupStatus: async () => {
    const response = await axiosClient.get<ApiResponse<{ setupNeeded: boolean }>>('/auth/setup-status');
    return response.data.data;
  },

  setupInitialAdmin: async (data: SetupAdminRequest) => {
    const response = await axiosClient.post<ApiResponse<User>>('/auth/setup-admin', data);
    return response.data;
  },

  login: async (credentials: { usernameOrEmail: string; password: string }) => {
    const response = await axiosClient.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
    return response.data;
  },

  register: async (data: RegisterRequest) => {
    const response = await axiosClient.post<ApiResponse<User>>('/auth/register', data);
    return response.data;
  },

  forgotPassword: async (data: ForgotPasswordRequest) => {
    const response = await axiosClient.post<ApiResponse<void>>('/auth/forgot-password', data);
    return response.data;
  },

  resetPassword: async (data: ResetPasswordRequest) => {
    const response = await axiosClient.post<ApiResponse<void>>('/auth/reset-password', data);
    return response.data;
  },
};
