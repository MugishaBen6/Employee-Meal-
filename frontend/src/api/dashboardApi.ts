import axiosClient from './axiosClient';
import { ApiResponse, DashboardStats, UserDashboardStats } from '../types';

export const dashboardApi = {
  getStatistics: async () => {
    const res = await axiosClient.get<ApiResponse<DashboardStats>>('/dashboard/statistics');
    return res.data.data;
  },

  getMyStats: async () => {
    const res = await axiosClient.get<ApiResponse<UserDashboardStats>>('/dashboard/my-stats');
    return res.data.data;
  },
};
