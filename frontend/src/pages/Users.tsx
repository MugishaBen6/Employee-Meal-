import React, { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import {
  Users as UsersIcon,
  UserPlus,
  Search,
  KeyRound,
  UserX,
  UserCheck,
  Edit2,
  CheckCircle,
  XCircle,
  AlertCircle,
  ShieldCheck,
} from 'lucide-react';
import { userApi } from '../api/userApi';
import { User, Role, UserStatus } from '../types';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import Modal from '../components/common/Modal';
import Skeleton from '../components/common/Skeleton';
import Toast from '../components/common/Toast';

interface CreateUserInputs {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  role: Role;
}

interface UpdateUserInputs {
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  status: UserStatus;
}

const Users: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<UserStatus | ''>('');
  const [roleFilter, setRoleFilter] = useState<Role | ''>('');
  const [isLoading, setIsLoading] = useState(true);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [approveRole, setApproveRole] = useState<Role>('HR');
  const [newPassword, setNewPassword] = useState('');

  // Toast
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const {
    register: registerCreate,
    handleSubmit: handleSubmitCreate,
    reset: resetCreate,
    formState: { errors: errorsCreate, isSubmitting: isSubmittingCreate },
  } = useForm<CreateUserInputs>();

  const {
    register: registerEdit,
    handleSubmit: handleSubmitEdit,
    reset: resetEdit,
    formState: { errors: errorsEdit, isSubmitting: isSubmittingEdit },
  } = useForm<UpdateUserInputs>();

  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await userApi.searchUsers({
        query: searchQuery || undefined,
        role: roleFilter || undefined,
        status: statusFilter || undefined,
        page,
        size: pageSize,
      });
      setUsers(data.content);
      setTotalElements(data.totalElements);
      setTotalPages(data.totalPages);
    } catch (err: any) {
      setToast({ message: 'Failed to load users', type: 'error' });
    } finally {
      setIsLoading(false);
    }
  }, [searchQuery, roleFilter, statusFilter, page, pageSize]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleCreateUser = async (data: CreateUserInputs) => {
    try {
      await userApi.createUser(data);
      setToast({ message: `User ${data.username} created successfully`, type: 'success' });
      setIsCreateModalOpen(false);
      resetCreate();
      fetchUsers();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to create user';
      setToast({ message: msg, type: 'error' });
    }
  };

  const handleEditUser = async (data: UpdateUserInputs) => {
    if (!selectedUser) return;
    try {
      await userApi.updateUser(selectedUser.id, data);
      setToast({ message: `User ${selectedUser.username} updated successfully`, type: 'success' });
      setIsEditModalOpen(false);
      fetchUsers();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to update user';
      setToast({ message: msg, type: 'error' });
    }
  };

  const handleApproveUser = async () => {
    if (!selectedUser) return;
    try {
      await userApi.approveUser(selectedUser.id, { role: approveRole });
      setToast({ message: `User ${selectedUser.username} approved as ${approveRole}`, type: 'success' });
      setIsApproveModalOpen(false);
      fetchUsers();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to approve user';
      setToast({ message: msg, type: 'error' });
    }
  };

  const handleRejectUser = async (user: User) => {
    if (!window.confirm(`Are you sure you want to reject registration for @${user.username}?`)) return;
    try {
      await userApi.rejectUser(user.id);
      setToast({ message: `Registration for @${user.username} rejected`, type: 'success' });
      fetchUsers();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to reject user';
      setToast({ message: msg, type: 'error' });
    }
  };

  const handleChangePassword = async () => {
    if (!selectedUser || !newPassword) return;
    if (newPassword.length < 6) {
      setToast({ message: 'Password must be at least 6 characters', type: 'error' });
      return;
    }
    try {
      await userApi.changePassword(selectedUser.id, newPassword);
      setToast({ message: `Password reset successfully for ${selectedUser.username}`, type: 'success' });
      setIsPasswordModalOpen(false);
      setNewPassword('');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to reset password';
      setToast({ message: msg, type: 'error' });
    }
  };

  const handleToggleStatus = async (user: User) => {
    try {
      await userApi.toggleStatus(user.id);
      setToast({
        message: `User ${user.username} is now ${user.status === 'ACTIVE' ? 'Inactive' : 'Active'}`,
        type: 'success',
      });
      fetchUsers();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to toggle status';
      setToast({ message: msg, type: 'error' });
    }
  };

  const openEditModal = (user: User) => {
    setSelectedUser(user);
    resetEdit({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      status: user.status,
    });
    setIsEditModalOpen(true);
  };

  const openApproveModal = (user: User) => {
    setSelectedUser(user);
    setApproveRole(user.role || 'HR');
    setIsApproveModalOpen(true);
  };

  const getStatusBadge = (status: UserStatus) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge variant="success">Active</Badge>;
      case 'PENDING_APPROVAL':
        return <Badge variant="warning">Pending Approval</Badge>;
      case 'REJECTED':
        return <Badge variant="danger">Rejected</Badge>;
      case 'INACTIVE':
        return <Badge variant="neutral">Inactive</Badge>;
      default:
        return <Badge variant="neutral">{status}</Badge>;
    }
  };

  const getRoleBadge = (role: Role) => {
    switch (role) {
      case 'ADMIN':
        return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-rose-100 text-rose-800">Admin</span>;
      case 'MANAGING_DIRECTOR':
        return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-purple-100 text-purple-800">Director</span>;
      case 'ACCOUNTANT':
        return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-800">Accountant</span>;
      case 'HR':
        return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 text-emerald-800">HR</span>;
      default:
        return <Badge variant="neutral">{role}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            <UsersIcon className="w-7 h-7 text-blue-600" />
            User Management
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage system access, approve pending registrations, assign roles and security
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsCreateModalOpen(true)}>
          <UserPlus className="w-4 h-4 mr-2" />
          Create New User
        </Button>
      </div>

      {/* Status Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-slate-200 pb-3">
        {[
          { label: 'All Users', value: '' },
          { label: 'Active', value: 'ACTIVE' },
          { label: 'Pending Approval', value: 'PENDING_APPROVAL' },
          { label: 'Inactive', value: 'INACTIVE' },
        ].map((tab) => (
          <button
            key={tab.value}
            onClick={() => {
              setStatusFilter(tab.value as any);
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === tab.value
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Filters Card */}
      <Card>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder="Search by name, username, or email..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500 pl-9"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          </div>

          <div className="w-full sm:w-48">
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value as any);
                setPage(0);
              }}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Roles</option>
              <option value="ADMIN">Admin</option>
              <option value="MANAGING_DIRECTOR">Managing Director</option>
              <option value="ACCOUNTANT">Accountant</option>
              <option value="HR">HR</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Users Table */}
      <Card>
        {isLoading ? (
          <div className="space-y-3 p-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : users.length === 0 ? (
          <div className="text-center py-12">
            <UsersIcon className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-700">No Users Found</h3>
            <p className="text-xs text-slate-400 mt-1">
              {searchQuery || roleFilter || statusFilter
                ? 'No users match your filter criteria.'
                : 'No users currently exist in the database.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-700">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Username</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-4 py-3 font-medium text-slate-900">
                      {u.firstName} {u.lastName}
                    </td>
                    <td className="px-4 py-3 text-slate-600 font-mono text-xs">
                      @{u.username}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{u.email}</td>
                    <td className="px-4 py-3">{getRoleBadge(u.role)}</td>
                    <td className="px-4 py-3">{getStatusBadge(u.status)}</td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {u.status === 'PENDING_APPROVAL' ? (
                          <>
                            <button
                              onClick={() => openApproveModal(u)}
                              title="Approve User & Assign Role"
                              className="px-2.5 py-1 text-xs font-semibold bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded-lg border border-emerald-200 flex items-center gap-1"
                            >
                              <CheckCircle className="w-3.5 h-3.5" />
                              Approve
                            </button>
                            <button
                              onClick={() => handleRejectUser(u)}
                              title="Reject Registration"
                              className="p-1 text-rose-500 hover:bg-rose-50 rounded-lg"
                            >
                              <XCircle className="w-4 h-4" />
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              onClick={() => openEditModal(u)}
                              title="Edit User"
                              className="p-1.5 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => {
                                setSelectedUser(u);
                                setNewPassword('');
                                setIsPasswordModalOpen(true);
                              }}
                              title="Reset Password"
                              className="p-1.5 text-slate-500 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors"
                            >
                              <KeyRound className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleToggleStatus(u)}
                              title={u.status === 'ACTIVE' ? 'Deactivate User' : 'Activate User'}
                              className={`p-1.5 rounded-lg transition-colors ${
                                u.status === 'ACTIVE'
                                  ? 'text-slate-400 hover:text-rose-600 hover:bg-rose-50'
                                  : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50'
                              }`}
                            >
                              {u.status === 'ACTIVE' ? (
                                <UserX className="w-4 h-4" />
                              ) : (
                                <UserCheck className="w-4 h-4" />
                              )}
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 text-xs text-slate-500">
            <span>
              Showing {users.length} of {totalElements} users
            </span>
            <div className="flex gap-1">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </Button>
              <span className="px-3 py-1 font-semibold text-slate-700">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Modal: Create User */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create New User Account"
      >
        <form onSubmit={handleSubmitCreate(handleCreateUser)} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                First Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...registerCreate('firstName', { required: 'First name is required' })}
                className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
                placeholder="John"
              />
              {errorsCreate.firstName && (
                <p className="text-xs text-rose-500 mt-1">{errorsCreate.firstName.message}</p>
              )}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Last Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...registerCreate('lastName', { required: 'Last name is required' })}
                className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
                placeholder="Doe"
              />
              {errorsCreate.lastName && (
                <p className="text-xs text-rose-500 mt-1">{errorsCreate.lastName.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Username <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              {...registerCreate('username', {
                required: 'Username is required',
                minLength: { value: 3, message: 'Minimum 3 characters' },
              })}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
              placeholder="jdoe"
            />
            {errorsCreate.username && (
              <p className="text-xs text-rose-500 mt-1">{errorsCreate.username.message}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Email <span className="text-rose-500">*</span>
            </label>
            <input
              type="email"
              {...registerCreate('email', { required: 'Email is required' })}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
              placeholder="jdoe@company.com"
            />
            {errorsCreate.email && (
              <p className="text-xs text-rose-500 mt-1">{errorsCreate.email.message}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Password <span className="text-rose-500">*</span>
            </label>
            <input
              type="password"
              {...registerCreate('password', {
                required: 'Password is required',
                minLength: { value: 6, message: 'Minimum 6 characters' },
              })}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
              placeholder="••••••••"
            />
            {errorsCreate.password && (
              <p className="text-xs text-rose-500 mt-1">{errorsCreate.password.message}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Role <span className="text-rose-500">*</span>
            </label>
            <select
              {...registerCreate('role', { required: 'Role is required' })}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
            >
              <option value="HR">HR Manager</option>
              <option value="ACCOUNTANT">Accountant</option>
              <option value="MANAGING_DIRECTOR">Managing Director</option>
              <option value="ADMIN">System Administrator</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmittingCreate}>
              Create User
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal: Edit User */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title={`Edit User: @${selectedUser?.username}`}
      >
        <form onSubmit={handleSubmitEdit(handleEditUser)} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">First Name</label>
              <input
                type="text"
                {...registerEdit('firstName', { required: 'First name is required' })}
                className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Last Name</label>
              <input
                type="text"
                {...registerEdit('lastName', { required: 'Last name is required' })}
                className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Email</label>
            <input
              type="email"
              {...registerEdit('email', { required: 'Email is required' })}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Role</label>
            <select
              {...registerEdit('role')}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800"
            >
              <option value="HR">HR Manager</option>
              <option value="ACCOUNTANT">Accountant</option>
              <option value="MANAGING_DIRECTOR">Managing Director</option>
              <option value="ADMIN">System Administrator</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Status</label>
            <select
              {...registerEdit('status')}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800"
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="PENDING_APPROVAL">Pending Approval</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={() => setIsEditModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmittingEdit}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal: Approve User */}
      <Modal
        isOpen={isApproveModalOpen}
        onClose={() => setIsApproveModalOpen(false)}
        title={`Approve User Registration: @${selectedUser?.username}`}
      >
        <div className="space-y-4">
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-600 space-y-1">
            <p><strong>Name:</strong> {selectedUser?.firstName} {selectedUser?.lastName}</p>
            <p><strong>Email:</strong> {selectedUser?.email}</p>
            <p><strong>Username:</strong> @{selectedUser?.username}</p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Assign System Role
            </label>
            <select
              value={approveRole}
              onChange={(e) => setApproveRole(e.target.value as Role)}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800 focus:ring-2 focus:ring-blue-500"
            >
              <option value="HR">HR Manager</option>
              <option value="ACCOUNTANT">Accountant</option>
              <option value="MANAGING_DIRECTOR">Managing Director</option>
              <option value="ADMIN">System Administrator</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={() => setIsApproveModalOpen(false)}>
              Cancel
            </Button>
            <Button type="button" variant="primary" onClick={handleApproveUser}>
              <ShieldCheck className="w-4 h-4 mr-1.5" />
              Approve & Activate Account
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Reset Password */}
      <Modal
        isOpen={isPasswordModalOpen}
        onClose={() => setIsPasswordModalOpen(false)}
        title={`Reset Password: @${selectedUser?.username}`}
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-500">
            Enter a new password for {selectedUser?.firstName} {selectedUser?.lastName}.
          </p>
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              New Password
            </label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full border border-slate-300 rounded-xl px-3 py-2 text-sm text-slate-800"
              placeholder="Minimum 6 characters"
            />
          </div>
          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={() => setIsPasswordModalOpen(false)}>
              Cancel
            </Button>
            <Button type="button" variant="primary" onClick={handleChangePassword}>
              Update Password
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Users;
