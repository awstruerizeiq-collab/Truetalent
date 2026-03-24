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
    <div className="min-h-screen">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <div className="relative overflow-hidden rounded-3xl border border-white/70 bg-white/80 backdrop-blur-sm shadow-[0_30px_80px_-50px_rgba(15,23,42,0.45)] p-6 sm:p-8">
          <div className="pointer-events-none absolute -top-24 right-0 h-56 w-56 rounded-full bg-blue-200/40 blur-3xl animate-float-slow"></div>
          <div className="pointer-events-none absolute -bottom-24 -left-16 h-48 w-48 rounded-full bg-indigo-200/40 blur-3xl animate-float"></div>

          <div className="relative z-10">
            <div className="mb-6 sm:mb-8 animate-fade-in">
              <span className="inline-flex items-center gap-2 rounded-full border border-blue-200/60 bg-blue-50/70 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-blue-700">
                Admin overview
              </span>
              <h1 className="mt-3 text-2xl sm:text-3xl lg:text-4xl font-semibold text-slate-900">
                Welcome back, <span className="text-blue-700">Admin</span>
              </h1>
              <p className="mt-2 text-sm sm:text-base text-slate-600">
                Here is a quick snapshot of performance and the most recent user activity.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-8 sm:mb-10 animate-slide-up anim-delay-100">
              <StatCard title="Total Users" value={stats.totalUsers} color="blue" icon="fas fa-users" />
              <StatCard title="Total Exams" value={stats.totalExams} color="green" icon="fas fa-file-alt" />
              <StatCard title="Total Questions" value={stats.totalQuestions} color="yellow" icon="fas fa-question-circle" />
              <StatCard title="Exams Completed" value={stats.examsCompleted} color="purple" icon="fas fa-check-circle" />
            </div>

            <div className="animate-fade-in anim-delay-200">
              <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-2 mb-4 sm:mb-6">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-semibold text-slate-900">
                    Latest User Activity
                  </h2>
                  <p className="text-xs sm:text-sm text-slate-500">
                    Recently added non-admin users in the portal.
                  </p>
                </div>
                <span className="inline-flex w-fit items-center gap-2 rounded-full border border-slate-200 bg-white/80 px-3 py-1 text-xs font-semibold text-slate-600">
                  Latest snapshot
                </span>
              </div>

              <div className="rounded-2xl border border-white/70 bg-white/80 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] overflow-hidden">
                <div className="block sm:hidden space-y-3 p-4">
                  {latestUsers.length > 0 ? (
                    latestUsers.map((user) => (
                      <div
                        key={user.id}
                        className="rounded-xl border border-slate-200/60 bg-white/90 p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
                      >
                        <div className="flex items-center justify-between text-xs font-semibold uppercase text-slate-400">
                          <span>ID</span>
                          <span className="text-slate-700 normal-case text-sm font-medium">{user.id}</span>
                        </div>
                        <div className="mt-3 flex items-center justify-between text-xs font-semibold uppercase text-slate-400">
                          <span>Name</span>
                          <span className="text-slate-700 normal-case text-sm font-medium">{user.name}</span>
                        </div>
                        <div className="mt-3 text-xs font-semibold uppercase text-slate-400">Email</div>
                        <div className="text-sm text-slate-600 break-all">{user.email}</div>
                      </div>
                    ))
                  ) : (
                    <div className="rounded-xl border border-slate-200/60 bg-white/90 p-6 text-center text-slate-500">
                      No users found
                    </div>
                  )}
                </div>

                <div className="hidden sm:block overflow-x-auto">
                  <table className="min-w-full table-auto">
                    <thead>
                      <tr className="bg-slate-50/80 text-slate-500 uppercase text-xs sm:text-sm leading-normal tracking-wider">
                        <th className="py-3 px-4 sm:px-6 text-left">ID</th>
                        <th className="py-3 px-4 sm:px-6 text-left">Name</th>
                        <th className="py-3 px-4 sm:px-6 text-left">Email</th>
                      </tr>
                    </thead>
                    <tbody className="text-slate-600 text-xs sm:text-sm font-light">
                      {latestUsers.length > 0 ? (
                        latestUsers.map((user) => (
                          <tr
                            key={user.id}
                            className="border-b border-slate-100 hover:bg-slate-50/70 transition-colors"
                          >
                            <td className="py-3 px-4 sm:px-6 text-left">{user.id}</td>
                            <td className="py-3 px-4 sm:px-6 text-left">{user.name}</td>
                            <td className="py-3 px-4 sm:px-6 text-left">{user.email}</td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="3" className="py-6 text-center text-slate-500">
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
      </div>
    </div>
  );
}


function StatCard({ title, value, color, icon }) {
  const colorMap = {
    blue: {
      text: "text-blue-700",
      iconBg: "bg-blue-50",
      gradient: "from-blue-500 to-cyan-400",
      ring: "ring-blue-100",
    },
    green: {
      text: "text-emerald-700",
      iconBg: "bg-emerald-50",
      gradient: "from-emerald-500 to-lime-400",
      ring: "ring-emerald-100",
    },
    yellow: {
      text: "text-amber-700",
      iconBg: "bg-amber-50",
      gradient: "from-amber-500 to-yellow-400",
      ring: "ring-amber-100",
    },
    purple: {
      text: "text-indigo-700",
      iconBg: "bg-indigo-50",
      gradient: "from-indigo-500 to-blue-400",
      ring: "ring-indigo-100",
    },
  };

  const selectedColor = colorMap[color] || colorMap.blue;

  return (
    <div
      className={`group relative overflow-hidden rounded-2xl border border-white/70 bg-white/80 p-5 sm:p-6 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] ring-1 ${selectedColor.ring} transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_30px_60px_-40px_rgba(15,23,42,0.5)]`}
    >
      <div className={`absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${selectedColor.gradient}`}></div>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-slate-500">{title}</p>
          <p className="mt-2 text-3xl sm:text-4xl font-semibold text-slate-900">{value}</p>
        </div>
        <span className={`flex h-12 w-12 items-center justify-center rounded-xl ${selectedColor.iconBg} ${selectedColor.text} shadow-sm`}>
          <i className={icon}></i>
        </span>
      </div>
    </div>
  );
}
