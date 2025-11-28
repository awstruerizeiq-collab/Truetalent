import React, { useState, useEffect } from "react";
import axios from "axios";
import Sidebar from "../../components/Sidebar";

export default function ManageExams() {
  const [exams, setExams] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [formData, setFormData] = useState({ title: "", duration: "" });
  const [editingExam, setEditingExam] = useState(null);
  const [examToDelete, setExamToDelete] = useState(null);
  const [notification, setNotification] = useState({ message: "", type: "" });

  const API_URL = "http://localhost:8080/api/admin/exams";

 
  const fetchExams = async () => {
    try {
      const res = await axios.get(API_URL);
      setExams(res.data);
    } catch (error) {
      console.error("Error fetching exams:", error);
    }
  };
  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    
    const handlePopState = (event) => {
      window.history.pushState(null, "", window.location.href);
      alert("⚠️ Please use the navigation menu to switch pages.");
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);
  

  useEffect(() => {
    fetchExams();
  }, []);

  
  useEffect(() => {
    if (editingExam) {
      setFormData({ title: editingExam.title, duration: editingExam.duration });
    } else {
      setFormData({ title: "", duration: "" });
    }
  }, [editingExam]);

 
  const handleOpenModal = (exam = null) => {
    setEditingExam(exam);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setEditingExam(null);
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
    const payload = { title: formData.title, duration: Number(formData.duration) };
    try {
      if (editingExam) {
        await axios.put(`${API_URL}/${editingExam.id}`, payload);
        setNotification({ message: "Exam updated successfully!", type: "success" });
      } else {
        await axios.post(API_URL, payload);
        setNotification({ message: "Exam added successfully!", type: "success" });
      }
      fetchExams();
      handleCloseModal();
    } catch (error) {
      console.error("Error saving exam:", error);
      setNotification({ message: "Error saving exam!", type: "error" });
    }
  };

 
  const handleDelete = async () => {
    try {
      await axios.delete(`${API_URL}/${examToDelete.id}`);
      fetchExams();
      handleCloseDeleteModal();
      setNotification({ message: "Exam deleted successfully!", type: "success" });
    } catch (error) {
      console.error("Error deleting exam:", error);
      setNotification({ message: "Error deleting exam!", type: "error" });
    }
  };

  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      <h2 className="text-4xl font-bold mb-6 text-gray-800">Manage Exams</h2>

      {notification.message && (
        <div
          className={`mb-4 p-4 rounded-lg ${
            notification.type === "success" ? "bg-green-200 text-green-800" : "bg-red-200 text-red-800"
          }`}
        >
          {notification.message}
        </div>
      )}

      <div className="flex justify-between items-center mb-6">
        <h3 className="text-xl font-semibold text-gray-700">Exam List</h3>
        <button
          onClick={() => handleOpenModal()}
          className="bg-blue-600 text-white font-bold py-2 px-6 rounded-lg shadow-lg hover:bg-blue-700 transition-colors duration-200"
        >
          Add Exam
        </button>
      </div>

      <div className="bg-white p-6 rounded-lg shadow-xl">
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white border-collapse rounded-lg overflow-hidden">
            <thead>
              <tr className="bg-gray-200 text-gray-700 uppercase text-sm leading-normal">
                <th className="py-3 px-6 text-left">ID</th>
                <th className="py-3 px-6 text-left">Title</th>
                <th className="py-3 px-6 text-left">Duration (min)</th>
                <th className="py-3 px-6 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="text-gray-700 text-sm font-light">
              {exams.map((exam) => (
                <tr
                  key={exam.id}
                  className="border-b border-gray-200 hover:bg-gray-100 transition-colors duration-200"
                >
                  <td className="py-3 px-6 text-left whitespace-nowrap">{exam.id}</td>
                  <td className="py-3 px-6 text-left">{exam.title}</td>
                  <td className="py-3 px-6 text-left">{exam.duration}</td>
                  <td className="py-3 px-6 text-center">
                    <div className="flex items-center justify-center space-x-2">
                      <button
                        onClick={() => handleOpenModal(exam)}
                        className="text-blue-600 hover:text-blue-800 transition-colors duration-200 font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleOpenDeleteModal(exam)}
                        className="text-red-600 hover:text-red-800 transition-colors duration-200 font-medium"
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
                  Title
                </label>
                <input
                  type="text"
                  id="title"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow"
                  required
                />
              </div>
              <div>
                <label htmlFor="duration" className="block text-gray-700 font-medium mb-1">
                  Duration (min)
                </label>
                <input
                  type="number"
                  id="duration"
                  value={formData.duration}
                  onChange={(e) => setFormData({ ...formData, duration: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow"
                  required
                />
              </div>
              <div className="flex justify-end space-x-3 mt-6">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="bg-gray-300 text-gray-800 font-bold py-2 px-5 rounded-lg hover:bg-gray-400 transition-colors duration-200"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveExam}
                  className="bg-blue-600 text-white font-bold py-2 px-5 rounded-lg hover:bg-blue-700 transition-colors duration-200"
                >
                  {editingExam ? "Save Changes" : "Add Exam"}
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
                className="bg-gray-300 text-gray-800 font-bold py-2 px-5 rounded-lg hover:bg-gray-400 transition-colors duration-200"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="bg-red-600 text-white font-bold py-2 px-5 rounded-lg hover:bg-red-700 transition-colors duration-200"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

