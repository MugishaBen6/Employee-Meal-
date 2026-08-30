import axiosClient from './axiosClient';
import { ApiResponse, PageResponse, User, Role, UserStatus, ApproveUserRequest } from '../types';

export const userApi = {
  searchUsers: async (params?: {
    query?: string;
    role?: Role;
    status?: UserStatus;
    page?: number;
    size?: number;
  }) => {
    const response = await axiosClient.get<ApiResponse<PageResponse<User>>>('/users', { params });
    return response.data.data;
  },

  getUserById: async (id: number) => {
    const response = await axiosClient.get<ApiResponse<User>>(`/users/${id}`);
    return response.data.data;
  },

  createUser: async (data: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    role: Role;
  }) => {
    const response = await axiosClient.post<ApiResponse<User>>('/users', data);
    return response.data.data;
  },

  updateUser: async (
    id: number,
    data: {
      email: string;
      firstName: string;
      lastName: string;
      role: Role;
      status: UserStatus;
    }
  ) => {
    const response = await axiosClient.put<ApiResponse<User>>(`/users/${id}`, data);
    return response.data.data;
  },

  approveUser: async (id: number, data?: ApproveUserRequest) => {
    const response = await axiosClient.patch<ApiResponse<User>>(`/users/${id}/approve`, data || { role: 'HR' });
    return response.data.data;
  },

  rejectUser: async (id: number) => {
    const response = await axiosClient.patch<ApiResponse<void>>(`/users/${id}/reject`);
    return response.data;
  },

  changePassword: async (id: number, newPassword: string) => {
    const response = await axiosClient.patch<ApiResponse<void>>(`/users/${id}/password`, { newPassword });
    return response.data;
  },

  toggleStatus: async (id: number) => {
    const response = await axiosClient.patch<ApiResponse<void>>(`/users/${id}/toggle-status`);
    return response.data;
  },
};
