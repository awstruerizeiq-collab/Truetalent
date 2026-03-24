import React, { useState, useEffect, useRef } from "react";
import { Clock, User, BookOpen, AlertCircle, ChevronLeft, ChevronRight, Video, VideoOff, AlertTriangle, CheckCircle } from "lucide-react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import axios from "../../api/axiosConfig";
import truerizeLogo from "../../assets/images/Truerize_Logo.png";

const sectionMap = [
  { id: "A", name: "Quantitative Aptitude", marks: 1, type: "mcq" },
  { id: "B", name: "Logical Reasoning", marks: 1, type: "mcq" },
  { id: "C", name: "Verbal Ability", marks: 1, type: "mcq" },
  { id: "D", name: "Technical MCQ's", marks: 2, type: "mcq" },
];

const QuizInterface = () => {
  const navigate = useNavigate();
  const urlParams = useParams();
  const location = useLocation();
  const stateExamId = location.state?.examId ? String(location.state.examId) : null;
  const sessionExamId = sessionStorage.getItem("currentExamId");
  const liveExamId = urlParams.examId || stateExamId || sessionExamId || null;

  const [examData, setExamData] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [timeLeft, setTimeLeft] = useState(0);
  const [answers, setAnswers] = useState({});
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [showSidebar, setShowSidebar] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showSuccessScreen, setShowSuccessScreen] = useState(false);
  const [tabSwitchCount, setTabSwitchCount] = useState(0);
  const MAX_TAB_SWITCHES = 2;
  const isAutoSubmittingRef = useRef(false);
  const [backButtonCount, setBackButtonCount] = useState(0);
  const MAX_BACK_ATTEMPTS = 1;

  // ✅ NEW: Submit confirmation modal state
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Camera state
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState(null);
  const videoRef = useRef(null);
  const streamRef = useRef(null);

  const [studentId, setStudentId] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [assignedSetNumber, setAssignedSetNumber] = useState(null);
  const [isAutoAssigning, setIsAutoAssigning] = useState(false);
  const [userName, setUserName] = useState("Loading...");
  const [userEmail, setUserEmail] = useState("");

  const getStorageKey = (key) => `exam_${liveExamId}_${studentId}_${key}`;

  useEffect(() => {
    document.body.classList.remove("admin-dashboard-body", "login-page-body");
    document.body.classList.add("candidate-body");
    return () => {
      document.body.classList.remove("candidate-body");
    };
  }, []);

  // Back button protection
  useEffect(() => {
    if (isSubmitted || showSuccessScreen) return;

    const stateObj = { page: 'quiz', timestamp: Date.now() };
    window.history.pushState(stateObj, "", window.location.href);
    
    const handlePopState = (event) => {
      window.history.pushState(stateObj, "", window.location.href);
      
      setBackButtonCount(prev => {
        const newCount = prev + 1;

        if (newCount > MAX_BACK_ATTEMPTS) {
          if (!isAutoSubmittingRef.current) {
            alert("⚠️ Second back button attempt detected!\n\n🔴 Your exam will be submitted automatically now.");
            setTimeout(() => {
              performExamSubmission();
            }, 1000);
          }
        } else {
          alert("⚠️ WARNING: Back button detected!\n\n⚡ This is your first warning.\n\n🔴 If you press back again, your exam will be AUTOMATICALLY SUBMITTED.\n\nPlease use the 'Submit Exam' button to exit properly.");
        }

        return newCount;
      });
    };

    window.addEventListener("popstate", handlePopState);

    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, [isSubmitted, showSuccessScreen, backButtonCount]);

  // Prevent accidental page close
  useEffect(() => {
    if (isSubmitted || showSuccessScreen) return;

    const handleBeforeUnload = (e) => {
      e.preventDefault();
      e.returnValue = "Are you sure you want to leave? Your exam progress may be lost.";
      return e.returnValue;
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [isSubmitted, showSuccessScreen]);

  // Disable navigation shortcuts
  useEffect(() => {
    if (isSubmitted || showSuccessScreen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Backspace' && !['INPUT', 'TEXTAREA'].includes(e.target.tagName)) {
        e.preventDefault();
        alert("⚠️ Navigation shortcuts are disabled during the exam!");
      }

      if (e.altKey && (e.key === 'ArrowLeft' || e.key === 'ArrowRight')) {
        e.preventDefault();
        alert("⚠️ Navigation shortcuts are disabled during the exam!");
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isSubmitted, showSuccessScreen]);

  // Load saved data
  const loadSavedData = () => {
    try {
      const savedAnswers = sessionStorage.getItem(getStorageKey('answers'));
      const savedTimeLeft = sessionStorage.getItem(getStorageKey('timeLeft'));
      const savedQuestionIndex = sessionStorage.getItem(getStorageKey('currentQuestionIndex'));
      const savedTabSwitchCount = sessionStorage.getItem(getStorageKey('tabSwitchCount'));
      const savedBackButtonCount = sessionStorage.getItem(getStorageKey('backButtonCount'));

      if (savedAnswers) {
        setAnswers(JSON.parse(savedAnswers));
      }

      if (savedTimeLeft) {
        const parsedTime = parseInt(savedTimeLeft, 10);
        if (parsedTime > 0) {
          setTimeLeft(parsedTime);
        }
      }

      if (savedQuestionIndex) {
        setCurrentQuestionIndex(parseInt(savedQuestionIndex, 10));
      }

      if (savedTabSwitchCount) {
        setTabSwitchCount(parseInt(savedTabSwitchCount, 10));
      }

      if (savedBackButtonCount) {
        setBackButtonCount(parseInt(savedBackButtonCount, 10));
      }
    } catch (err) {
      console.error("Error loading saved data:", err);
    }
  };

  // Save data to session
  const saveDataToSession = () => {
    try {
      if (studentId && liveExamId) {
        sessionStorage.setItem(getStorageKey('answers'), JSON.stringify(answers));
        sessionStorage.setItem(getStorageKey('timeLeft'), timeLeft.toString());
        sessionStorage.setItem(getStorageKey('currentQuestionIndex'), currentQuestionIndex.toString());
        sessionStorage.setItem(getStorageKey('tabSwitchCount'), tabSwitchCount.toString());
        sessionStorage.setItem(getStorageKey('backButtonCount'), backButtonCount.toString());
      }
    } catch (err) {
      console.error("Error saving to session storage:", err);
    }
  };

  // Clear exam session
  const clearExamSession = () => {
    try {
      if (studentId && liveExamId) {
        sessionStorage.removeItem(getStorageKey('answers'));
        sessionStorage.removeItem(getStorageKey('timeLeft'));
        sessionStorage.removeItem(getStorageKey('currentQuestionIndex'));
        sessionStorage.removeItem(getStorageKey('tabSwitchCount'));
        sessionStorage.removeItem(getStorageKey('backButtonCount'));
      }
    } catch (err) {
      console.error("Error clearing exam session:", err);
    }
  };

  // Save data periodically
  useEffect(() => {
    if (studentId && liveExamId && Object.keys(answers).length > 0) {
      saveDataToSession();
    }
  }, [answers, timeLeft, currentQuestionIndex, tabSwitchCount, backButtonCount]);

  // Start camera
  const startCamera = async () => {
    try {
      console.log("🎥 Starting camera for proctoring visibility...");
      
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480 },
        audio: false
      });

      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }

      setIsCameraActive(true);
      setCameraError(null);
      console.log("✅ Camera started (display only - not recording)");

    } catch (err) {
      console.error("❌ Camera access error:", err);
      setCameraError("Unable to access camera. Please allow camera permissions.");
      setError("Camera access is required for this exam.");
      setIsCameraActive(false);
    }
  };

  // Stop camera
  const stopCamera = () => {
    console.log("🛑 Stopping camera...");
    
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => {
        track.stop();
        console.log("Track stopped:", track.kind);
      });
    }

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    setIsCameraActive(false);
    console.log("✅ Camera stopped");
  };

  // Clear all browser storage
  const clearAllBrowserStorage = () => {
    try {
      localStorage.clear();
      sessionStorage.clear();

      const cookies = document.cookie.split(";");
      for (let i = 0; i < cookies.length; i++) {
        const cookie = cookies[i];
        const eqPos = cookie.indexOf("=");
        const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();

        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/";
        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=" + window.location.hostname;
        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=." + window.location.hostname;
      }

      return true;
    } catch (err) {
      console.error("❌ Error clearing storage:", err);
      return false;
    }
  };

  // Clear user session
  const clearUserSession = async () => {
    try {
      await axios.post(
        '/auth/logout',
        {},
        { withCredentials: true }
      );
      return true;
    } catch (err) {
      console.error("⚠️ Error clearing backend session:", err);
      return false;
    }
  };

  // Auto assign student
  const autoAssignStudent = async (studentId, examId) => {
    setIsAutoAssigning(true);
    setError("🔄 Assigning you to an exam set...");

    try {
      const response = await axios.post(
        '/examset/auto-assign',
        {
          userId: String(studentId),
          examId: Number(examId)
        },
        { withCredentials: true }
      );

      if (response.data.success) {
        setAssignedSetNumber(response.data.assignedSetNumber);
        setIsAutoAssigning(false);
        setError(null);
        return true;
      } else {
        throw new Error(response.data.error || "Auto-assignment failed");
      }

    } catch (err) {
      const errorMessage = err.response?.data?.error
        || err.response?.data?.message
        || err.message
        || "Failed to assign you to this exam.";

      setIsAutoAssigning(false);
      setError(errorMessage);
      return false;
    }
  };

  // ✅ NEW: Actual exam submission logic (called after confirmation)
  const performExamSubmission = async () => {
    if (!examData || isSubmitted || !studentId || isAutoSubmittingRef.current) {
      return;
    }

    isAutoSubmittingRef.current = true;
    setIsSubmitted(true);
    setIsSubmitting(true);

    try {
      console.log("📝 Submitting exam...");
      
      // Stop camera
      stopCamera();
      await new Promise(resolve => setTimeout(resolve, 500));

      // Prepare answers
      const payloadAnswers = {};
      Object.entries(answers).forEach(([questionId, answerContent]) => {
        const question = questions.find(q => q.id === Number(questionId));
        if (question) {
          payloadAnswers[questionId] = answerContent;
        }
      });

      const payload = {
        user: { id: Number(studentId) },
        exam: { id: Number(liveExamId) },
        answersJson: JSON.stringify(payloadAnswers),
        videoUrl: null
      };

      console.log("📤 Submitting payload (no video):", payload);

      await axios.post(
        "/candidate/submit-exam",
        payload,
        { withCredentials: true }
      );

      console.log("✅ Exam submitted successfully (without video)");

      // Clear exam session
      clearExamSession();

      const examFlowKey = `exam_flow_started_${studentId}`;
      const quizAccessKey = `quiz_access_${studentId}`;
      sessionStorage.removeItem(examFlowKey);
      sessionStorage.removeItem(quizAccessKey);

      setShowSuccessScreen(true);
      setShowSubmitModal(false);

      await clearUserSession();
      clearAllBrowserStorage();

      setStudentId(null);
      setAuthChecked(false);
      setExamData(null);
      setQuestions([]);
      setAnswers({});

    } catch (err) {
      console.error("❌ EXAM SUBMISSION FAILED:", err);
      alert("❌ Failed to submit exam. Please contact support.");
      setIsSubmitted(false);
      setIsSubmitting(false);
      setShowSubmitModal(false);
      isAutoSubmittingRef.current = false;
    }
  };

  // ✅ NEW: Handle submit button click - show modal first
  const handleSubmitExam = () => {
    if (isSubmitted || !examData || !studentId) {
      return;
    }
    setShowSubmitModal(true);
  };

  // ✅ NEW: Confirm submission from modal
  const confirmSubmission = () => {
    performExamSubmission();
  };

  // ✅ NEW: Cancel submission
  const cancelSubmission = () => {
    setShowSubmitModal(false);
  };

  // Tab switching detection
  useEffect(() => {
    if (isSubmitted || !examData) return;

    const handleVisibilityChange = () => {
      if (document.hidden) {
        setTabSwitchCount(prev => {
          const newCount = prev + 1;

          if (newCount > MAX_TAB_SWITCHES) {
            setTimeout(() => {
              if (!isAutoSubmittingRef.current) {
                performExamSubmission();
              }
            }, 500);
          } else {
            alert(`Warning: Tab switch detected! (${newCount}/${MAX_TAB_SWITCHES})`);
          }

          return newCount;
        });
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [isSubmitted, examData]);

  // Check authentication
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const res = await axios.get("/auth/current-user", {
          withCredentials: true
        });

        if (res.data && res.data.userId) {
          setStudentId(String(res.data.userId));
          if (res.data.currentExamId) {
            sessionStorage.setItem("currentExamId", String(res.data.currentExamId));
          }

          try {
            const userRes = await axios.get(
              `/admin/users/${res.data.userId}`,
              { withCredentials: true }
            );

            if (userRes.data) {
              setUserName(userRes.data.name || "Candidate");
              setUserEmail(userRes.data.email || "");
            }
          } catch (userErr) {
            console.error("⚠️ Could not fetch user details:", userErr);
            setUserName("Candidate");
          }

          setAuthChecked(true);
        } else {
          setError("Authentication failed. Please login again.");
          setTimeout(() => navigate("/"), 3000);
        }
      } catch (err) {
        setError("Please login to access the exam.");
        setTimeout(() => navigate("/"), 3000);
      }
    };

    checkAuth();
  }, [navigate]);

  // Fetch exam data
  useEffect(() => {
    if (!authChecked || !studentId) return;

    const fetchExamData = async () => {
      try {
        setLoading(true);
        setError(null);

        if (!liveExamId) {
          setError("No active exam found. Please return to Candidate Dashboard and start again.");
          setLoading(false);
          setTimeout(() => navigate('/candidate'), 3000);
          return;
        }

        let isAssigned = false;
        let currentSetNumber = null;

        try {
          const assignmentRes = await axios.get(
            `/examset/assignment`,
            {
              params: { userId: studentId, examId: liveExamId },
              withCredentials: true
            }
          );

          if (assignmentRes.data && assignmentRes.data.assignedSetNumber) {
            isAssigned = true;
            currentSetNumber = assignmentRes.data.assignedSetNumber;
            setAssignedSetNumber(currentSetNumber);
          }
        } catch (assignErr) {
          // Assignment not found
        }

        if (!isAssigned) {
          const assignSuccess = await autoAssignStudent(studentId, liveExamId);

          if (!assignSuccess) {
            setError("Failed to assign you to this exam.");
            setLoading(false);
            setTimeout(() => navigate('/candidate'), 5000);
            return;
          }

          const newAssignmentRes = await axios.get(
            `/examset/assignment`,
            {
              params: { userId: studentId, examId: liveExamId },
              withCredentials: true
            }
          );

          if (newAssignmentRes.data) {
            currentSetNumber = newAssignmentRes.data.assignedSetNumber;
            setAssignedSetNumber(currentSetNumber);
          }
        }

        const res = await axios.get(
          `/candidate/exams/${liveExamId}/shuffled-questions`,
          { withCredentials: true }
        );

        const questionsArray = res.data;

        if (!questionsArray || questionsArray.length === 0) {
          setError("No questions found.");
          setTimeout(() => navigate('/candidate'), 3000);
          return;
        }

        setExamData({
          title: ``,
          duration: 60,
          studentName: userName,
          studentId: studentId || "CD001",
          totalQuestions: questionsArray.length,
        });

        setQuestions(questionsArray);

        const savedTime = sessionStorage.getItem(getStorageKey('timeLeft'));
        if (!savedTime) {
          setTimeLeft(60 * 60);
        }

        setLoading(false);
        loadSavedData();
        
        startCamera();

      } catch (err) {
        const errorMessage = err.response?.data?.error || err.message || 'Failed to load exam.';
        setError(errorMessage);
        setLoading(false);
        setTimeout(() => navigate('/candidate'), 5000);
      }
    };

    fetchExamData();

    return () => {
      stopCamera();
    };
  }, [liveExamId, authChecked, studentId, navigate, userName]);

  // Timer countdown
  useEffect(() => {
    if (!examData || isSubmitted || timeLeft <= 0) return;

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          performExamSubmission();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [examData, isSubmitted, timeLeft]);

  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const currentQ = questions[currentQuestionIndex];
  const currentSection = sectionMap.find(s => s.id === currentQ?.section);
  const totalQuestions = questions.length;
  const answeredCount = Object.keys(answers).length;
  const unansweredCount = totalQuestions - answeredCount;

  const handleAnswerChange = (questionId, value) => {
    setAnswers((prev) => ({ ...prev, [questionId]: value }));
  };

  const handleNext = () => {
    if (!currentQ) return;
    const sectionQs = questions.filter((q) => q.section === currentQ.section);
    const idxInSection = sectionQs.findIndex((q) => q.id === currentQ.id);

    if (idxInSection < sectionQs.length - 1) {
      const nextId = sectionQs[idxInSection + 1].id;
      const globalIndex = questions.findIndex((q) => q.id === nextId);
      if (globalIndex !== -1) setCurrentQuestionIndex(globalIndex);
      return;
    }

    const currentSectionIndex = sectionMap.findIndex((s) => s.id === currentQ.section);
    for (let si = currentSectionIndex + 1; si < sectionMap.length; si++) {
      const secQs = questions.filter((q) => q.section === sectionMap[si].id);
      if (secQs.length > 0) {
        const globalIndex = questions.findIndex((q) => q.id === secQs[0].id);
        if (globalIndex !== -1) {
          setCurrentQuestionIndex(globalIndex);
          return;
        }
      }
    }

    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
    }
  };

  const handlePrevious = () => {
    if (!currentQ) return;
    const sectionQs = questions.filter((q) => q.section === currentQ.section);
    const idxInSection = sectionQs.findIndex((q) => q.id === currentQ.id);

    if (idxInSection > 0) {
      const prevId = sectionQs[idxInSection - 1].id;
      const globalIndex = questions.findIndex((q) => q.id === prevId);
      if (globalIndex !== -1) setCurrentQuestionIndex(globalIndex);
      return;
    }

    const currentSectionIndex = sectionMap.findIndex((s) => s.id === currentQ.section);
    for (let si = currentSectionIndex - 1; si >= 0; si--) {
      const secQs = questions.filter((q) => q.section === sectionMap[si].id);
      if (secQs.length > 0) {
        const lastQ = secQs[secQs.length - 1];
        const globalIndex = questions.findIndex((q) => q.id === lastQ.id);
        if (globalIndex !== -1) {
          setCurrentQuestionIndex(globalIndex);
          return;
        }
      }
    }

    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
    }
  };

  const handleQuestionJump = (questionIndex) => {
    setCurrentQuestionIndex(questionIndex);
  };

  const clearResponse = () => {
    const questionId = currentQ.id;
    setAnswers((prev) => {
      const newAnswers = { ...prev };
      delete newAnswers[questionId];
      return newAnswers;
    });
  };

  const getQuestionStatus = (questionIndex) => {
    const questionId = questions[questionIndex].id;
    const hasAnswer = answers[questionId] !== undefined;
    if (questionIndex === currentQuestionIndex) return "current";
    if (hasAnswer) return "answered";
    return "unanswered";
  };

  // Success screen
  if (showSuccessScreen) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-6">
        <div className="max-w-2xl w-full bg-white rounded-3xl shadow-2xl overflow-hidden">
          <div className="bg-white p-12 text-center border-b border-gray-200">
            <div className="flex justify-center mb-8">
              <img
                src={truerizeLogo}
                alt="Truerize Logo"
                className="h-32 w-auto object-contain"
              />
            </div>
            <div className="flex justify-center mb-6">
              <CheckCircle className="w-20 h-20 text-green-500" />
            </div>
            <h1 className="text-4xl font-bold mb-4 text-gray-900">
              Thank You for Submitting Your Exam!
            </h1>
            <p className="text-gray-600 text-lg">
              Your responses have been recorded successfully.
            </p>
            <p className="text-gray-500 text-sm mt-4">
              You may now close this window.
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (!authChecked || loading || !examData || isAutoAssigning) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-700 text-xl">
        {error ? (
          <div className="text-center">
            <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
            <p className="text-red-600">{typeof error === 'string' ? error : 'An error occurred'}</p>
          </div>
        ) : (
          <div className="text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p>{isAutoAssigning ? "🤖 Assigning you to an exam set..." : "⏳ Loading your exam..."}</p>
          </div>
        )}
      </div>
    );
  }

  const sectionQuestions = currentSection ? questions.filter((q) => q.section === currentSection.id) : [];
  const sectionQuestionIndex = currentQ ? sectionQuestions.findIndex((q) => q.id === currentQ.id) : -1;
  const displaySectionQuestionNumber = sectionQuestionIndex >= 0 ? sectionQuestionIndex + 1 : 0;
  const sectionQs = questions.filter((q) => q.section === currentQ.section);
  const idxInSection = sectionQs.findIndex((q) => q.id === currentQ.id);
  const isLastSection = sectionMap.findIndex((s) => s.id === currentQ.section) === sectionMap.length - 1;
  const isLastQuestionInSection = idxInSection === sectionQs.length - 1;
  const isLastQuestionOverall = isLastSection && isLastQuestionInSection;

  return (
    <div className="min-h-screen bg-gray-100 flex">
      {/* ✅ NEW: Submit Confirmation Modal */}
      {showSubmitModal && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full overflow-hidden animate-fade-in">
            <div className="bg-gradient-to-r from-orange-500 to-red-500 p-6">
              <div className="flex items-center justify-center">
                <AlertTriangle className="w-12 h-12 text-white" />
              </div>
              <h2 className="text-2xl font-bold text-white text-center mt-4">
                Confirm Exam Submission
              </h2>
            </div>
            
            <div className="p-6">
              <div className="mb-6">
                <p className="text-gray-700 text-center mb-4">
                  Are you sure you want to submit your exam?
                </p>
                
                <div className="mt-4 bg-red-50 border-l-4 border-red-500 p-3 rounded">
                  <p className="text-red-800 text-sm font-semibold">
                    ⚠️ Once submitted, you cannot make any changes to your answers.
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={cancelSubmission}
                  disabled={isSubmitting}
                  className="flex-1 px-6 py-3 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Cancel
                </button>
                <button
                  onClick={confirmSubmission}
                  disabled={isSubmitting}
                  className="flex-1 px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                      Submitting...
                    </>
                  ) : (
                    'Yes, Submit Exam'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showSidebar && (
        <div className="w-80 bg-slate-900 shadow-xl flex flex-col sticky top-0 h-screen text-white">
          <div className="p-6 border-b border-gray-700">
            <div className="flex items-center mb-4">
              <User className="w-8 h-8 text-blue-400 mr-3" />
              <div>
                <h3 className="font-semibold text-white">{examData.studentName}</h3>
                <p className="text-sm text-gray-300">{examData.studentId}</p>
              </div>
            </div>
            <div className="flex items-center text-sm text-gray-300 mb-2">
              <BookOpen className="w-4 h-4 mr-2" />
              <span>{examData.title}</span>
            </div>
          </div>

          {/* Camera Display */}
          <div className="p-4 border-b border-gray-700">
            <div className="relative bg-black rounded-lg overflow-hidden">
              <video 
                ref={videoRef} 
                autoPlay 
                muted 
                playsInline 
                className="w-full h-40 object-cover" 
              />
              <div className="absolute top-2 right-2">
                {isCameraActive ? (
                  <div className="flex items-center gap-2 bg-green-600 text-white px-2 py-1 rounded-full text-xs">
                    <Video className="w-3 h-3" />
                    Camera On
                  </div>
                ) : (
                  <div className="flex items-center gap-2 bg-red-600 text-white px-2 py-1 rounded-full text-xs">
                    <VideoOff className="w-3 h-3" />
                    Camera Off
                  </div>
                )}
              </div>
            </div>
            {cameraError && <p className="text-red-400 text-xs mt-2">{cameraError}</p>}
            <p className="text-gray-400 text-xs mt-2 text-center">
              📹 Proctoring: Display Only
            </p>
          </div>

          <div className="flex-1 p-6 overflow-y-auto sidebar-scroll">
            <h4 className="font-medium text-white mb-3 border-b border-gray-600 pb-2">
              Questions ({answeredCount} / {totalQuestions})
            </h4>
            {sectionMap.map(section => {
              const sectionQs = questions.filter(q => q.section === section.id);
              if (sectionQs.length === 0) return null;

              return (
                <div key={section.id} className="mb-6">
                  <p className="font-bold text-sm text-blue-300 mb-2">{section.name}</p>
                  <div className="grid grid-cols-5 gap-2">
                    {sectionQs.map((q, index) => {
                      const qIndex = questions.findIndex(qg => qg.id === q.id);
                      const status = getQuestionStatus(qIndex);
                      return (
                        <button
                          key={q.id}
                          onClick={() => handleQuestionJump(qIndex)}
                          className={`w-10 h-10 rounded-lg text-sm font-medium transition-colors ${
                            status === "current" 
                              ? "bg-blue-600 text-white shadow-lg border-2 border-blue-300"
                              : status === "answered" 
                                ? "bg-green-600 text-white"
                                : "bg-gray-600 text-gray-300 hover:bg-gray-500"
                          }`}
                        >
                          {index + 1}
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="p-6 border-t border-gray-700">
            <button
              onClick={handleSubmitExam}
              disabled={isSubmitted}
              className={`w-full font-medium py-3 px-4 rounded-lg transition-colors shadow-lg ${
                isSubmitted 
                  ? 'bg-gray-600 cursor-not-allowed' 
                  : 'bg-red-600 hover:bg-red-700 text-white'
              }`}
            >
              {isSubmitted ? 'Submitting...' : 'Submit Exam'}
            </button>
          </div>
        </div>
      )}

      <div className="flex-1 flex flex-col">
        <header className="bg-white shadow-md border-b border-gray-300 p-4 sticky top-0 z-40">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <button 
                onClick={() => setShowSidebar(!showSidebar)} 
                className="mr-4 p-2 hover:bg-gray-100 rounded-lg text-gray-700 transition-colors"
              >
                {showSidebar ? <ChevronLeft className="w-6 h-6" /> : <ChevronRight className="w-6 h-6" />}
              </button>
              <span className="text-lg font-medium text-gray-600">
                Section: <span className="font-semibold text-blue-600">{currentSection?.name}</span> |
                Question: <span className="font-semibold text-blue-600">{displaySectionQuestionNumber}</span>
              </span>
            </div>
            <div className="flex items-center space-x-6">
              <div className="flex items-center">
                <Clock className="w-5 h-5 text-red-500 mr-2" />
                <span className="font-medium text-gray-600 mr-2">Time Left:</span>
                <span className={`font-mono text-xl font-bold ${
                  timeLeft < 300 ? "text-red-500 animate-pulse" : "text-gray-800"
                }`}>
                  {formatTime(timeLeft)}
                </span>
              </div>
              <div className="text-sm text-gray-600 bg-gray-100 px-3 py-1 rounded-full">
                Question {displaySectionQuestionNumber} of {sectionQuestions.length}
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 p-8 bg-gray-50 overflow-y-auto">
          <div className="max-w-4xl mx-auto">
            <div className="bg-white rounded-xl shadow-lg border border-gray-200 p-8">
              <div className="mb-8">
                <div className="flex items-start justify-between mb-4">
                  <p className="text-lg font-semibold text-gray-800 flex-1">
                    Q{displaySectionQuestionNumber}. {currentQ.questionText}
                  </p>
                  <span className="inline-block bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-semibold ml-4">
                    {currentQ.marks} {currentQ.marks > 1 ? 'Marks' : 'Mark'}
                  </span>
                </div>
              </div>

              {currentQ?.type?.toLowerCase() === "mcq" && currentQ.options && currentQ.options.length > 0 && (
                <div className="space-y-3 mb-8">
                  {currentQ.options.map((option, index) => {
                    const optionValue = String.fromCharCode(65 + index);
                    const isSelected = answers[currentQ.id] === optionValue;
                    return (
                      <label
                        key={index}
                        className={`flex items-center p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                          isSelected
                            ? "border-blue-500 bg-blue-50"
                            : "border-gray-300 hover:border-gray-400 hover:bg-gray-50"
                        }`}
                      >
                        <input
                          type="radio"
                          name={`question-${currentQ.id}`}
                          value={optionValue}
                          checked={isSelected}
                          onChange={(e) => handleAnswerChange(currentQ.id, e.target.value)}
                          className="w-5 h-5 text-blue-600 mr-3"
                        />
                        <span className="font-semibold mr-3 text-gray-700">{optionValue}.</span>
                        <span className="text-gray-800 select-none">{option}</span>
                      </label>
                    );
                  })}
                </div>
              )}

              <div className="flex items-center justify-between pt-6 border-t border-gray-300 mt-8">
                <div className="flex space-x-3">
                  <button
                    onClick={handlePrevious}
                    disabled={currentQuestionIndex === 0}
                    className={`px-6 py-2 border border-gray-400 text-gray-700 rounded-lg transition-colors ${
                      currentQuestionIndex === 0
                        ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                        : "bg-gray-200 hover:bg-gray-300 text-gray-800"
                    }`}
                  >
                    Previous
                  </button>

                  <button
                    onClick={clearResponse}
                    className="px-6 py-2 bg-gray-100 border border-gray-400 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                  >
                    Clear Response
                  </button>
                </div>

                {!isLastQuestionOverall && (
                  <button
                    onClick={handleNext}
                    className="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
                  >
                    Next
                  </button>
                )}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default QuizInterface; 
