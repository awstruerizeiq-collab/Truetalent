import React, { useState, useEffect } from "react";
import axios from "axios";

export default function ManageExams() {
  const [exams, setExams] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [formData, setFormData] = useState({ title: "", duration: "" });
  const [editingExam, setEditingExam] = useState(null);
  const [examToDelete, setExamToDelete] = useState(null);
  const [notification, setNotification] = useState({ message: "", type: "" });
  const [loading, setLoading] = useState(false);

  const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "/api";
  const API_URL = `${API_BASE_URL}/admin/exams`;
 
  const fetchExams = async () => {
    setLoading(true);
    try {
      console.log("🔍 Fetching exams from:", API_URL);
      const res = await axios.get(API_URL, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      console.log("✅ Exams fetched:", res.data);
      
      const examData = Array.isArray(res.data) ? res.data : res.data.exams || [];
      setExams(examData);
      
      if (examData.length === 0) {
        setNotification({ 
          message: "No exams found. Create your first exam!", 
          type: "info" 
        });
      }
    } catch (error) {
      console.error("❌ Error fetching exams:", error);
      const errorMsg = error.response?.data?.message || 
                       error.response?.data?.error || 
                       "Failed to fetch exams. Please check if the server is running.";
      setNotification({ message: errorMsg, type: "error" });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    
    const handlePopState = (event) => {
      window.history.pushState(null, "", window.location.href);
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    fetchExams();
  }, []);

  useEffect(() => {
    document.body.classList.add('admin-dashboard-body');
    return () => {
      document.body.classList.remove('admin-dashboard-body');
    };
  }, []);

  useEffect(() => {
    if (editingExam) {
      setFormData({ title: editingExam.title, duration: editingExam.duration });
    } else {
      setFormData({ title: "", duration: "" });
    }
  }, [editingExam]);

  
  useEffect(() => {
    if (notification.message) {
      const timer = setTimeout(() => {
        setNotification({ message: "", type: "" });
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const handleOpenModal = (exam = null) => {
    setEditingExam(exam);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setEditingExam(null);
    setFormData({ title: "", duration: "" });
    setIsModalOpen(false);
  };

  const handleOpenDeleteModal = (exam) => {
    setExamToDelete(exam);
    setIsDeleteModalOpen(true);
  };

  const handleCloseDeleteModal = () => {
    setExamToDelete(null);
    setIsDeleteModalOpen(false);
  };

  const handleSaveExam = async () => {
    
    if (!formData.title.trim()) {
      setNotification({ message: "Title is required!", type: "error" });
      return;
    }
    
    if (!formData.duration || formData.duration <= 0) {
      setNotification({ message: "Duration must be greater than 0!", type: "error" });
      return;
    }

    const payload = { 
      title: formData.title.trim(), 
      duration: Number(formData.duration),
      status: "Active"
    };

    setLoading(true);
    try {
      console.log("💾 Saving exam:", payload);
      
      if (editingExam) {
        const response = await axios.put(`${API_URL}/${editingExam.id}`, payload, {
          withCredentials: true,
          headers: {
            'Content-Type': 'application/json'
          }
        });
        console.log("✅ Update response:", response.data);
        setNotification({ message: "Exam updated successfully!", type: "success" });
      } else {
        const response = await axios.post(API_URL, payload, {
          withCredentials: true,
          headers: {
            'Content-Type': 'application/json'
          }
        });
        console.log("✅ Create response:", response.data);
        setNotification({ message: "Exam added successfully!", type: "success" });
      }
      
      await fetchExams();
      handleCloseModal();
    } catch (error) {
      console.error("❌ Error saving exam:", error);
      const errorMsg = error.response?.data?.message || 
                       error.response?.data?.error || 
                       "Error saving exam!";
      setNotification({ message: errorMsg, type: "error" });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    setLoading(true);
    try {
      console.log("🗑️ Deleting exam:", examToDelete.id);
      await axios.delete(`${API_URL}/${examToDelete.id}`, {
        withCredentials: true
      });
      console.log("✅ Exam deleted");
      
      await fetchExams();
      handleCloseDeleteModal();
      setNotification({ message: "Exam deleted successfully!", type: "success" });
    } catch (error) {
      console.error("❌ Error deleting exam:", error);
      const errorMsg = error.response?.data?.message || 
                       error.response?.data?.error || 
                       "Error deleting exam!";
      setNotification({ message: errorMsg, type: "error" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <div className="relative overflow-hidden rounded-3xl border border-white/70 bg-white/80 backdrop-blur-sm shadow-[0_30px_80px_-50px_rgba(15,23,42,0.45)] p-6 sm:p-8">
          <div className="pointer-events-none absolute -top-24 right-0 h-48 w-48 rounded-full bg-blue-200/40 blur-3xl"></div>
          <div className="pointer-events-none absolute -bottom-24 -left-16 h-40 w-40 rounded-full bg-indigo-200/40 blur-3xl"></div>

          <div className="relative z-10">
            <span className="inline-flex items-center gap-2 rounded-full border border-blue-200/60 bg-blue-50/70 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-blue-700">
              Exam management
            </span>
            <h2 className="mt-3 text-2xl sm:text-3xl lg:text-4xl font-semibold text-slate-900">Manage Exams</h2>

      {notification.message && (
        <div
          className={`mb-4 rounded-xl border p-4 ${
            notification.type === "success" 
              ? "border-emerald-200 bg-emerald-50 text-emerald-700" 
              : notification.type === "info"
              ? "border-blue-200 bg-blue-50 text-blue-700"
              : "border-red-200 bg-red-50 text-red-700"
          }`}
        >
          {notification.message}
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-6">
        <h3 className="text-lg font-semibold text-slate-900">Exam List</h3>
        <button
          onClick={() => handleOpenModal()}
          className="rounded-xl bg-blue-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:bg-blue-300"
          disabled={loading}
        >
          {loading ? "Loading..." : "Add Exam"}
        </button>
      </div>

      <div className="rounded-2xl border border-white/70 bg-white/80 p-6 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
        {loading && exams.length === 0 ? (
          <div className="text-center py-8">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <p className="mt-2 text-slate-600">Loading exams...</p>
          </div>
        ) : exams.length === 0 ? (
          <div className="text-center py-8 text-slate-500">
            <p className="text-lg">No exams found</p>
            <p className="text-sm mt-2">Click "Add Exam" to create your first exam</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full border-collapse">
              <thead>
                <tr className="bg-slate-50/80 text-slate-500 uppercase text-xs tracking-wider">
                  <th className="py-3 px-6 text-left">ID</th>
                  <th className="py-3 px-6 text-left">Title</th>
                  <th className="py-3 px-6 text-left">Duration (min)</th>
                  <th className="py-3 px-6 text-left">Status</th>
                  <th className="py-3 px-6 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="text-slate-700 text-sm font-light">
                {exams.map((exam) => (
                  <tr
                    key={exam.id}
                    className="border-b border-slate-100 hover:bg-slate-50/70 transition-colors duration-200"
                  >
                    <td className="py-3 px-6 text-left whitespace-nowrap">{exam.id}</td>
                    <td className="py-3 px-6 text-left">{exam.title}</td>
                    <td className="py-3 px-6 text-left">{exam.duration}</td>
                    <td className="py-3 px-6 text-left">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                        exam.status === 'Active' 
                          ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100' 
                          : 'bg-slate-100 text-slate-700 ring-1 ring-slate-200'
                      }`}>
                        {exam.status || 'Active'}
                      </span>
                    </td>
                    <td className="py-3 px-6 text-center">
                      <div className="flex items-center justify-center space-x-2">
                        <button
                          onClick={() => handleOpenModal(exam)}
                          className="text-blue-600 hover:text-blue-800 transition-colors duration-200 font-medium disabled:text-blue-300"
                          disabled={loading}
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleOpenDeleteModal(exam)}
                          className="text-red-600 hover:text-red-800 transition-colors duration-200 font-medium disabled:text-red-300"
                          disabled={loading}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-lg transform transition-all duration-300 scale-100">
            <h3 className="text-2xl font-bold mb-6 text-gray-800">
              {editingExam ? "Edit Exam" : "Add Exam"}
            </h3>
            <div className="space-y-4">
              <div>
                <label htmlFor="title" className="block text-gray-700 font-medium mb-1">
                  Title *
                </label>
                <input
                  type="text"
                  id="title"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow"
                  placeholder="e.g., Software Engineering Exam"
                  required
                  disabled={loading}
                />
              </div>
              <div>
                <label htmlFor="duration" className="block text-gray-700 font-medium mb-1">
                  Duration (minutes) *
                </label>
                <input
                  type="number"
                  id="duration"
                  value={formData.duration}
                  onChange={(e) => setFormData({ ...formData, duration: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow"
                  placeholder="e.g., 120"
                  min="1"
                  required
                  disabled={loading}
                />
              </div>
              <div className="flex justify-end space-x-3 mt-6">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="bg-gray-300 text-gray-800 font-bold py-2 px-5 rounded-lg hover:bg-gray-400 transition-colors duration-200 disabled:bg-gray-200"
                  disabled={loading}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveExam}
                  className="bg-blue-600 text-white font-bold py-2 px-5 rounded-lg hover:bg-blue-700 transition-colors duration-200 disabled:bg-blue-300"
                  disabled={loading}
                >
                  {loading ? "Saving..." : editingExam ? "Save Changes" : "Add Exam"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {isDeleteModalOpen && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md transform transition-all duration-300 scale-100">
            <h3 className="text-2xl font-bold mb-4 text-gray-800">Delete Exam</h3>
            <p className="text-gray-700 mb-6">
              Are you sure you want to delete <span className="font-semibold">{examToDelete?.title}</span>? This cannot be undone.
            </p>
            <div className="flex justify-end space-x-3">
              <button
                onClick={handleCloseDeleteModal}
                className="bg-gray-300 text-gray-800 font-bold py-2 px-5 rounded-lg hover:bg-gray-400 transition-colors duration-200 disabled:bg-gray-200"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="bg-red-600 text-white font-bold py-2 px-5 rounded-lg hover:bg-red-700 transition-colors duration-200 disabled:bg-red-300"
                disabled={loading}
              >
                {loading ? "Deleting..." : "Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
          </div>
        </div>
      </div>
    </div>
  );
}
