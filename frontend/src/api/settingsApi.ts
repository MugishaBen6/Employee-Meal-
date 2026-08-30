import axiosClient from './axiosClient';
import { ApiResponse, Setting } from '../types';

export const settingsApi = {
  getAll: async () => {
    const res = await axiosClient.get<ApiResponse<Setting[]>>('/settings');
    return res.data.data;
  },

  update: async (key: string, settingValue: string) => {
    const res = await axiosClient.put<ApiResponse<Setting>>(`/settings/${key}`, { settingValue });
    return res.data.data;
  },
};
