import axiosClient from './axiosClient';
import { ApiResponse } from '../types';

export const expenseApi = {
  getSummary: async () => {
    const res = await axiosClient.get<ApiResponse<any>>('/expenses/summary');
    return res.data.data;
  },
};
