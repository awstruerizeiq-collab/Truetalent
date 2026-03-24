import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import axios from '../api/axiosConfig';

function Sidebar() {
  const navigate = useNavigate();

  const navItems = [
    { name: 'Dashboard', path: '/dashboard' },
    { name: 'Users', path: '/users' },
    { name: 'Exams', path: '/exams' },
    { name: 'Questions', path: '/questions' },
    { name: 'Results', path: '/results' },
  ];

  const handleLogout = async () => {
    try {
      
      await axios.post('/auth/logout', {}, { withCredentials: true });

      localStorage.clear();
      sessionStorage.clear();
      sessionStorage.setItem('justLoggedOut', 'true');

      
      navigate('/', { replace: true });
      console.log('Logout successful');
    } catch (err) {
      console.error('Logout error:', err);

   
      localStorage.clear();
      sessionStorage.clear();
      sessionStorage.setItem('justLoggedOut', 'true');
      navigate('/', { replace: true });
    }
  };

  return (
    <div className="w-64 bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 text-white flex flex-col h-full border-r border-white/10">
    
      <div className="p-6 text-2xl font-bold text-center border-b border-white/10 flex-shrink-0">
        <div className="flex items-center justify-center space-x-3">
          <div className="w-10 h-10 bg-white/10 rounded-full flex items-center justify-center">
            <svg
              className="w-6 h-6 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
              ></path>
            </svg>
          </div>
          <span className="text-sm font-normal text-slate-300">
            Logged in as: Admin
          </span>
        </div>
      </div>

      {}
      <nav className="flex-grow px-4 py-6">
        <ul>
          {navItems.map((item) => (
            <li key={item.name} className="mb-2">
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center space-x-3 p-3 rounded-xl transition-all duration-200 ease-in-out font-medium ${
                    isActive
                      ? "bg-white/10 text-white shadow-sm"
                      : "text-slate-300 hover:bg-white/10 hover:text-white"
                  }`
                }
              >
                <span>{item.name}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {}
      <div className="p-4 border-t border-white/10 mt-auto flex-shrink-0">
        <button
          onClick={handleLogout}
          className="w-full bg-gradient-to-r from-red-500 to-rose-500 hover:from-red-600 hover:to-rose-600 text-white font-semibold py-2 px-4 rounded-xl transition-colors shadow-sm"
        >
          Logout
        </button>
      </div>
    </div>
  );
}

export default Sidebar;
