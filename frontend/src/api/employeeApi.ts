import axiosClient from './axiosClient';
import { ApiResponse, CreateEmployeeRequest, Employee, EmployeeAttendancePageResponse, EmployeeStatus, PageResponse, UpdateEmployeeRequest } from '../types';

export const employeeApi = {
  getEmployees: async (params?: { query?: string; department?: string; status?: EmployeeStatus; page?: number; size?: number }) => {
    const res = await axiosClient.get<ApiResponse<PageResponse<Employee>>>('/employees', { params });
    return res.data.data;
  },

  getAttendance: async (params?: {
    date?: string;
    query?: string;
    department?: string;
    mealStatus?: string;
    status?: EmployeeStatus;
    page?: number;
    size?: number;
  }) => {
    const res = await axiosClient.get<ApiResponse<EmployeeAttendancePageResponse>>('/employees/attendance', { params });
    return res.data.data;
  },

  quickSearch: async (query: string) => {
    const res = await axiosClient.get<ApiResponse<Employee[]>>('/employees/quick-search', { params: { query } });
    return res.data.data;
  },

  getDepartments: async () => {
    const res = await axiosClient.get<ApiResponse<string[]>>('/employees/departments');
    return res.data.data;
  },

  getById: async (id: number) => {
    const res = await axiosClient.get<ApiResponse<Employee>>(`/employees/${id}`);
    return res.data.data;
  },

  getByCode: async (code: string) => {
    const res = await axiosClient.get<ApiResponse<Employee>>(`/employees/code/${code}`);
    return res.data.data;
  },

  create: async (data: CreateEmployeeRequest) => {
    const res = await axiosClient.post<ApiResponse<Employee>>('/employees', data);
    return res.data.data;
  },

  update: async (id: number, data: UpdateEmployeeRequest) => {
    const res = await axiosClient.put<ApiResponse<Employee>>(`/employees/${id}`, data);
    return res.data.data;
  },

  deactivate: async (id: number) => {
    const res = await axiosClient.delete<ApiResponse<void>>(`/employees/${id}`);
    return res.data;
  },
};
