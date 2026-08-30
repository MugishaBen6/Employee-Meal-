import React, { useEffect, useState } from 'react';
import { auditApi } from '../api/auditApi';
import { AuditLog, PageResponse } from '../types';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { Skeleton } from '../components/common/Skeleton';
import { Activity, Search, ChevronLeft, ChevronRight } from 'lucide-react';

export const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<PageResponse<AuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [username, setUsername] = useState('');
  const [action, setAction] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    fetchAuditLogs();
  }, [username, action, page]);

  const fetchAuditLogs = async () => {
    setLoading(true);
    try {
      const data = await auditApi.getAuditLogs({
        username: username || undefined,
        action: action || undefined,
        page,
        size: 15,
      });
      setLogs(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Activity & Audit Logs</h2>
        <p className="text-sm text-slate-500 mt-1">Complete system activity audit trail for Managing Director & Security Admin</p>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <input
            type="text"
            placeholder="Filter by Username..."
            value={username}
            onChange={(e) => {
              setUsername(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 rounded-lg border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500"
          />
          <input
            type="text"
            placeholder="Filter by Action (RECORD_MEAL, UPDATE_EMPLOYEE...)..."
            value={action}
            onChange={(e) => {
              setAction(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 rounded-lg border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </Card>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-500 uppercase text-[11px] tracking-wider font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4">User</th>
                <th className="py-3.5 px-4">Role</th>
                <th className="py-3.5 px-4">Action</th>
                <th className="py-3.5 px-4">Entity</th>
                <th className="py-3.5 px-4">Description</th>
                <th className="py-3.5 px-4 text-right">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading || !logs ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="p-4"><Skeleton className="h-4 w-full" /></td>
                  </tr>
                ))
              ) : logs.content.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400 text-sm">No audit logs found.</td>
                </tr>
              ) : (
                logs.content.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-bold text-slate-900 font-mono">{log.username}</td>
                    <td className="py-3.5 px-4"><Badge variant="info">{log.userRole}</Badge></td>
                    <td className="py-3.5 px-4 font-mono font-semibold text-indigo-700">{log.action}</td>
                    <td className="py-3.5 px-4 text-xs font-mono text-slate-500">{log.entityType} ({log.entityId || '-'})</td>
                    <td className="py-3.5 px-4 text-slate-700 text-xs">{log.description}</td>
                    <td className="py-3.5 px-4 text-right text-xs font-mono text-slate-400">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

export default AuditLogs;

