import React, { useState, useEffect, useMemo } from "react";
import axios from "axios";

const Notification = ({ message, type, onClose }) => {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [message, onClose]);

  if (!message) return null;

  const baseStyle =
    "fixed top-5 right-5 p-4 rounded-lg shadow-2xl text-white transform transition-all duration-300 z-50 max-w-md";
  const typeStyles = { 
    success: "bg-green-500", 
    info: "bg-blue-500", 
    error: "bg-red-500",
    warning: "bg-yellow-500" 
  };

  return (
    <div className={`${baseStyle} ${typeStyles[type]}`}>
      <span className="text-sm">{message}</span>
      <button
        onClick={onClose}
        className="ml-4 font-bold opacity-70 hover:opacity-100"
      >
        ✖
      </button>
    </div>
  );
};

const DetailsModal = ({ result, onClose }) => {
  if (!result) return null;
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-40 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div className="p-6 border-b">
          <h2 className="text-2xl font-bold text-gray-800">
            Exam Details: {result.name}
          </h2>
          <p className="text-gray-600">
            Exam: <span className="font-semibold">{result.exam}</span>
          </p>
        </div>
        <div className="p-6 overflow-y-auto">
          {result.details && result.details.length > 0 ? (
            <div className="space-y-4">
              {result.details.map((detail, idx) => (
                <div
                  key={idx}
                  className={`p-4 rounded-lg ${detail.result === "Correct"
                      ? "bg-green-50 border-green-200"
                      : "bg-red-50 border-red-200"
                    } border`}
                >
                  <p className="font-semibold text-gray-800 mb-2">
                    Q{idx + 1}: {detail.question}
                  </p>
                  <p className="text-sm text-gray-600">
                    Your Answer:{" "}
                    <span className="font-medium">{detail.yourAnswer}</span>
                  </p>
                  {detail.result === "Incorrect" && (
                    <p className="text-sm text-green-700">
                      Correct Answer:{" "}
                      <span className="font-medium">
                        {detail.correctAnswer}
                      </span>
                    </p>
                  )}
                  <p
                    className={`text-right font-bold text-sm ${detail.result === "Correct"
                        ? "text-green-600"
                        : "text-red-600"
                      }`}
                  >
                    {detail.result}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500 text-center">
              No detailed answer breakdown available for this result.
            </p>
          )}
        </div>
        <div className="p-4 bg-gray-50 border-t text-right">
          <button
            onClick={onClose}
            className="bg-gray-600 text-white font-bold py-2 px-6 rounded-lg hover:bg-gray-700 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

const EditModal = ({ selectedData, onClose, onSave }) => {
  const [formData, setFormData] = useState(selectedData || []);

  const handleChange = (index, field, value) => {
    const updated = [...formData];
    updated[index][field] = value;
    setFormData(updated);
  };

  const handleSave = () => {
    onSave(formData);
    onClose();
  };

  if (!selectedData) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-40 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-y-auto">
        <div className="p-6 border-b">
          <h2 className="text-2xl font-bold">Edit Candidates</h2>
        </div>
        <div className="p-6 space-y-4">
          {formData.map((r, idx) => (
            <div key={r.id} className="border p-4 rounded-lg space-y-2">
              <input
                type="text"
                value={r.name}
                onChange={(e) => handleChange(idx, "name", e.target.value)}
                placeholder="Name"
                className="w-full px-3 py-2 border rounded"
              />
              <input
                type="text"
                value={r.email}
                onChange={(e) => handleChange(idx, "email", e.target.value)}
                placeholder="Email"
                className="w-full px-3 py-2 border rounded"
              />
              <input
                type="number"
                value={r.score}
                onChange={(e) => handleChange(idx, "score", parseInt(e.target.value))}
                placeholder="Score"
                className="w-full px-3 py-2 border rounded"
              />
            </div>
          ))}
        </div>
        <div className="p-4 border-t flex justify-end space-x-2">
          <button
            onClick={onClose}
            className="bg-gray-600 text-white py-2 px-4 rounded hover:bg-gray-700"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="bg-green-600 text-white py-2 px-4 rounded hover:bg-green-700"
          >
            Save
          </button>
        </div>
      </div>
    </div>
  );
};


export default function Results() {
  const [results, setResults] = useState([]);
  const [notification, setNotification] = useState({ message: "", type: "" });
  const [searchTerm, setSearchTerm] = useState("");
  const [examFilter, setExamFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("All");
  const [sortConfig, setSortConfig] = useState({
    key: "id",
    direction: "ascending",
  });
  const [selectedResults, setSelectedResults] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [selectedResultDetails, setSelectedResultDetails] = useState(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const fetchResults = async () => {
    try {
      const res = await axios.get("http://localhost:8080/api/admin/results/all");
      setResults(res.data);
    } catch (err) {
      console.error("Error fetching results:", err);
      setNotification({ 
        message: "Failed to fetch results: " + (err.response?.data || err.message), 
        type: "error" 
      });
    }
  };

  useEffect(() => {
    fetchResults();
  }, []);
  
  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    
    const handlePopState = (event) => {
      window.history.pushState(null, "", window.location.href);
      alert("⚠️ Please use the navigation menu to switch pages.");
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const sortedAndFilteredResults = useMemo(() => {
    let processedResults = [...results].filter(
      (result) =>
        (result.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          result.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          result.collegeName?.toLowerCase().includes(searchTerm.toLowerCase())) &&
        (examFilter === "All" || result.exam === examFilter) &&
        (statusFilter === "All" || result.status === statusFilter)
    );
    if (sortConfig.key) {
      processedResults.sort((a, b) => {
        if (a[sortConfig.key] < b[sortConfig.key])
          return sortConfig.direction === "ascending" ? -1 : 1;
        if (a[sortConfig.key] > b[sortConfig.key])
          return sortConfig.direction === "ascending" ? 1 : -1;
        return 0;
      });
    }
    return processedResults;
  }, [results, searchTerm, examFilter, statusFilter, sortConfig]);

  const totalPages = Math.ceil(sortedAndFilteredResults.length / 5);
  const paginatedResults = useMemo(() => {
    const start = (currentPage - 1) * 5;
    return sortedAndFilteredResults.slice(start, start + 5);
  }, [currentPage, sortedAndFilteredResults]);

  const examOptions = useMemo(
    () => ["All", ...new Set(results.map((r) => r.exam))],
    [results]
  );

  const requestSort = (key) => {
    let direction = "ascending";
    if (sortConfig.key === key && sortConfig.direction === "ascending")
      direction = "descending";
    setSortConfig({ key, direction });
  };

  const getSortIndicator = (key) =>
    sortConfig.key === key
      ? sortConfig.direction === "ascending"
        ? "▲"
        : "▼"
      : null;

  const handleDeleteOne = async (id) => {
    if (!window.confirm('Are you sure you want to delete this result?')) return;
    
    try {
      await axios.delete(`http://localhost:8080/api/admin/results/${id}`);
      setNotification({ message: 'Result deleted successfully!', type: 'success' });
      fetchResults();
    } catch (err) {
      console.error('Error deleting result:', err);
      setNotification({ 
        message: 'Failed to delete result: ' + (err.response?.data?.error || err.message), 
        type: 'error' 
      });
    }
  };

  const handleReleaseResult = async (id) => {
    try {
      const response = await axios.put(`http://localhost:8080/api/admin/results/${id}/release`);
      fetchResults();
      
     
      const message = response.data;
      
     
      if (typeof message === 'string' && 
          (message.includes("email sending failed") || 
           message.includes("email failed") || 
           message.includes("not configured"))) {
        setNotification({ message: message, type: "warning" });
      } else {
        setNotification({ message: message, type: "success" });
      }
    } catch (err) {
      console.error(err);
      setNotification({ 
        message: "Error releasing result: " + (err.response?.data || err.message), 
        type: "error" 
      });
    }
  };

  const handleBulkRelease = async () => {
    try {
      const results = await Promise.allSettled(
        selectedResults.map((id) =>
          axios.put(`http://localhost:8080/api/admin/results/${id}/release`)
        )
      );
      
      const successful = results.filter(r => r.status === 'fulfilled').length;
      const failed = results.filter(r => r.status === 'rejected').length;
      
      fetchResults();
      setNotification({
        message: `Results released: ${successful} successful, ${failed} failed`,
        type: successful > 0 ? "success" : "error",
      });
      setSelectedResults([]);
    } catch (err) {
      console.error(err);
      setNotification({ 
        message: "Error in bulk release", 
        type: "error" 
      });
    }
  };

  const handleAutoMailSend = async () => {
    try {
      const response = await axios.put("http://localhost:8080/api/admin/results/send-mails");
      
     
      const message = response.data;
      
      
      if (typeof message === 'string' && message.includes("not configured")) {
        setNotification({
          message: "Email service is not configured. Please check application.properties",
          type: "warning",
        });
      } else {
        setNotification({
          message: message,
          type: "success",
        });
      }
    } catch (err) {
      console.error(err);
      setNotification({
        message: "Error sending mails: " + (err.response?.data || err.message),
        type: "error",
      });
    }
  };

  const handleSaveEdits = async (updatedData) => {
    try {
      await Promise.all(
        updatedData.map((r) =>
          axios.put(`http://localhost:8080/api/admin/results/${r.id}`, r)
        )
      );
      fetchResults();
      setNotification({ message: "Candidates updated successfully!", type: "success" });
      setSelectedResults([]);
    } catch (err) {
      console.error(err);
      setNotification({ 
        message: "Error updating candidates: " + (err.response?.data?.error || err.message), 
        type: "error" 
      });
    }
  };

  const handleBulkDelete = async () => {
    if (!window.confirm("Are you sure you want to delete selected results?")) return;
    
    try {
      const results = await Promise.allSettled(
        selectedResults.map((id) =>
          axios.delete(`http://localhost:8080/api/admin/results/${id}`)
        )
      );
      
      const successful = results.filter(r => r.status === 'fulfilled').length;
      const failed = results.filter(r => r.status === 'rejected').length;
      
      setNotification({ 
        message: `Deleted: ${successful} successful, ${failed} failed`, 
        type: successful > 0 ? "success" : "error" 
      });
      setSelectedResults([]);
      fetchResults();
    } catch (err) {
      console.error(err);
      setNotification({ 
        message: "Error deleting selected results: " + (err.response?.data?.error || err.message), 
        type: "error" 
      });
    }
  };

  const handleSelectAll = (e) => {
    const ids = paginatedResults.map((r) => r.id);
    if (e.target.checked)
      setSelectedResults((prev) => [...new Set([...prev, ...ids])]);
    else setSelectedResults((prev) => prev.filter((id) => !ids.includes(id)));
  };

  const handleSelectOne = (id) =>
    setSelectedResults((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );

  const areAllOnPageSelected =
    paginatedResults.length > 0 &&
    paginatedResults.every((r) => selectedResults.includes(r.id));

  const goToPage = (page) =>
    setCurrentPage(Math.max(1, Math.min(page, totalPages)));

  const handleViewDetails = (result) => {
    setSelectedResultDetails(result);
    setIsDetailsModalOpen(true);
  };

  const handleEditSelected = () => {
    const selectedData = results.filter((r) => selectedResults.includes(r.id));
    setSelectedResultDetails(selectedData);
    setIsEditModalOpen(true);
  };

  return (
    <div className="p-8 bg-gray-50 min-h-full">
      <Notification
        message={notification.message}
        type={notification.type}
        onClose={() => setNotification({ message: "", type: "" })}
      />
      {isDetailsModalOpen && (
        <DetailsModal
          result={selectedResultDetails}
          onClose={() => setIsDetailsModalOpen(false)}
        />
      )}

      {isEditModalOpen && (
        <EditModal
          selectedData={selectedResultDetails}
          onClose={() => setIsEditModalOpen(false)}
          onSave={handleSaveEdits}
        />
      )}

      <div className="flex justify-between items-center mb-6">
        <h1 className="text-4xl font-bold text-gray-800">Candidate Results</h1>
        <button
          onClick={handleAutoMailSend}
          className="bg-indigo-600 text-white font-bold py-2 px-6 rounded-lg hover:bg-indigo-700 shadow-md transition"
        >
          📧 Send Mails Automatically
        </button>
      </div>

      
      <div className="bg-white p-4 rounded-xl shadow-md mb-6 grid grid-cols-1 md:grid-cols-3 gap-4">
        <input
          type="text"
          placeholder="Search by name, email, college..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <select
          value={examFilter}
          onChange={(e) => setExamFilter(e.target.value)}
          className="w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {examOptions.map((e) => (
            <option key={e} value={e}>
              {e}
            </option>
          ))}
        </select>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="All">All Statuses</option>
          <option value="Completed">Completed</option>
          <option value="Result Released">Result Released</option>
          <option value="Passed">Passed</option>
          <option value="Failed">Failed</option>
        </select>
      </div>

      
      {selectedResults.length > 0 && (
        <div className="flex space-x-4 mb-4">
          <button
            onClick={handleBulkRelease}
            className="bg-green-600 text-white font-bold py-2 px-4 rounded-lg shadow-md hover:bg-green-700 transition-colors"
          >
            Release Results ({selectedResults.length})
          </button>
          <button
            onClick={handleEditSelected}
            className="bg-yellow-500 text-white font-bold py-2 px-4 rounded-lg hover:bg-yellow-600 transition-colors"
          >
            Edit ({selectedResults.length})
          </button>
          <button
            onClick={handleBulkDelete}
            className="bg-red-600 text-white font-bold py-2 px-4 rounded-lg hover:bg-red-700 transition-colors"
          >
            Delete ({selectedResults.length})
          </button>
        </div>
      )}

      
      <div className="bg-white rounded-2xl shadow-xl overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-200">
            <tr className="text-gray-700 uppercase text-sm leading-normal">
              <th className="py-4 px-6 text-left">
                <input
                  type="checkbox"
                  onChange={handleSelectAll}
                  checked={areAllOnPageSelected}
                />
              </th>
              <th className="py-4 px-6 text-left cursor-pointer" onClick={() => requestSort("id")}>
                ID {getSortIndicator("id")}
              </th>
              <th className="py-4 px-6 text-left cursor-pointer" onClick={() => requestSort("name")}>
                Candidate Name {getSortIndicator("name")}
              </th>
              <th className="py-4 px-6 text-left">College</th>
              <th className="py-4 px-6 text-left">Email</th>
              <th className="py-4 px-6 text-left">Exam</th>
              <th className="py-4 px-6 text-center cursor-pointer" onClick={() => requestSort("score")}>
                Score {getSortIndicator("score")}
              </th>
              <th className="py-4 px-6 text-center">Status</th>
              <th className="py-4 px-6 text-center">Actions</th>
            </tr>
          </thead>
          <tbody className="text-gray-700 text-sm font-light">
            {paginatedResults.length === 0 ? (
              <tr>
                <td colSpan="9" className="py-8 text-center text-gray-500">
                  No results found
                </td>
              </tr>
            ) : (
              paginatedResults.map((r) => (
                <tr
                  key={r.id}
                  className={`border-b border-gray-200 transition-colors ${selectedResults.includes(r.id)
                      ? "bg-blue-50"
                      : "hover:bg-gray-100"
                    }`}
                >
                  <td className="py-4 px-6 text-left">
                    <input
                      type="checkbox"
                      checked={selectedResults.includes(r.id)}
                      onChange={() => handleSelectOne(r.id)}
                    />
                  </td>
                  <td className="py-4 px-6 text-left font-semibold">{r.id}</td>
                  <td className="py-4 px-6 text-left font-medium">{r.name}</td>
                  <td className="py-4 px-6 text-left">{r.collegeName}</td>
                  <td className="py-4 px-6 text-left">{r.email}</td>
                  <td className="py-4 px-6 text-left">{r.exam}</td>
                  <td className="py-4 px-6 text-center font-semibold text-blue-700">
                    {r.score}
                  </td>
                  <td className="py-4 px-6 text-center">
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-semibold ${r.status === "Result Released"
                          ? "bg-blue-100 text-blue-800"
                          : r.status === "Passed"
                            ? "bg-green-100 text-green-800"
                            : r.status === "Failed"
                              ? "bg-red-100 text-red-800"
                              : "bg-yellow-100 text-yellow-800"
                        }`}
                    >
                      {r.status}
                    </span>
                  </td>
                  <td className="py-4 px-6 text-center flex items-center justify-center space-x-2">
                    <button
                      onClick={() => handleViewDetails(r)}
                      className="text-purple-600 hover:text-purple-800 font-semibold"
                      title="View Details"
                    >
                      Details
                    </button>
                    <button
                      onClick={() => handleReleaseResult(r.id)}
                      disabled={r.status === "Result Released"}
                      className="bg-blue-600 text-white font-bold py-2 px-4 rounded-lg shadow-md hover:bg-blue-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
                      title="Release Result"
                    >
                      Release
                    </button>
                    <button
                      onClick={() => handleDeleteOne(r.id)}
                      className="bg-red-600 text-white font-bold py-2 px-4 rounded-lg shadow-md hover:bg-red-700 transition-colors"
                      title="Delete Result"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        
        <div className="flex justify-between items-center p-4">
          <span className="text-sm text-gray-700">
            Page <strong>{currentPage}</strong> of{" "}
            <strong>{totalPages || 1}</strong>
            {sortedAndFilteredResults.length > 0 && (
              <span className="ml-2">
                ({sortedAndFilteredResults.length} result{sortedAndFilteredResults.length !== 1 ? 's' : ''})
              </span>
            )}
          </span>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 1}
              className="px-3 py-1 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <button
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage === totalPages || totalPages === 0}
              className="px-3 py-1 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}