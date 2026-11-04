import axiosClient from './axiosClient';
import { ApiResponse, DailyReportSummary } from '../types';

/**
 * Reports for an inclusive period: From Date (startDate) to To Date (endDate), both YYYY-MM-DD.
 * The page data, the Excel file and the PDF use the same parameters and the same backend query,
 * so they always contain exactly the same records.
 */
const periodParams = (startDate: string, endDate: string, department?: string) => ({
  startDate,
  endDate,
  department,
});

/** Same naming rule as the backend; used only if the server filename header is unavailable. */
export const reportFileBaseName = (startDate: string, endDate: string): string =>
  startDate === endDate
    ? `Daily_Meal_Report_${startDate}`
    : `Meal_Report_${startDate}_to_${endDate}`;

const filenameFromHeader = (header: string | undefined, fallback: string): string => {
  const match = header?.match(/filename="?([^";]+)"?/i);
  return match ? match[1] : fallback;
};

const saveBlob = (data: BlobPart, type: string | undefined, filename: string) => {
  const url = window.URL.createObjectURL(new Blob([data], type ? { type } : undefined));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Delay so the browser has started the download before the URL is released.
  setTimeout(() => window.URL.revokeObjectURL(url), 1000);
};

export const reportApi = {
  getReport: async (startDate: string, endDate: string, department?: string) => {
    const res = await axiosClient.get<ApiResponse<DailyReportSummary>>('/reports/daily', {
      params: periodParams(startDate, endDate, department),
    });
    return res.data.data;
  },

  /** Downloads the Excel report and returns the saved filename. */
  downloadExcel: async (startDate: string, endDate: string, department?: string) => {
    const res = await axiosClient.get('/reports/daily/excel', {
      params: periodParams(startDate, endDate, department),
      responseType: 'blob',
    });
    const filename = filenameFromHeader(
      res.headers['content-disposition'],
      `${reportFileBaseName(startDate, endDate)}.xlsx`
    );
    saveBlob(res.data, undefined, filename);
    return filename;
  },

  /** Downloads the PDF report and returns the saved filename. */
  downloadPdf: async (startDate: string, endDate: string, department?: string) => {
    const res = await axiosClient.get('/reports/daily/pdf', {
      params: periodParams(startDate, endDate, department),
      responseType: 'blob',
    });
    const filename = filenameFromHeader(
      res.headers['content-disposition'],
      `${reportFileBaseName(startDate, endDate)}.pdf`
    );
    saveBlob(res.data, 'application/pdf', filename);
    return filename;
  },
};
