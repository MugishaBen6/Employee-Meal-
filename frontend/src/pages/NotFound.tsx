import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/common/Button';

export const NotFound: React.FC = () => {
  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-4">
      <h1 className="text-6xl font-bold text-indigo-600">404</h1>
      <h2 className="text-2xl font-bold text-slate-800 mt-2">Page Not Found</h2>
      <p className="text-sm text-slate-500 max-w-sm mt-1">
        The page you are looking for does not exist or you do not have authorization to view it.
      </p>
      <Link to="/dashboard" className="mt-6">
        <Button>Back to Dashboard</Button>
      </Link>
    </div>
  );
};

export default NotFound;
