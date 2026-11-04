import React, { useState, Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Navbar } from './Navbar';

const PageFallback = () => (
  <div className="flex items-center justify-center min-h-[420px] w-full" role="status" aria-label="Loading">
    <div className="flex flex-col items-center gap-3">
      <div className="relative h-10 w-10">
        <div className="absolute inset-0 rounded-full border-[3px] border-indigo-100" />
        <div className="absolute inset-0 rounded-full border-[3px] border-indigo-600 border-t-transparent animate-spin" />
      </div>
      <p className="text-xs text-slate-500 font-medium tracking-wide">Loading…</p>
    </div>
  </div>
);

export const AppLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(false);

  return (
    <div className="min-h-screen app-canvas flex w-full overflow-x-hidden">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="flex-1 lg:pl-64 flex flex-col min-w-0 w-full overflow-x-hidden">
        <Navbar onToggleSidebar={() => setSidebarOpen(!sidebarOpen)} />
        <main className="flex-1 px-3.5 py-4 sm:p-6 lg:p-8 max-w-[1400px] w-full mx-auto">
          <Suspense fallback={<PageFallback />}>
            <div className="animate-fadeIn">
              <Outlet />
            </div>
          </Suspense>
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
