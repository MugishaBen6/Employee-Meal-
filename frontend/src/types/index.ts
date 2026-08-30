export type Role = 'ADMIN' | 'MANAGING_DIRECTOR' | 'ACCOUNTANT' | 'HR';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING_APPROVAL' | 'REJECTED';

export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export type MealStatus = 'ATE' | 'DID_NOT_EAT';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: Role;
  status: UserStatus;
  createdAt: string;
}

export interface Employee {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  department: string;
  position: string;
  phone: string;
  email?: string;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeAttendance {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  telephone?: string;
  email?: string;
  position?: string;
  department: string;
  status: EmployeeStatus;
  mealDate: string;
  mealStatus: 'ATE' | 'DID_NOT_EAT' | 'NOT_RECORDED';
  amount: number | null;
  currency: string;
  mealRecordId?: number | null;
  recordedBy?: string | null;
  mealRecordedAt?: string | null;
}

export interface EmployeeAttendanceSummary {
  totalActiveEmployees: number;
  ateCount: number;
  didNotEatCount: number;
  notRecordedCount: number;
  totalMealCost: number;
  currency: string;
}

export interface EmployeeAttendancePageResponse {
  summary: EmployeeAttendanceSummary;
  employees: PageResponse<EmployeeAttendance>;
}

export interface CreateEmployeeRequest {
  employeeCode: string;
  employeeName: string;
  phone: string;
  position: string;
  mealStatus: 'ATE' | 'DID_NOT_EAT';
  amount: number;
  mealDate?: string;
  department?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
}

export interface UpdateEmployeeRequest {
  employeeName?: string;
  firstName?: string;
  lastName?: string;
  department?: string;
  position: string;
  phone: string;
  email?: string;
  status?: EmployeeStatus;
}

export interface MealRecord {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  department: string;
  mealDate: string;
  mealStatus: MealStatus;
  amount: number;
  recordedBy: string;
  createdAt: string;
}

export interface RecordMealRequest {
  employeeId: number;
  mealDate?: string;
  mealStatus: MealStatus;
  amount?: number;
}

export interface QuickMealCheck {
  employee: Employee;
  alreadyRecordedToday: boolean;
  todayRecord: MealRecord | null;
  defaultMealPrice: number;
  targetDate: string;
}

export type QuickMealCheckResponse = QuickMealCheck;

export interface ExpenseChartData {
  date: string;
  formattedDate: string;
  amount: number;
  ateCount: number;
  didNotEatCount: number;
}

export interface DepartmentMealStats {
  department: string;
  totalEmployees: number;
  ateCount: number;
  didNotEatCount: number;
  totalAmount: number;
}

export interface AuditLog {
  id: number;
  userId?: number;
  username: string;
  userRole: string;
  action: string;
  entityType?: string;
  entityId?: string;
  description: string;
  ipAddress?: string;
  timestamp: string;
}

export interface Setting {
  id: number;
  settingKey: string;
  settingValue: string;
  description?: string;
}

export interface DashboardStats {
  totalEmployees: number;
  ateToday: number;
  didNotEatToday: number;
  todayTotalCost: number;
  thisWeekTotalCost: number;
  thisMonthTotalCost: number;
  averageMealCostToday: number;
  currency: string;
  dailyExpenditures: ExpenseChartData[];
  departmentStats: DepartmentMealStats[];
  recentActivities: AuditLog[];
}

export interface DailyReportSummary {
  reportDate: string;
  formattedReportDate: string;
  companyName: string;
  totalEmployees: number;
  ateCount: number;
  didNotEatCount: number;
  totalExpenditure: number;
  averageMealCost: number;
  currency: string;
  records: MealRecord[];
}

export interface PageResponse<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  user: User;
}

export type JwtResponse = LoginResponse;

export interface SetupAdminRequest {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ApproveUserRequest {
  role: Role;
}
