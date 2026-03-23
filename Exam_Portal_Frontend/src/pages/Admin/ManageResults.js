import React, { useState, useEffect, useMemo } from "react";
import axios from "../../api/axiosConfig";

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

const normalizeResultStatus = (status) => {
  const value = (status || "").toLowerCase();
  if (value === "pass" || value === "passed") return "Pass";
  if (value === "fail" || value === "failed") return "Fail";
  if (value === "result released") return "Result Released";
  if (value === "completed") return "Completed";
  return status || "Completed";
};

const formatPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return `${value}%`;
};

const DetailsModal = ({ result, onClose }) => {
  if (!result) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-40 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col">
        <div className="p-6 border-b">
          <h2 className="text-2xl font-bold text-gray-800">
            Answer Sheet: {result.candidateName || result.name || "Candidate"}
          </h2>
          <p className="text-gray-600">
            Exam: <span className="font-semibold">{result.exam || "-"}</span>
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-4 text-sm">
            <div className="bg-gray-50 rounded p-2">
              <span className="text-gray-500">Score</span>
              <p className="font-semibold">{result.score ?? "-"}</p>
            </div>
            <div className="bg-gray-50 rounded p-2">
              <span className="text-gray-500">Pass %</span>
              <p className="font-semibold">{formatPercent(result.scorePercentage)}</p>
            </div>
            <div className="bg-gray-50 rounded p-2">
              <span className="text-gray-500">Attempted</span>
              <p className="font-semibold">
                {result.attemptedQuestions ?? 0}/{result.totalQuestions ?? 0}
              </p>
            </div>
            <div className="bg-gray-50 rounded p-2">
              <span className="text-gray-500">Correct</span>
              <p className="font-semibold">{result.correctAnswers ?? 0}</p>
            </div>
          </div>
        </div>
        <div className="p-6 overflow-y-auto">
          {result.questions && result.questions.length > 0 ? (
            <div className="space-y-4">
              {result.questions.map((detail, idx) => (
                <div
                  key={detail.questionId || idx}
                  className={`p-4 rounded-lg border ${detail.result === "Correct"
                    ? "bg-green-50 border-green-200"
                    : detail.result === "Incorrect"
                      ? "bg-red-50 border-red-200"
                      : "bg-gray-50 border-gray-200"
                    }`}
                >
                  <p className="font-semibold text-gray-800 mb-2">
                    Q{detail.qNo || idx + 1}: {detail.question}
                  </p>
                  <p className="text-sm text-gray-700">
                    Your Answer: <span className="font-medium">{detail.selectedAnswer || "-"}</span>
                  </p>
                  <p className="text-sm text-green-700">
                    Correct Answer: <span className="font-medium">{detail.correctAnswer || "-"}</span>
                  </p>
                  <div className="text-xs text-gray-500 mt-2">
                    Marks: {detail.marks ?? 0}
                  </div>
                  <p
                    className={`text-right font-bold text-sm ${detail.result === "Correct"
                      ? "text-green-700"
                      : detail.result === "Incorrect"
                        ? "text-red-700"
                        : "text-gray-600"
                      }`}
                  >
                    {detail.result || "-"}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500 text-center">
              No answer sheet data available for this candidate.
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

// 🔥 Slot-wise view component
const SlotWiseView = ({ slotsData, onViewSlot, loading }) => {
  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="mt-4 text-gray-600">Loading slot-wise data...</p>
      </div>
    );
  }

  if (!slotsData || slotsData.length === 0) {
    return (
      <div className="text-center py-8">
        <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-6 max-w-md mx-auto">
          <svg className="w-16 h-16 text-yellow-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p className="text-lg font-semibold text-gray-800 mb-2">No Slots Available</p>
          <p className="text-sm text-gray-600">Create slots in the Slots management section to see slot-wise results</p>
        </div>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {slotsData.map((slotData) => (
        <div
          key={slotData.slotId}
          className="bg-white rounded-xl shadow-lg p-6 border border-gray-200 hover:shadow-xl transition-shadow"
        >
          <div className="flex justify-between items-start mb-4">
            <div>
              <h3 className="text-2xl font-bold text-blue-600">
                Slot {slotData.slotNumber}
              </h3>
              {slotData.collegeName && (
                <p className="text-sm text-gray-600 mt-1">{slotData.collegeName}</p>
              )}
            </div>
            <span className={`px-3 py-1 rounded-full text-sm font-semibold ${
              slotData.totalCandidates > 0 
                ? 'bg-blue-100 text-blue-800' 
                : 'bg-gray-100 text-gray-600'
            }`}>
              {slotData.totalCandidates} Candidate{slotData.totalCandidates !== 1 ? 's' : ''}
            </span>
          </div>

          <div className="space-y-2 mb-4">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Date:</span>
              <span className="font-semibold">{slotData.date}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Time:</span>
              <span className="font-semibold">{slotData.time}</span>
            </div>
          </div>

          {slotData.totalCandidates > 0 ? (
            <>
              <div className="border-t pt-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Average Score:</span>
                  <span className="font-bold text-blue-700">{slotData.averageScore}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Max Score:</span>
                  <span className="font-bold text-green-700">{slotData.maxScore}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Min Score:</span>
                  <span className="font-bold text-orange-700">{slotData.minScore}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Pass Rate:</span>
                  <span className="font-bold text-purple-700">{slotData.passPercentage}%</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Pass Threshold:</span>
                  <span className="font-bold text-indigo-700">{slotData.slotPassPercentage ?? 80}%</span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 mt-4">
                <div className="bg-green-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-green-700">{slotData.passedCount}</p>
                  <p className="text-xs text-gray-600">Pass</p>
                </div>
                <div className="bg-red-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-red-700">{slotData.failedCount}</p>
                  <p className="text-xs text-gray-600">Fail</p>
                </div>
              </div>

              <button
                onClick={() => onViewSlot(slotData.slotNumber)}
                className="w-full mt-4 bg-blue-600 text-white font-bold py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors"
              >
                View Results
              </button>
            </>
          ) : (
            <div className="mt-4 p-4 bg-gray-50 rounded-lg text-center">
              <p className="text-sm text-gray-500">No candidates in this slot yet</p>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default function Results() {
  const [results, setResults] = useState([]);
  const [slotsData, setSlotsData] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [viewMode, setViewMode] = useState("all"); // "all", "slots", "filtered"
  const [selectedSlotNumber, setSelectedSlotNumber] = useState(null);
  const [notification, setNotification] = useState({ message: "", type: "" });
  const [searchTerm, setSearchTerm] = useState("");
  const [examFilter, setExamFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("All");
  const [slotFilter, setSlotFilter] = useState("All");
  const [sortConfig, setSortConfig] = useState({
    key: "id",
    direction: "ascending",
  });
  const [selectedResults, setSelectedResults] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [selectedResultDetails, setSelectedResultDetails] = useState(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [slotLoading, setSlotLoading] = useState(false);
  const [viewAnswerSheetLoadingId, setViewAnswerSheetLoadingId] = useState(null);
  const [downloadAnswerSheetLoadingId, setDownloadAnswerSheetLoadingId] = useState(null);

  const fetchResults = async (slotNumber = null) => {
    try {
      setLoading(true);
      console.log("📊 Fetching results...", slotNumber ? `for slot ${slotNumber}` : "all");
      
      const url = slotNumber 
        ? `/admin/results/all?slotNumber=${slotNumber}`
        : "/admin/results/all";
      
      const res = await axios.get(url, {
        withCredentials: true,
      });
      
      console.log("✅ Results fetched:", res.data);
      setResults(res.data || []);
      
      if (slotNumber) {
        setViewMode("filtered");
        setSelectedSlotNumber(slotNumber);
      }
    } catch (err) {
      console.error("❌ Error fetching results:", err);
      setNotification({ 
        message: "Failed to fetch results: " + (err.response?.data?.error || err.message), 
        type: "error" 
      });
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchSlotWiseData = async () => {
    try {
      setSlotLoading(true);
      console.log("🎯 Fetching slot-wise data...");
      
      const res = await axios.get("/admin/results/by-slots", {
        withCredentials: true,
      });
      
      console.log("✅ Slot-wise data received:", res.data);
      
      if (res.data && res.data.slots) {
        setSlotsData(res.data.slots);
        console.log(`📊 Total slots: ${res.data.slots.length}`);
      } else {
        setSlotsData([]);
        console.warn("⚠️ No slot data in response");
      }
    } catch (err) {
      console.error("❌ Error fetching slot-wise data:", err);
      setNotification({ 
        message: "Failed to fetch slot-wise data: " + (err.response?.data?.error || err.message), 
        type: "error" 
      });
      setSlotsData([]);
    } finally {
      setSlotLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      console.log("📈 Fetching statistics...");
      const res = await axios.get("/admin/results/statistics", {
        withCredentials: true,
      });
      console.log("✅ Statistics received:", res.data);
      setStatistics(res.data);
    } catch (err) {
      console.error("❌ Error fetching statistics:", err);
      setStatistics(null);
    }
  };

  useEffect(() => {
    console.log("🚀 Component mounted, fetching initial data...");
    fetchResults();
    fetchSlotWiseData();
    fetchStatistics();
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
        (statusFilter === "All" || normalizeResultStatus(result.status) === statusFilter) &&
        (slotFilter === "All" || (result.slot && result.slot.slotNumber === parseInt(slotFilter)))
    );
    
    if (sortConfig.key) {
      processedResults.sort((a, b) => {
        let aVal = a[sortConfig.key];
        let bVal = b[sortConfig.key];
        
        // Handle slot sorting
        if (sortConfig.key === "slot") {
          aVal = a.slot?.slotNumber || 0;
          bVal = b.slot?.slotNumber || 0;
        }
        
        if (aVal < bVal)
          return sortConfig.direction === "ascending" ? -1 : 1;
        if (aVal > bVal)
          return sortConfig.direction === "ascending" ? 1 : -1;
        return 0;
      });
    }
    return processedResults;
  }, [results, searchTerm, examFilter, statusFilter, slotFilter, sortConfig]);

  const totalPages = Math.ceil(sortedAndFilteredResults.length / 10);
  const paginatedResults = useMemo(() => {
    const start = (currentPage - 1) * 10;
    return sortedAndFilteredResults.slice(start, start + 10);
  }, [currentPage, sortedAndFilteredResults]);

  const examOptions = useMemo(
    () => ["All", ...new Set(results.map((r) => r.exam).filter(Boolean))],
    [results]
  );

  const slotOptions = useMemo(
    () => {
      const slots = new Set(results.map((r) => r.slot?.slotNumber).filter(Boolean));
      return ["All", ...Array.from(slots).sort((a, b) => a - b)];
    },
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
      await axios.delete(`/admin/results/${id}`, { withCredentials: true });
      setNotification({ message: 'Result deleted successfully!', type: 'success' });
      await fetchResults(selectedSlotNumber);
      await fetchSlotWiseData();
      await fetchStatistics();
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
      const response = await axios.put(
        `/admin/results/${id}/release`,
        {},
        { withCredentials: true }
      );
      await fetchResults(selectedSlotNumber);
      await fetchStatistics();
      
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
          axios.put(
            `/admin/results/${id}/release`,
            {},
            { withCredentials: true }
          )
        )
      );
      
      const successful = results.filter(r => r.status === 'fulfilled').length;
      const failed = results.filter(r => r.status === 'rejected').length;
      
      await fetchResults(selectedSlotNumber);
      await fetchStatistics();
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
      const response = await axios.put(
        "/admin/results/send-mails",
        {},
        { withCredentials: true }
      );
      
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
      
      await fetchResults(selectedSlotNumber);
      await fetchStatistics();
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
          axios.put(`/admin/results/${r.id}`, r, { withCredentials: true })
        )
      );
      await fetchResults(selectedSlotNumber);
      await fetchSlotWiseData();
      await fetchStatistics();
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
          axios.delete(`/admin/results/${id}`, { withCredentials: true })
        )
      );
      
      const successful = results.filter(r => r.status === 'fulfilled').length;
      const failed = results.filter(r => r.status === 'rejected').length;
      
      setNotification({ 
        message: `Deleted: ${successful} successful, ${failed} failed`, 
        type: successful > 0 ? "success" : "error" 
      });
      setSelectedResults([]);
      await fetchResults(selectedSlotNumber);
      await fetchSlotWiseData();
      await fetchStatistics();
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

  const resolveAnswerSheetContext = (result) => {
    const candidateId = result?.candidateId;
    const examId = result?.examId;
    const slotId = result?.slot?.id;

    if (!candidateId || !examId) {
      return null;
    }

    return { candidateId, examId, slotId };
  };

  const extractServerErrorMessage = (error, fallbackMessage) => {
    const errorData = error?.response?.data;
    if (typeof errorData === "string" && errorData.trim()) return errorData;
    if (errorData?.error) return errorData.error;
    if (errorData?.message) return errorData.message;
    return fallbackMessage;
  };

  const getFileNameFromDisposition = (disposition, fallbackName) => {
    if (!disposition) return fallbackName;
    const utfMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utfMatch?.[1]) return decodeURIComponent(utfMatch[1]);

    const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
    if (match?.[1]) return match[1];
    return fallbackName;
  };

  const handleViewDetails = async (result) => {
    const context = resolveAnswerSheetContext(result);
    if (!context) {
      setNotification({
        message: "Answer sheet cannot be opened because candidateId/examId is missing for this row.",
        type: "warning",
      });
      return;
    }

    try {
      setViewAnswerSheetLoadingId(result.id);
      const params = context.slotId ? { slotId: context.slotId } : {};
      const response = await axios.get(
        `/admin/answersheet/${context.candidateId}/${context.examId}`,
        { withCredentials: true, params }
      );

      setSelectedResultDetails(response.data);
      setIsDetailsModalOpen(true);
    } catch (err) {
      setNotification({
        message: extractServerErrorMessage(err, "Failed to fetch answer sheet"),
        type: "error",
      });
    } finally {
      setViewAnswerSheetLoadingId(null);
    }
  };

  const handleDownloadAnswerSheet = async (result) => {
    const context = resolveAnswerSheetContext(result);
    if (!context) {
      setNotification({
        message: "Answer sheet cannot be downloaded because candidateId/examId is missing for this row.",
        type: "warning",
      });
      return;
    }

    try {
      setDownloadAnswerSheetLoadingId(result.id);
      const params = context.slotId ? { slotId: context.slotId } : {};
      const response = await axios.get(
        `/admin/answersheet/download/${context.candidateId}/${context.examId}`,
        {
          withCredentials: true,
          params,
          responseType: "blob",
        }
      );

      const fallbackName = `answer_sheet_${context.candidateId}_${context.examId}.xlsx`;
      const filename = getFileNameFromDisposition(
        response.headers["content-disposition"],
        fallbackName
      );

      const blob = new Blob([response.data], {
        type: response.headers["content-type"] || "application/octet-stream",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      setNotification({ message: "Answer sheet downloaded successfully", type: "success" });
    } catch (err) {
      let message = "Failed to download answer sheet";
      if (err?.response?.data instanceof Blob) {
        try {
          const text = await err.response.data.text();
          const json = JSON.parse(text);
          message = json?.error || json?.message || message;
        } catch (parseError) {
          message = "Failed to download answer sheet";
        }
      } else {
        message = extractServerErrorMessage(err, message);
      }

      setNotification({ message, type: "error" });
    } finally {
      setDownloadAnswerSheetLoadingId(null);
    }
  };

  const handleEditSelected = () => {
    const selectedData = results.filter((r) => selectedResults.includes(r.id));
    setSelectedResultDetails(selectedData);
    setIsEditModalOpen(true);
  };

  const handleViewSlot = async (slotNumber) => {
    console.log("👀 Viewing slot:", slotNumber);
    await fetchResults(slotNumber);
    setCurrentPage(1);
  };

  const handleBackToAll = async () => {
    console.log("🔙 Back to all results");
    setViewMode("all");
    setSelectedSlotNumber(null);
    setSlotFilter("All");
    await fetchResults();
    setCurrentPage(1);
  };

  const handleShowSlots = () => {
    console.log("🎯 Switching to slot-wise view");
    setViewMode("slots");
    setCurrentPage(1);
    
    fetchSlotWiseData();
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
        <div>
          <h1 className="text-4xl font-bold text-gray-800">Candidate Results</h1>
          {selectedSlotNumber && (
            <p className="text-gray-600 mt-2">
              Viewing Slot {selectedSlotNumber} results
            </p>
          )}
        </div>
        <div className="flex space-x-2">
          {selectedSlotNumber && (
            <button
              onClick={handleBackToAll}
              className="bg-gray-600 text-white font-bold py-2 px-6 rounded-lg hover:bg-gray-700 shadow-md transition"
            >
              ← Back to All
            </button>
          )}
          <button
            onClick={handleAutoMailSend}
            className="bg-indigo-600 text-white font-bold py-2 px-6 rounded-lg hover:bg-indigo-700 shadow-md transition"
            disabled={loading}
          >
            📧 Send Mails Automatically
          </button>
        </div>
      </div>

      
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <div className="bg-white rounded-xl shadow-md p-6">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Total Results</h3>
            <p className="text-3xl font-bold text-blue-600">{statistics.totalResults}</p>
          </div>
          <div className="bg-white rounded-xl shadow-md p-6">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Pass Rate</h3>
            <p className="text-3xl font-bold text-green-600">{statistics.passPercentage}%</p>
            <p className="text-sm text-gray-500 mt-1">{statistics.totalPassed} passed</p>
          </div>
          <div className="bg-white rounded-xl shadow-md p-6">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Average Score</h3>
            <p className="text-3xl font-bold text-purple-600">{statistics.averageScore}</p>
          </div>
          <div className="bg-white rounded-xl shadow-md p-6">
            <h3 className="text-gray-600 text-sm font-semibold mb-2">Total Slots</h3>
            <p className="text-3xl font-bold text-orange-600">{statistics.totalSlots}</p>
          </div>
        </div>
      )}

      
      <div className="bg-white p-4 rounded-xl shadow-md mb-6 flex justify-between items-center">
        <div className="flex space-x-2">
          <button
            onClick={handleBackToAll}
            className={`px-4 py-2 rounded-lg font-semibold transition-colors ${
              viewMode === "all" || viewMode === "filtered"
                ? "bg-blue-600 text-white"
                : "bg-gray-200 text-gray-700 hover:bg-gray-300"
            }`}
          >
            📋 All Results
          </button>
          <button
            onClick={handleShowSlots}
            className={`px-4 py-2 rounded-lg font-semibold transition-colors ${
              viewMode === "slots"
                ? "bg-blue-600 text-white"
                : "bg-gray-200 text-gray-700 hover:bg-gray-300"
            }`}
          >
            🎯 Slot-wise View
          </button>
        </div>
        
        {(viewMode === "all" || viewMode === "filtered") && (
          <div className="text-sm text-gray-600">
            Showing {sortedAndFilteredResults.length} of {results.length} results
          </div>
        )}
      </div>

      
      {viewMode === "slots" && (
        <SlotWiseView 
          slotsData={slotsData} 
          onViewSlot={handleViewSlot}
          loading={slotLoading}
        />
      )}

     
      {(viewMode === "all" || viewMode === "filtered") && (
        <>
          
          <div className="bg-white p-4 rounded-xl shadow-md mb-6 grid grid-cols-1 md:grid-cols-4 gap-4">
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
                  {e === "All" ? "All Exams" : e}
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
              <option value="Pass">Pass</option>
              <option value="Fail">Fail</option>
            </select>
            <select
              value={slotFilter}
              onChange={(e) => setSlotFilter(e.target.value)}
              className="w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={selectedSlotNumber !== null}
            >
              {slotOptions.map((s) => (
                <option key={s} value={s}>
                  {s === "All" ? "All Slots" : `Slot ${s}`}
                </option>
              ))}
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

          {/* Results Table */}
          <div className="bg-white rounded-2xl shadow-xl overflow-x-auto">
            {loading ? (
              <div className="text-center py-12">
                <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                <p className="mt-4 text-gray-600">Loading results...</p>
              </div>
            ) : (
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
                    <th className="py-4 px-6 text-center cursor-pointer" onClick={() => requestSort("slot")}>
                      Slot {getSortIndicator("slot")}
                    </th>
                    <th className="py-4 px-6 text-center cursor-pointer" onClick={() => requestSort("score")}>
                      Score {getSortIndicator("score")}
                    </th>
                    <th className="py-4 px-6 text-center">Pass %</th>
                    <th className="py-4 px-6 text-center">Status</th>
                    <th className="py-4 px-6 text-center">Actions</th>
                  </tr>
                </thead>
                <tbody className="text-gray-700 text-sm font-light">
                  {paginatedResults.length === 0 ? (
                    <tr>
                      <td colSpan="11" className="py-8 text-center text-gray-500">
                        {selectedSlotNumber 
                          ? `No results found for Slot ${selectedSlotNumber}`
                          : "No results found"
                        }
                      </td>
                    </tr>
                  ) : (
                    paginatedResults.map((r) => {
                      const normalizedStatus = normalizeResultStatus(r.status);
                      return (
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
                        <td className="py-4 px-6 text-center">
                          {r.slot ? (
                            <span className="bg-purple-100 text-purple-800 px-2 py-1 rounded-full text-xs font-semibold">
                              Slot {r.slot.slotNumber}
                            </span>
                          ) : (
                            <span className="text-gray-400 text-xs">No Slot</span>
                          )}
                        </td>
                        <td className="py-4 px-6 text-center font-semibold text-blue-700">
                          {r.score}
                        </td>
                        <td className="py-4 px-6 text-center font-semibold text-indigo-700">
                          {formatPercent(r.scorePercentage)}
                        </td>
                        <td className="py-4 px-6 text-center">
                          <span
                            className={`px-3 py-1 rounded-full text-xs font-semibold ${normalizedStatus === "Result Released"
                                ? "bg-blue-100 text-blue-800"
                                : normalizedStatus === "Pass"
                                  ? "bg-green-100 text-green-800"
                                  : normalizedStatus === "Fail"
                                    ? "bg-red-100 text-red-800"
                                    : "bg-yellow-100 text-yellow-800"
                              }`}
                          >
                            {normalizedStatus}
                          </span>
                        </td>
                        <td className="py-4 px-6 text-center">
                          <div className="flex items-center justify-center space-x-2">
                            <button
                              onClick={() => handleViewDetails(r)}
                              disabled={viewAnswerSheetLoadingId === r.id}
                              className="bg-purple-600 text-white font-bold py-1 px-3 text-xs rounded-lg shadow-md hover:bg-purple-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
                              title="View Answer Sheet"
                            >
                              {viewAnswerSheetLoadingId === r.id ? "Loading..." : "View Answer Sheet"}
                            </button>
                            <button
                              onClick={() => handleDownloadAnswerSheet(r)}
                              disabled={downloadAnswerSheetLoadingId === r.id}
                              className="bg-indigo-600 text-white font-bold py-1 px-3 text-xs rounded-lg shadow-md hover:bg-indigo-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
                              title="Download Answer Sheet"
                            >
                              {downloadAnswerSheetLoadingId === r.id ? "Downloading..." : "Download Answer Sheet"}
                            </button>
                            <button
                              onClick={() => handleReleaseResult(r.id)}
                              disabled={normalizedStatus === "Result Released"}
                              className="bg-blue-600 text-white font-bold py-1 px-3 text-xs rounded-lg shadow-md hover:bg-blue-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
                              title="Release Result"
                            >
                              Release
                            </button>
                            <button
                              onClick={() => handleDeleteOne(r.id)}
                              className="bg-red-600 text-white font-bold py-1 px-3 text-xs rounded-lg shadow-md hover:bg-red-700 transition-colors"
                              title="Delete Result"
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    )})
                  )}
                </tbody>
              </table>
            )}

            
            <div className="flex justify-between items-center p-4 border-t">
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
        </>
      )}
    </div>
  );
}
