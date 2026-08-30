import axiosClient from './axiosClient';
import { ApiResponse, MealRecord, MealStatus, PageResponse, QuickMealCheckResponse, RecordMealRequest } from '../types';

export const mealApi = {
  recordMeal: async (data: RecordMealRequest) => {
    const res = await axiosClient.post<ApiResponse<MealRecord>>('/meals', data);
    return res.data;
  },

  quickCheck: async (query: string, date?: string) => {
    const res = await axiosClient.get<ApiResponse<QuickMealCheckResponse>>('/meals/quick-check', {
      params: { query, date },
    });
    return res.data.data;
  },

  getMealRecords: async (params?: {
    startDate?: string;
    endDate?: string;
    employeeId?: number;
    department?: string;
    status?: MealStatus;
    recordedBy?: string;
    page?: number;
    size?: number;
  }) => {
    const res = await axiosClient.get<ApiResponse<PageResponse<MealRecord>>>('/meals', { params });
    return res.data.data;
  },

  getEmployeeMealHistory: async (employeeId: number) => {
    const res = await axiosClient.get<ApiResponse<MealRecord[]>>(`/meals/employee/${employeeId}`);
    return res.data.data;
  },

  updateMealRecord: async (id: number, data: { mealStatus: MealStatus; amount: number }) => {
    const res = await axiosClient.put<ApiResponse<MealRecord>>(`/meals/${id}`, data);
    return res.data.data;
  },

  deleteMealRecord: async (id: number) => {
    const res = await axiosClient.delete<ApiResponse<void>>(`/meals/${id}`);
    return res.data;
  },
};
