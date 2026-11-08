import axiosClient from './axiosClient';
import { ApiResponse, MealRecord, MealStatus, PageResponse, QuickMealCheckResponse, RecordMealRequest, UpdateMealRecordRequest } from '../types';

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

  updateMealRecord: async (id: number, data: UpdateMealRecordRequest) => {
    const res = await axiosClient.put<ApiResponse<MealRecord>>(`/meals/${id}`, data);
    return res.data.data;
  },

  deleteMealRecord: async (id: number) => {
    const res = await axiosClient.delete<ApiResponse<void>>(`/meals/${id}`);
    return res.data;
  },

  // ---- ADMIN data maintenance (summary -> CSV backup -> confirmed delete) ----

  maintenanceSummary: async (range?: { startDate: string; endDate: string }) => {
    const res = await axiosClient.get<ApiResponse<MealDataSummary>>('/meals/maintenance/summary', { params: range });
    return res.data.data;
  },

  /** Downloads the full CSV backup and returns the saved filename. */
  downloadMaintenanceBackup: async (range?: { startDate: string; endDate: string }) => {
    const res = await axiosClient.get('/meals/maintenance/backup', { params: range, responseType: 'blob' });
    const match = /filename="?([^"]+)"?/i.exec(res.headers['content-disposition'] || '');
    const filename = match ? match[1] : 'meal_records_backup.csv';
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    return filename;
  },

  maintenanceDelete: async (
    expectedCount: number,
    range?: { startDate: string; endDate: string },
    employees?: { expectedEmployees: number }
  ) => {
    const res = await axiosClient.delete<ApiResponse<MealDataSummary & { deletedRecords: number; deletedEmployees?: number }>>('/meals/maintenance', {
      params: {
        ...range,
        expectedCount,
        confirm: 'DELETE',
        ...(employees ? { includeEmployees: true, expectedEmployees: employees.expectedEmployees } : {}),
      },
    });
    return res.data;
  },
};

export interface MealDataSummary {
  totalRecords: number;
  ateCount: number;
  didNotEatCount: number;
  totalAmount: number;
  firstMealDate: string | null;
  lastMealDate: string | null;
  /** All employees (any status). */
  employees: number;
}
