import React, { useState, useEffect, useRef } from "react";
import axios from "../../api/axiosConfig";
import { v4 as uuidv4 } from "uuid";
import { AlertCircle, CheckCircle, Shuffle, Eye, Trash2, RefreshCw } from "lucide-react";

const API_BASE_URL = "";

const sections = [
  { id: "A", name: "Quantitative Aptitude", marks: 1, type: "mcq" },
  { id: "B", name: "Logical Reasoning", marks: 1, type: "mcq" },
  { id: "C", name: "Verbal Ability", marks: 1, type: "mcq" },
  { id: "D", name: "Technical MCQ's", marks: 2, type: "mcq" },
];

const Notification = ({ message, type, onClose }) => {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [message, onClose]);

  if (!message) return null;

  const Icon = type === 'success' ? CheckCircle : AlertCircle;
  const bgColor = type === 'success' ? 'bg-green-500' : 'bg-red-500';

  return (
    <div className={`fixed top-5 right-5 p-4 rounded-lg shadow-lg text-white z-50 ${bgColor} flex items-center gap-2 max-w-md`}>
      <Icon className="w-5 h-5 flex-shrink-0" />
      <span>{message}</span>
    </div>
  );
};

export default function QuestionBank() {
  const [questions, setQuestions] = useState([]);
  const [exams, setExams] = useState([]);
  const [selectedExam, setSelectedExam] = useState("");
  const [activeTab, setActiveTab] = useState("questions");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [questionToDelete, setQuestionToDelete] = useState(null);
  const [notification, setNotification] = useState({ message: "", type: "" });
  
  const [questionSets, setQuestionSets] = useState([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [viewingSet, setViewingSet] = useState(null);
  const [viewSetQuestions, setViewSetQuestions] = useState([]);
  const [loadingQuestions, setLoadingQuestions] = useState(false);
  const [uploadingSection, setUploadingSection] = useState(null);
  const [selectedQuestionIds, setSelectedQuestionIds] = useState(new Set());
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const fileInputRef = useRef(null);
  const uploadSectionRef = useRef(null);

  const initialFormData = {
    id: null,
    section: "A",
    examId: "",
    type: "mcq",
    questionText: "",
    options: ["", "", "", ""],
    answer: "",
    marks: 1,
    qNo: null,
  };
  const [formData, setFormData] = useState(initialFormData);

  const axiosConfig = {
    withCredentials: true
  };

  const fetchExams = async () => {
    try { 
      const res = await axios.get(`${API_BASE_URL}/admin/exams`, axiosConfig); 
      const examList = Array.isArray(res.data)
        ? res.data
        : Array.isArray(res.data?.exams)
          ? res.data.exams
          : [];
      setExams(examList); 
    } 
    catch (err) {
      console.error("Error fetching exams:", err);
      setNotification({ message: "Failed to load exams", type: "error" });
    }
  };

  const fetchQuestions = async () => {
    if (!selectedExam) return;
    try { 
      const res = await axios.get(`${API_BASE_URL}/admin/exams/${selectedExam}/questions`, axiosConfig);
      const questionsList = Array.isArray(res.data) ? res.data : [];
      setQuestions(questionsList); 
    } 
    catch (err) { 
      console.error("Error fetching questions:", err);
      setNotification({ message: "Failed to load questions", type: "error" });
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
    document.body.classList.add('admin-dashboard-body');
    return () => {
      document.body.classList.remove('admin-dashboard-body');
    };
  }, []);

  const fetchQuestionSets = async () => {
    if (!selectedExam) return;
    try {
      const response = await axios.get(`${API_BASE_URL}/examset/${selectedExam}/sets`, axiosConfig);
      const data = Array.isArray(response.data) ? response.data : [];
      setQuestionSets(data);
    } catch (err) {
      console.error("Error fetching question sets:", err);
      setQuestionSets([]);
    }
  };

  useEffect(() => { fetchExams(); }, []);
  useEffect(() => { 
    fetchQuestions();
    if (activeTab === "sets") {
      fetchQuestionSets();
    }
  }, [selectedExam, activeTab]);
  
  useEffect(() => {
    setSelectedQuestionIds(new Set());
  }, [selectedExam]);

  useEffect(() => {
    setSelectedQuestionIds(prev => {
      if (!prev.size) return prev;
      const validIds = new Set(questions.map(q => q.id));
      const next = new Set([...prev].filter(id => validIds.has(id)));
      return next;
    });
  }, [questions]);

  const handleGenerateSets = async () => {
    if (!selectedExam) {
      setNotification({ message: "Please select an exam first", type: "error" });
      return;
    }

    setIsGenerating(true);
    try {
      const response = await axios.post(
        `${API_BASE_URL}/examset/${selectedExam}/generate`,
        {},
        axiosConfig
      );
      const data = response.data || {};
      if (data.success === false) {
        throw new Error(data.error || data.message || "Failed to generate question sets");
      }
      setNotification({ 
        message: `Successfully generated ${data.numberOfSets || 5} question sets with ${data.totalQuestions || 0} questions!`, 
        type: "success" 
      });
      await fetchQuestionSets();
    } catch (err) {
      console.error("Error generating sets:", err);
      setNotification({ 
        message: err.response?.data?.error || err.message || "Unable to generate question sets.", 
        type: "error" 
      });
    } finally {
      setIsGenerating(false);
    }
  };

  const handleDeleteSets = async () => {
    if (!selectedExam) return;
    
    if (!window.confirm("Are you sure you want to delete all question sets?")) {
      return;
    }

    setIsDeleting(true);
    try {
      const response = await axios.post(
        `${API_BASE_URL}/examset/${selectedExam}/delete-sets`,
        {},
        axiosConfig
      );
      const data = response.data || {};
      if (data.success === false) {
        throw new Error(data.error || data.message || "Failed to delete sets");
      }
      setNotification({ message: "Question sets deleted successfully!", type: "success" });
      setQuestionSets([]);
    } catch (err) {
      console.error("Error deleting sets:", err);
      setNotification({ message: err.response?.data?.error || "Failed to delete question sets", type: "error" });
    } finally {
      setIsDeleting(false);
    }
  };

  const handleViewSet = async (set) => {
    setLoadingQuestions(true);
    setViewingSet(set.setNumber);
    
    try {
      const questionIds = set.questionIds || [];
      
      if (questionIds.length === 0) {
        setViewSetQuestions([]);
        setNotification({ message: "No questions found in this set", type: "error" });
        return;
      }

      const response = await axios.get(`${API_BASE_URL}/admin/exams/${selectedExam}/questions`, axiosConfig);
      const allQuestions = Array.isArray(response.data) ? response.data : [];
      
      const questionMap = {};
      allQuestions.forEach(q => {
        questionMap[q.id] = q;
      });

      const orderedQuestions = questionIds
        .map(id => questionMap[parseInt(id)])
        .filter(q => q !== undefined);

      setViewSetQuestions(orderedQuestions);
      
      if (orderedQuestions.length === 0) {
        setNotification({ message: "Could not load questions for this set", type: "error" });
      }
    } catch (err) {
      console.error("Error viewing set:", err);
      setNotification({ message: "Failed to load set questions: " + err.message, type: "error" });
      setViewSetQuestions([]);
    } finally {
      setLoadingQuestions(false);
    }
  };

  const openModal = (question = null, sectionId = null) => {
    setEditingQuestion(question);
    if (question) { 
      setFormData({ 
        ...initialFormData, 
        ...question,
        examId: question.examId || selectedExam, 
        questionText: question.questionText || question.question || '', 
        options: question.options || ["", "", "", ""],
      }); 
    } 
    else if (sectionId) {
      const section = sections.find(s => s.id === sectionId);
      setFormData({
        ...initialFormData,
        id: uuidv4(),
        examId: selectedExam,
        section: sectionId,
        type: section.type,
        marks: section.marks,
      });
    }
    setIsModalOpen(true);
  };

  const closeModal = () => { 
    setIsModalOpen(false); 
    setEditingQuestion(null); 
    setFormData(initialFormData); 
  };

  const openDeleteModal = (question) => { 
    setQuestionToDelete(question); 
    setIsDeleteModalOpen(true); 
  };

  const closeDeleteModal = () => { 
    setQuestionToDelete(null); 
    setIsDeleteModalOpen(false); 
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.questionText || !formData.examId) {
      setNotification({ message: "Please fill required fields.", type: "error" });
      return;
    }

    try {
      const payload = {
        section: formData.section,
        type: formData.type,
        questionText: formData.questionText,
        marks: formData.marks,
        qNo: editingQuestion 
            ? formData.qNo 
            : questions.filter(q => q.section === formData.section).length + 1,
        options: formData.options,
        answer: formData.answer,
      };

      if (editingQuestion) {
        await axios.put(
          `${API_BASE_URL}/admin/exams/${formData.examId}/questions/${editingQuestion.id}`, 
          payload, 
          axiosConfig
        );
        setNotification({ message: "Question updated successfully!", type: "success" });
      } 
      else {
        await axios.post(
          `${API_BASE_URL}/admin/exams/${formData.examId}/questions`,
          payload, 
          axiosConfig
        );
        setNotification({ message: "Question added successfully!", type: "success" });
      }
      fetchQuestions();
      closeModal();
    } 
    catch (err) { 
      console.error("Error saving question:", err);
      setNotification({ message: "Failed to save question", type: "error" });
    }
  };

  const handleDelete = async () => {
    const examId = questionToDelete.examId || selectedExam; 
    const questionId = questionToDelete.id;

    try {
      await axios.delete(
        `${API_BASE_URL}/admin/exams/${examId}/questions/${questionId}`,
        axiosConfig
      );
      setNotification({ message: "Question deleted successfully.", type: "success" });
      fetchQuestions();
      closeDeleteModal();
    } catch (err) { 
      console.error("Error deleting question:", err.response);
      setNotification({ message: "Failed to delete question", type: "error" });
    }
  };

  const toggleQuestionSelection = (questionId, checked) => {
    setSelectedQuestionIds(prev => {
      const next = new Set(prev);
      if (checked) {
        next.add(questionId);
      } else {
        next.delete(questionId);
      }
      return next;
    });
  };

  const toggleSectionSelection = (sectionId, checked) => {
    const sectionIds = questions
      .filter(q => q.section === sectionId)
      .map(q => q.id);

    setSelectedQuestionIds(prev => {
      const next = new Set(prev);
      sectionIds.forEach(id => {
        if (checked) next.add(id);
        else next.delete(id);
      });
      return next;
    });
  };

  const toggleAllSelection = (checked) => {
    if (!checked) {
      setSelectedQuestionIds(new Set());
      return;
    }
    setSelectedQuestionIds(new Set(questions.map(q => q.id)));
  };

  const handleDeleteSelected = async () => {
    if (!selectedQuestionIds.size) {
      setNotification({ message: "Please select at least one question", type: "error" });
      return;
    }

    if (!window.confirm(`Are you sure you want to delete ${selectedQuestionIds.size} selected question(s)?`)) {
      return;
    }

    setBulkDeleting(true);
    try {
      const idsToDelete = Array.from(selectedQuestionIds);
      const results = await Promise.allSettled(
        idsToDelete.map(questionId =>
          axios.delete(
            `${API_BASE_URL}/admin/exams/${selectedExam}/questions/${questionId}`,
            axiosConfig
          )
        )
      );

      const successCount = results.filter(r => r.status === "fulfilled").length;
      const failedCount = results.length - successCount;

      if (successCount > 0) {
        await fetchQuestions();
      }

      setSelectedQuestionIds(new Set());
      if (failedCount === 0) {
        setNotification({ message: `Deleted ${successCount} question(s) successfully`, type: "success" });
      } else {
        setNotification({
          message: `Deleted ${successCount} question(s), failed ${failedCount}`,
          type: "error",
        });
      }
    } catch (err) {
      console.error("Error deleting selected questions:", err);
      setNotification({ message: "Failed to delete selected questions", type: "error" });
    } finally {
      setBulkDeleting(false);
    }
  };

  const triggerUploadFilePicker = (sectionId) => {
    if (!selectedExam) {
      setNotification({ message: "Please select an exam first", type: "error" });
      return;
    }
    uploadSectionRef.current = sectionId;
    setUploadingSection(sectionId);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
      fileInputRef.current.click();
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const allowedExtPattern = /\.(xlsx|xls|csv|txt|pdf|docx|doc)$/i;
    if (!allowedExtPattern.test(file.name || "")) {
      setNotification({
        message: "Unsupported file. Allowed: xlsx, xls, csv, txt, pdf, docx, doc",
        type: "error",
      });
      setUploadingSection(null);
      uploadSectionRef.current = null;
      if (fileInputRef.current) fileInputRef.current.value = "";
      return;
    }

    if (!selectedExam) {
      setNotification({ message: "Please select an exam first", type: "error" });
      setUploadingSection(null);
      uploadSectionRef.current = null;
      if (fileInputRef.current) fileInputRef.current.value = "";
      return;
    }

    const sectionId = uploadSectionRef.current;
    setUploadingSection(sectionId || "uploading");

    try {
      const formData = new FormData();
      formData.append("file", file);
      if (sectionId) {
        formData.append("section", sectionId);
      }

      const uploadConfig = {
        ...axiosConfig,
        headers: {
          ...(axiosConfig.headers || {}),
          "Content-Type": "multipart/form-data",
        },
      };

      const res = await axios.post(
        `${API_BASE_URL}/admin/exams/${selectedExam}/questions/upload`,
        formData,
        uploadConfig
      );

      const createdCount = res.data?.createdCount || 0;
      const failedCount = res.data?.failedCount || 0;
      const failedRows = Array.isArray(res.data?.failedRows) ? res.data.failedRows : [];

      const previewErrors = failedRows.slice(0, 2).join(" | ");
      const message = failedCount > 0
        ? `Imported ${createdCount} question(s), ${failedCount} failed${previewErrors ? ` (${previewErrors}${failedRows.length > 2 ? " ..." : ""})` : ""}`
        : `Imported ${createdCount} question(s) successfully`;

      setNotification({
        message,
        type: failedCount > 0 ? "error" : "success",
      });

      await fetchQuestions();
    } catch (err) {
      console.error("Error uploading questions:", err);
      const errorMsg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Failed to upload questions";
      setNotification({ message: errorMsg, type: "error" });
    } finally {
      setUploadingSection(null);
      uploadSectionRef.current = null;
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const getAnswerText = (q) => {
    const answerKey = q.answer?.toUpperCase();
    const optionIndex = answerKey ? ["A", "B", "C", "D"].indexOf(answerKey) : -1;
    
    if (optionIndex !== -1 && q.options && q.options.length > optionIndex) {
      return `${answerKey}) ${q.options[optionIndex]}`;
    }
    return "N/A";
  };

  const allQuestionsSelected =
    questions.length > 0 && questions.every(q => selectedQuestionIds.has(q.id));

  return (
    <div className="min-h-screen">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <div className="relative overflow-hidden rounded-3xl border border-white/70 bg-white/80 backdrop-blur-sm shadow-[0_30px_80px_-50px_rgba(15,23,42,0.45)] p-6 sm:p-8">
          <div className="pointer-events-none absolute -top-24 right-0 h-48 w-48 rounded-full bg-blue-200/40 blur-3xl"></div>
          <div className="pointer-events-none absolute -bottom-24 -left-16 h-40 w-40 rounded-full bg-indigo-200/40 blur-3xl"></div>

          <div className="relative z-10">
      <Notification 
        message={notification.message} 
        type={notification.type} 
        onClose={() => setNotification({ message: "", type: "" })} 
      />
      <h1 className="text-4xl font-bold mb-6 text-gray-800">Question Bank</h1>
      <input
        ref={fileInputRef}
        type="file"
        accept=".xlsx,.xls,.csv,.txt,.pdf,.doc,.docx"
        className="hidden"
        onChange={handleFileUpload}
      />

      <div className="bg-white p-4 rounded-xl shadow-md mb-8">
        <label htmlFor="exam-select" className="block text-lg font-semibold text-gray-700 mb-2">
          Select Exam
        </label>
        <select 
          id="exam-select" 
          value={selectedExam} 
          onChange={e => setSelectedExam(e.target.value)} 
          className="w-full max-w-md p-3 border rounded-lg"
        >
          <option value="">-- Choose an Exam --</option>
          {(Array.isArray(exams) ? exams : []).map(exam => (
            <option key={exam.id} value={exam.id}>{exam.title}</option>
          ))}
        </select>
      </div>

      {selectedExam && (
        <>
          <div className="bg-white rounded-xl shadow-md mb-8">
            <div className="flex border-b">
              <button
                onClick={() => setActiveTab("questions")}
                className={`flex-1 py-4 px-6 font-semibold text-lg transition ${
                  activeTab === "questions"
                    ? "border-b-4 border-blue-600 text-blue-600"
                    : "text-gray-600 hover:text-gray-800"
                }`}
              >
                Questions
              </button>
              <button
                onClick={() => setActiveTab("sets")}
                className={`flex-1 py-4 px-6 font-semibold text-lg transition ${
                  activeTab === "sets"
                    ? "border-b-4 border-blue-600 text-blue-600"
                    : "text-gray-600 hover:text-gray-800"
                }`}
              >
                Question Sets
              </button>
            </div>
          </div>

          {activeTab === "questions" && (
            <>
              <div className="bg-white rounded-xl shadow-md p-4 mb-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <label className="inline-flex items-center gap-2 text-gray-700 font-medium">
                    <input
                      type="checkbox"
                      className="h-4 w-4 accent-blue-600"
                      checked={allQuestionsSelected}
                      onChange={(e) => toggleAllSelection(e.target.checked)}
                    />
                    Select All Questions
                  </label>
                  <button
                    onClick={handleDeleteSelected}
                    disabled={!selectedQuestionIds.size || bulkDeleting}
                    className="bg-red-600 text-white py-2 px-4 rounded-lg hover:bg-red-700 disabled:bg-red-300 disabled:cursor-not-allowed"
                  >
                    {bulkDeleting ? "Deleting..." : `Delete Selected (${selectedQuestionIds.size})`}
                  </button>
                </div>
              </div>

              {sections.map(section => {
                const sectionQuestions = questions
                  .filter(q => q.section === section.id)
                  .sort((a, b) => a.qNo - b.qNo);
                const selectedCountInSection = sectionQuestions.filter(q => selectedQuestionIds.has(q.id)).length;
                const sectionAllSelected =
                  sectionQuestions.length > 0 && selectedCountInSection === sectionQuestions.length;
                
                return (
                  <div key={section.id} className="mb-8">
                    <div className="flex justify-between items-center mb-4">
                      <h2 className="text-2xl font-semibold text-gray-700">
                        Section {section.id}: {section.name}
                      </h2>
                      <div className="flex items-center gap-2">
                        <label className="inline-flex items-center gap-2 text-sm text-gray-700 font-medium mr-2">
                          <input
                            type="checkbox"
                            className="h-4 w-4 accent-blue-600"
                            checked={sectionAllSelected}
                            onChange={(e) => toggleSectionSelection(section.id, e.target.checked)}
                            disabled={!sectionQuestions.length}
                          />
                          Select All
                        </label>
                        <button
                          onClick={() => triggerUploadFilePicker(section.id)}
                          className="bg-green-600 text-white py-2 px-4 rounded-lg hover:bg-green-700 disabled:bg-green-400 disabled:cursor-not-allowed"
                          disabled={uploadingSection === section.id || uploadingSection === "uploading"}
                        >
                          {uploadingSection === section.id || uploadingSection === "uploading" ? "Uploading..." : "Upload Files"}
                        </button>
                        <button 
                          onClick={() => openModal(null, section.id)} 
                          className="bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700"
                        >
                          + Add Question
                        </button>
                      </div>
                    </div>
                    <div className="bg-white rounded-xl shadow-md overflow-hidden">
                      <table className="min-w-full bg-white">
                        <thead className="bg-gray-200 text-gray-600 uppercase text-sm">
                          <tr>
                            <th className="py-3 px-4 text-center w-16">Select</th>
                            <th className="py-3 px-4 text-left">Q.No</th>
                            <th className="py-3 px-4 text-left">Question Text</th>
                            <th className="py-3 px-4 text-center">Marks</th>
                            <th className="py-3 px-4 text-left">Correct Answer</th>
                            <th className="py-3 px-4 text-center">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="text-gray-700">
                          {sectionQuestions.map(q => (
                            <tr key={q.id} className="border-b border-gray-200 hover:bg-gray-50">
                              <td className="py-3 px-4 text-center">
                                <input
                                  type="checkbox"
                                  className="h-4 w-4 accent-blue-600"
                                  checked={selectedQuestionIds.has(q.id)}
                                  onChange={(e) => toggleQuestionSelection(q.id, e.target.checked)}
                                />
                              </td>
                              <td className="py-3 px-4 font-semibold">{q.qNo}</td>
                              <td className="py-3 px-4">{q.questionText}</td>
                              <td className="py-3 px-4 text-center font-semibold">{q.marks}</td>
                              <td className="py-3 px-4">{getAnswerText(q)}</td>
                              <td className="py-3 px-4 text-center">
                                <button 
                                  onClick={() => openModal(q)} 
                                  className="text-blue-600 hover:text-blue-800 mr-4 font-medium"
                                >
                                  Edit
                                </button>
                                <button 
                                  onClick={() => openDeleteModal(q)} 
                                  className="text-red-600 hover:text-red-800 font-medium"
                                >
                                  Delete
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                );
              })}
            </>
          )}

          {activeTab === "sets" && (
            <>
              <div className="bg-white p-6 rounded-xl shadow-md mb-8">
                <div className="flex gap-4 flex-wrap">
                  <button
                    onClick={handleGenerateSets}
                    disabled={isGenerating}
                    className="flex items-center gap-2 bg-blue-600 text-white py-3 px-6 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
                  >
                    {isGenerating ? (
                      <>
                        <RefreshCw className="w-5 h-5 animate-spin" />
                        Generating...
                      </>
                    ) : (
                      <>
                        <Shuffle className="w-5 h-5" />
                        Generate 5 Question Sets
                      </>
                    )}
                  </button>

                  {questionSets.length > 0 && (
                    <button
                      onClick={handleDeleteSets}
                      disabled={isDeleting}
                      className="flex items-center gap-2 bg-red-600 text-white py-3 px-6 rounded-lg hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
                    >
                      {isDeleting ? (
                        <>
                          <RefreshCw className="w-5 h-5 animate-spin" />
                          Deleting...
                        </>
                      ) : (
                        <>
                          <Trash2 className="w-5 h-5" />
                          Delete All Sets
                        </>
                      )}
                    </button>
                  )}
                </div>
                
                <p className="text-sm text-gray-600 mt-4">
                  Generate 5 different shuffled versions of the exam. Questions are shuffled within each section. Students will be automatically assigned to different sets to prevent cheating.
                </p>
              </div>

              {questionSets.length > 0 ? (
                <div className="bg-white rounded-xl shadow-md overflow-hidden">
                  <div className="p-6 border-b">
                    <h2 className="text-2xl font-semibold text-gray-700">Generated Question Sets</h2>
                    <p className="text-gray-600 mt-1">5 different shuffled versions of the exam</p>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="min-w-full">
                      <thead className="bg-gray-200 text-gray-600 uppercase text-sm">
                        <tr>
                          <th className="py-4 px-6 text-left">Set Number</th>
                          <th className="py-4 px-6 text-center">Total Questions</th>
                          <th className="py-4 px-6 text-left">Created At</th>
                          <th className="py-4 px-6 text-center">Status</th>
                          <th className="py-4 px-6 text-center">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="text-gray-700">
                        {questionSets.map(set => (
                          <tr key={set.id} className="border-b border-gray-200 hover:bg-gray-50">
                            <td className="py-4 px-6">
                              <span className="font-semibold text-lg">Set {set.setNumber}</span>
                            </td>
                            <td className="py-4 px-6 text-center">
                              <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full font-semibold">
                                {set.questionIds?.length || 0} Questions
                              </span>
                            </td>
                            <td className="py-4 px-6">
                              {new Date(set.createdAt).toLocaleString()}
                            </td>
                            <td className="py-4 px-6 text-center">
                              {set.isActive ? (
                                <span className="inline-block bg-green-100 text-green-800 px-3 py-1 rounded-full text-sm font-semibold">
                                  Active
                                </span>
                              ) : (
                                <span className="inline-block bg-gray-100 text-gray-800 px-3 py-1 rounded-full text-sm font-semibold">
                                  Inactive
                                </span>
                              )}
                            </td>
                            <td className="py-4 px-6 text-center">
                              <button
                                onClick={() => handleViewSet(set)}
                                className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 font-medium"
                              >
                                <Eye className="w-4 h-4" />
                                View Questions
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : (
                <div className="text-center py-16 bg-white rounded-xl shadow-md">
                  <Shuffle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                  <p className="text-gray-500 text-lg mb-2">No question sets generated yet</p>
                  <p className="text-gray-400 mb-6">Click "Generate 5 Question Sets" to create shuffled versions</p>
                </div>
              )}
            </>
          )}
        </>
      )}

      {!selectedExam && (
        <div className="text-center py-10 bg-white rounded-xl shadow-md text-gray-500 text-lg">
          Select an exam to manage questions
        </div>
      )}

      {isModalOpen && (
        <QuestionModal
          formData={formData}
          setFormData={setFormData}
          closeModal={closeModal}
          handleSubmit={handleSubmit}
          sections={sections}
          exams={exams}
          editingQuestion={editingQuestion}
        />
      )}

      {isDeleteModalOpen && (
        <DeleteModal closeModal={closeDeleteModal} handleDelete={handleDelete} />
      )}

      {viewingSet && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col">
            <div className="p-6 border-b flex justify-between items-center">
              <h2 className="text-2xl font-bold">Set {viewingSet} - Question Order</h2>
              <button 
                onClick={() => {
                  setViewingSet(null);
                  setViewSetQuestions([]);
                }}
                className="text-gray-500 hover:text-gray-700 text-2xl"
              >
                ✕
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1">
              {loadingQuestions ? (
                <div className="text-center py-8">
                  <RefreshCw className="w-8 h-8 text-blue-600 animate-spin mx-auto mb-2" />
                  <p className="text-gray-500">Loading questions...</p>
                </div>
              ) : viewSetQuestions.length > 0 ? (
                <div className="space-y-4">
                  {viewSetQuestions.map((q, idx) => (
                    <div key={q.id} className="p-4 border rounded-lg hover:bg-gray-50">
                      <div className="flex gap-4">
                        <div className="flex-shrink-0">
                          <span className="inline-block bg-blue-600 text-white w-8 h-8 rounded-full flex items-center justify-center font-semibold">
                            {idx + 1}
                          </span>
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2">
                            <span className="inline-block bg-gray-200 text-gray-700 px-2 py-1 rounded text-xs font-semibold">
                              Section {q.section}
                            </span>
                            <span className="inline-block bg-green-100 text-green-800 px-2 py-1 rounded text-xs font-semibold">
                              {q.marks} Mark{q.marks > 1 ? 's' : ''}
                            </span>
                          </div>
                          <p className="text-gray-800 font-medium mb-2">{q.questionText}</p>
                          {q.options && q.options.length > 0 && (
                            <div className="mt-2 space-y-1">
                              {q.options.map((opt, i) => (
                                <div key={i} className="text-sm p-2 rounded bg-gray-50">
                                  <span className="font-semibold text-gray-600">
                                    {String.fromCharCode(65 + i)})
                                  </span>
                                  <span className="ml-2 text-gray-700">
                                    {opt}
                                  </span>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-center text-gray-500 py-8">No questions found in this set</p>
              )}
            </div>
            
            <div className="p-4 bg-gray-50 border-t flex justify-end">
              <button 
                onClick={() => {
                  setViewingSet(null);
                  setViewSetQuestions([]);
                }}
                className="bg-gray-200 py-2 px-6 rounded-lg hover:bg-gray-300"
              >
                Close
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

const QuestionModal = ({ formData, setFormData, closeModal, handleSubmit, sections, exams, editingQuestion }) => {
  const currentSection = sections.find(s => s.id === formData.section);
  
  const getExamTitle = (examId) => {
    const exam = exams.find(e => String(e.id) === String(examId));
    return exam ? exam.title : "N/A";
  };
  
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };
  
  const handleOptionChange = (index, value) => { 
    const newOptions = [...formData.options]; 
    newOptions[index] = value;
    setFormData({ ...formData, options: newOptions }); 
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-40 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl">
        <form onSubmit={handleSubmit}>
          <div className="p-6 border-b">
            <h2 className="text-2xl font-bold">
              {editingQuestion ? "Edit Question" : "Add Question"}
            </h2>
          </div>
          
          <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
            <input type="hidden" value={formData.examId} />
            
            <div className="p-3 bg-gray-100 rounded-lg">
              <p><span className="font-semibold">Exam:</span> {getExamTitle(formData.examId)}</p>
              <p><span className="font-semibold">Section:</span> {currentSection?.name}</p>
              <p><span className="font-semibold">Question Type:</span> Multiple Choice</p>
            </div>
            
            <div>
              <label className="block text-gray-700 font-medium mb-1">Marks</label>
              <input 
                type="number" 
                name="marks"
                placeholder="Marks" 
                value={formData.marks} 
                onChange={handleInputChange} 
                className="w-full p-2 border rounded-lg"
                required
              />
            </div>
            
            <div>
              <label className="block text-gray-700 font-medium mb-1">Question Text</label>
              <textarea 
                placeholder="Enter the question" 
                name="questionText"
                value={formData.questionText} 
                onChange={handleInputChange} 
                rows="3" 
                className="w-full p-2 border rounded-lg"
                required
              />
            </div>

            <div className="space-y-3">
              <p className="font-semibold text-gray-700">Multiple Choice Options</p>
              {formData.options.map((opt, idx) => (
                <div key={idx} className="flex items-center space-x-2">
                  <label className="font-semibold w-8">
                    {String.fromCharCode(65 + idx)})
                  </label>
                  <input 
                    type="text" 
                    value={opt} 
                    onChange={e => handleOptionChange(idx, e.target.value)} 
                    className="w-full p-2 border rounded-lg" 
                    placeholder={`Option ${String.fromCharCode(65 + idx)}`}
                    required
                  />
                </div>
              ))}
              
              <div>
                <label className="block text-gray-700 font-medium mb-1">Correct Answer</label>
                <select 
                  value={formData.answer} 
                  onChange={e => setFormData({ ...formData, answer: e.target.value })} 
                  className="w-full p-2 border rounded-lg bg-white" 
                  required
                >
                  <option value="">Select Correct Answer (A, B, C, or D)</option>
                  <option value="A">A</option>
                  <option value="B">B</option>
                  <option value="C">C</option>
                  <option value="D">D</option>
                </select>
              </div>
            </div>
          </div>
          
          <div className="p-4 bg-gray-50 border-t flex justify-end space-x-2">
            <button 
              type="button" 
              onClick={closeModal} 
              className="bg-gray-200 py-2 px-5 rounded-lg hover:bg-gray-300"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="bg-blue-600 text-white py-2 px-5 rounded-lg hover:bg-blue-700"
            >
              Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};


const DeleteModal = ({ closeModal, handleDelete }) => (
  <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
    <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md">
      <h3 className="text-2xl font-bold mb-4">Confirm Deletion</h3>
      <p className="text-gray-700 mb-6">Are you sure you want to delete this question?</p>
      <div className="flex justify-end space-x-3">
        <button 
          onClick={closeModal} 
          className="bg-gray-300 text-gray-800 py-2 px-5 rounded-lg hover:bg-gray-400"
        >
          Cancel
        </button>
        <button 
          onClick={handleDelete} 
          className="bg-red-600 text-white py-2 px-5 rounded-lg hover:bg-red-700"
        >
          Delete
        </button>
      </div>
    </div>
  </div>
);
