import React, { useState, useEffect } from "react";
import axios from "../../api/axiosConfig";

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalExams: 0,
    totalQuestions: 0,
    examsCompleted: 0,
  });
  const [latestUsers, setLatestUsers] = useState([]);
  const isAdminUser = (user) =>
    Array.isArray(user?.roles) &&
    user.roles.some(role => (role?.name || '').toUpperCase() === "ADMIN");

  
  useEffect(() => {
    document.body.classList.add('admin-dashboard-body');
    return () => {
      document.body.classList.remove('admin-dashboard-body');
    };
  }, []);

  const fetchStats = async () => {
    try {
      const res = await axios.get("/admin/dashboard/stats", { withCredentials: true });
      setStats(res.data);

      const usersRes = await axios.get("/admin/users", { withCredentials: true });
      const nonAdminUsers = Array.isArray(usersRes.data)
        ? usersRes.data.filter(user => !isAdminUser(user))
        : [];
      setLatestUsers(nonAdminUsers.slice(-5).reverse());
    } catch (err) {
      console.error("Error fetching dashboard data:", err);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  return (
    <div className="h-screen overflow-y-auto bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
       
       <div className="mb-6 sm:mb-8">
  <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-800 mb-2 animate-fade-in">
    Welcome, <span className="text-blue-600">Admin!</span>
  </h1>
  <p className="text-sm sm:text-base lg:text-base text-gray-600 animate-fade-in delay-100">
    Here's a quick overview of your exam portal's performance.
  </p>
</div>

        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-8 sm:mb-10 animate-slide-up">
          <StatCard title="Total Users" value={stats.totalUsers} color="blue" icon="fas fa-users" />
          <StatCard title="Total Exams" value={stats.totalExams} color="green" icon="fas fa-file-alt" />
          <StatCard title="Total Questions" value={stats.totalQuestions} color="yellow" icon="fas fa-question-circle" />
          <StatCard title="Exams Completed" value={stats.examsCompleted} color="purple" icon="fas fa-check-circle" />
        </div>

        
      <div className="animate-fade-in delay-200">
  <h2 className="text-lg sm:text-xl lg:text-2xl font-semibold text-gray-800 mb-4 sm:mb-6">
    Latest User Activity
  </h2>
  <div className="bg-white rounded-xl sm:rounded-2xl shadow-xl overflow-hidden">
            
            <div className="block sm:hidden">
              {latestUsers.length > 0 ? (
                latestUsers.map((user) => (
                  <div key={user.id} className="border-b border-gray-200 p-4 hover:bg-gray-50 transition-colors">
                    <div className="flex justify-between items-start mb-2">
                      <span className="text-xs font-semibold text-gray-500 uppercase">ID</span>
                      <span className="text-sm font-medium text-gray-900">{user.id}</span>
                    </div>
                    <div className="flex justify-between items-start mb-2">
                      <span className="text-xs font-semibold text-gray-500 uppercase">Name</span>
                      <span className="text-sm font-medium text-gray-900">{user.name}</span>
                    </div>
                    <div className="flex justify-between items-start">
                      <span className="text-xs font-semibold text-gray-500 uppercase">Email</span>
                      <span className="text-sm text-gray-700 break-all">{user.email}</span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-6 text-center text-gray-500">No users found</div>
              )}
            </div>

            
            <div className="hidden sm:block overflow-x-auto">
              <table className="min-w-full table-auto">
                <thead>
                  <tr className="bg-gray-100 text-gray-600 uppercase text-xs sm:text-sm leading-normal">
                    <th className="py-3 px-4 sm:px-6 text-left">ID</th>
                    <th className="py-3 px-4 sm:px-6 text-left">Name</th>
                    <th className="py-3 px-4 sm:px-6 text-left">Email</th>
                  </tr>
                </thead>
                <tbody className="text-gray-600 text-xs sm:text-sm font-light">
                  {latestUsers.length > 0 ? (
                    latestUsers.map((user) => (
                      <tr key={user.id} className="border-b border-gray-200 hover:bg-gray-50 transition-colors">
                        <td className="py-3 px-4 sm:px-6 text-left">{user.id}</td>
                        <td className="py-3 px-4 sm:px-6 text-left">{user.name}</td>
                        <td className="py-3 px-4 sm:px-6 text-left">{user.email}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="3" className="py-6 text-center text-gray-500">
                        No users found
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}


function StatCard({ title, value, color, icon }) {
  const colorMap = {
    blue: { border: "border-blue-500", text: "text-blue-500", bg: "bg-blue-50" },
    green: { border: "border-green-500", text: "text-green-500", bg: "bg-green-50" },
    yellow: { border: "border-yellow-500", text: "text-yellow-500", bg: "bg-yellow-50" },
    purple: { border: "border-purple-500", text: "text-purple-500", bg: "bg-purple-50" },
  };

  const selectedColor = colorMap[color] || colorMap.blue;

  return (
    <div
      className={`bg-white p-4 sm:p-6 rounded-xl sm:rounded-2xl shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 border-t-4 ${selectedColor.border}`}
    >
      <div className="flex justify-between items-start mb-3 sm:mb-4">
        <h2 className="text-sm sm:text-base lg:text-xl font-semibold text-gray-700 leading-tight">
          {title}
        </h2>
        <span className={`text-2xl sm:text-3xl ${selectedColor.text}`}>
          <i className={icon}></i>
        </span>
      </div>
      <p className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900">{value}</p>
    </div>
  );
}
