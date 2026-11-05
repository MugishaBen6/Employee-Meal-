import React, { useEffect, useState } from 'react';
import { auditApi } from '../api/auditApi';
import { AuditLog, PageResponse } from '../types';
import { Card } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { Skeleton } from '../components/common/Skeleton';
import { Activity, Search, ChevronLeft, ChevronRight, ShieldCheck } from 'lucide-react';
import { PageHero, HeroStat, Toolbar, fmtNumber } from '../components/dashboard/DashboardKit';

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
    <div className="space-y-5 sm:space-y-6">
      <PageHero
        icon={ShieldCheck}
        eyebrow="Security · Audit trail"
        title="Activity & Audit Logs"
        subtitle="Complete system activity audit trail for Managing Director & Security Admin"
        stats={
          <>
            <HeroStat label="Total entries" value={logs ? fmtNumber(logs.totalElements) : '—'} />
            <HeroStat label="Pages" value={logs ? fmtNumber(logs.totalPages) : '—'} sub="15 per page" />
            <HeroStat label="User filter" value={username ? username : 'All'} />
            <HeroStat label="Action filter" value={action ? action : 'All'} />
          </>
        }
      />

      <Toolbar>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            placeholder="Filter by Username..."
            value={username}
            onChange={(e) => {
              setUsername(e.target.value);
              setPage(0);
            }}
            className="w-full h-11 pl-10 pr-3.5 rounded-xl border border-slate-200 bg-white text-sm text-slate-800 placeholder:text-slate-400 hover:border-slate-300 focus:outline-none"
          />
          </div>
          <div className="relative">
          <Activity className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            placeholder="Filter by Action (RECORD_MEAL, UPDATE_EMPLOYEE...)..."
            value={action}
            onChange={(e) => {
              setAction(e.target.value);
              setPage(0);
            }}
            className="w-full h-11 pl-10 pr-3.5 rounded-xl border border-slate-200 bg-white text-sm text-slate-800 placeholder:text-slate-400 hover:border-slate-300 focus:outline-none"
          />
          </div>
        </div>
      </Toolbar>

      <Card className="p-0 overflow-hidden shadow-sm">
        {/* Mobile: one card per audit entry (same data as the table) */}
        <div className="md:hidden">
          {loading || !logs ? (
            <div className="p-3.5 space-y-2.5">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-[96px] w-full rounded-2xl" />
              ))}
            </div>
          ) : logs.content.length === 0 ? (
            <p className="py-12 text-center text-slate-400 text-sm">No audit logs found matching the criteria.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {logs.content.map((log) => (
                <li key={log.id} className="p-4">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="font-semibold text-slate-900 text-sm truncate">{log.username}</span>
                      <Badge variant="info">{log.userRole}</Badge>
                    </div>
                    <span className="text-[11px] font-mono text-slate-400 whitespace-nowrap">
                      {new Date(log.timestamp).toLocaleString()}
                    </span>
                  </div>
                  <p className="mt-1.5 text-xs font-mono font-semibold text-indigo-700">
                    {log.action}
                    <span className="font-normal text-slate-400"> · {log.entityType} ({log.entityId || '-'})</span>
                  </p>
                  <p className="mt-1 text-[13px] text-slate-600 leading-snug break-words">{log.description}</p>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="hidden md:block overflow-x-auto touch-scroll">
          <table className="w-full text-left text-sm min-w-[720px]">
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
                  <td colSpan={6} className="py-12 text-center text-slate-400 text-sm">No audit logs found matching the criteria.</td>
                </tr>
              ) : (
                logs.content.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-bold text-slate-900 font-mono text-xs">{log.username}</td>
                    <td className="py-3.5 px-4 whitespace-nowrap"><Badge variant="info">{log.userRole}</Badge></td>
                    <td className="py-3.5 px-4 font-mono font-semibold text-xs text-indigo-700 whitespace-nowrap">{log.action}</td>
                    <td className="py-3.5 px-4 text-xs font-mono text-slate-500 whitespace-nowrap">{log.entityType} ({log.entityId || '-'})</td>
                    <td className="py-3.5 px-4 text-slate-700 text-xs max-w-xs truncate" title={log.description}>{log.description}</td>
                    <td className="py-3.5 px-4 text-right text-xs font-mono text-slate-400 whitespace-nowrap">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {logs && logs.totalPages > 1 && (
          <div className="p-4 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500 bg-slate-50/50">
            <div>
              Showing page <span className="font-semibold text-slate-800">{page + 1}</span> of <span className="font-semibold text-slate-800">{logs.totalPages}</span> ({logs.totalElements} total entries)
            </div>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0 || loading}
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-medium transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
                Previous
              </button>
              <button
                disabled={page >= logs.totalPages - 1 || loading}
                onClick={() => setPage((prev) => prev + 1)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-medium transition-colors"
              >
                Next
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};

export default AuditLogs;

