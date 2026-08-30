import axiosClient from './axiosClient';
import { ApiResponse, AuditLog, PageResponse } from '../types';

export const auditApi = {
  getAuditLogs: async (params?: {
    username?: string;
    userRole?: string;
    action?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }) => {
    const res = await axiosClient.get<ApiResponse<PageResponse<AuditLog>>>('/audit-logs', { params });
    return res.data.data;
  },
};
