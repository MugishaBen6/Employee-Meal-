import axiosClient from './axiosClient';
import { ApiResponse, PageResponse } from '../types';

export type TrashItemType = 'EMPLOYEE' | 'MEAL_RECORD';

export interface TrashItem {
  id: number;
  itemType: TrashItemType;
  originalId: number;
  employeeId: number | null;
  employeeCode: string | null;
  employeeName: string | null;
  mealDate: string | null;
  mealStatus: 'ATE' | 'DID_NOT_EAT' | null;
  /** Meal records inside the item (an employee's meals, or 1). */
  mealCount: number;
  /** Sum of ATE amounts inside the item. */
  mealAmount: number;
  position: string | null;
  department: string | null;
  recordedBy: string | null;
  deletedBy: string;
  deletedAt: string;
  /** A meal record whose employee is not in the employee list (restore the employee first). */
  employeeMissing: boolean;
}

export interface TrashSummary {
  employees: number;
  mealRecords: number;
  mealsInsideEmployees: number;
  ateAmount: number;
}

export interface RestoreResult {
  restored: number;
  failed: number;
  results: { id: number; ok: boolean; message: string }[];
}

type Filters = { type?: TrashItemType | 'ALL'; query?: string };

const clean = (f: Filters) => ({
  type: f.type && f.type !== 'ALL' ? f.type : undefined,
  query: f.query?.trim() || undefined,
});

/** ADMIN only (enforced by the server). */
export const trashApi = {
  list: async (f: Filters & { page: number; size: number }) => {
    const res = await axiosClient.get<ApiResponse<PageResponse<TrashItem>>>('/trash', {
      params: { ...clean(f), page: f.page, size: f.size },
    });
    return res.data.data;
  },
  ids: async (f: Filters) => {
    const res = await axiosClient.get<ApiResponse<number[]>>('/trash/ids', { params: clean(f) });
    return res.data.data;
  },
  summary: async () => {
    const res = await axiosClient.get<ApiResponse<TrashSummary>>('/trash/summary');
    return res.data.data;
  },
  restore: async (ids: number[]) => {
    const res = await axiosClient.post<ApiResponse<RestoreResult>>('/trash/restore', ids);
    return res.data;
  },
  /** Irreversible. */
  purge: async (ids: number[]) => {
    const res = await axiosClient.post<ApiResponse<{ purged: number }>>('/trash/purge', ids, { params: { confirm: 'DELETE' } });
    return res.data;
  },
};
