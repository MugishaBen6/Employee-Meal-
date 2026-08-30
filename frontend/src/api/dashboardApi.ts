import axiosClient from './axiosClient';
import { ApiResponse, DashboardStats } from '../types';

export const dashboardApi = {
  getStatistics: async () => {
    const res = await axiosClient.get<ApiResponse<DashboardStats>>('/dashboard/statistics');
    return res.data.data;
  },
};
