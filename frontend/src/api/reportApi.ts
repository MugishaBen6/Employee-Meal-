import axiosClient from './axiosClient';
import { ApiResponse, DailyReportSummary } from '../types';

export const reportApi = {
  getDailyReport: async (date?: string, department?: string) => {
    const res = await axiosClient.get<ApiResponse<DailyReportSummary>>('/reports/daily', {
      params: { date, department },
    });
    return res.data.data;
  },

  downloadExcel: async (date?: string, department?: string) => {
    const res = await axiosClient.get('/reports/daily/excel', {
      params: { date, department },
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Daily_Meal_Report_${date || new Date().toISOString().split('T')[0]}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  },

  downloadPdf: async (date?: string, department?: string) => {
    const res = await axiosClient.get('/reports/daily/pdf', {
      params: { date, department },
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Daily_Meal_Report_${date || new Date().toISOString().split('T')[0]}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  },
};
