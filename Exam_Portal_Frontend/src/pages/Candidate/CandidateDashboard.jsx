import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import axios from "../../api/axiosConfig";
import truerizeLogo from "../../assets/images/Truerize_Logo.png";

function CandidateNavbar() {
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between border-b border-white/60 bg-white/80 px-8 py-4 shadow-[0_12px_30px_-20px_rgba(15,23,42,0.35)] backdrop-blur-sm">
      <div className="flex items-center space-x-4">
        <img src={truerizeLogo} alt="Truerize Logo" className="h-12 w-12 object-contain rounded-xl shadow-sm" />
        <div className="text-2xl font-semibold text-slate-900 tracking-wide">Truerize Talent Portal</div>
      </div>
      <div className="inline-flex items-center rounded-full border border-emerald-200/70 bg-emerald-50/70 px-4 py-1 text-sm font-semibold text-emerald-700">
        Candidate Dashboard
      </div>
    </nav>
  );
}

function CandidateExamPortal() {
  const [systemReady, setSystemReady] = useState(false);
  const [exam, setExam] = useState(null);
  const [loading, setLoading] = useState(true);
  const [userId, setUserId] = useState(null);
  const [activeExamId, setActiveExamId] = useState(null);
  const [loadError, setLoadError] = useState("");
  const [examStats, setExamStats] = useState({ totalQuestions: 0, duration: 0 });

  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    document.body.classList.remove("admin-dashboard-body", "login-page-body");
    document.body.classList.add("candidate-body");
    document.body.style.overflow = "auto";
    return () => {
      document.body.classList.remove("candidate-body");
      document.body.style.overflow = "";
    };
  }, []);

  useEffect(() => {
    const keys = Object.keys(sessionStorage);
    keys.forEach((key) => {
      if (key.includes("exam_flow_started_") || key.includes("quiz_access_") || key.includes("exam_")) {
        sessionStorage.removeItem(key);
      }
    });
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => setSystemReady(true), 3000);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const parseExamId = (value) => {
      if (!value && value !== 0) return null;
      const parsed = Number(value);
      return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
    };

    const resolveExamId = async (currentUser) => {
      const fromQuery = parseExamId(searchParams.get("examId"));
      const fromCurrentUser = parseExamId(currentUser?.currentExamId);
      const fromStorage = parseExamId(sessionStorage.getItem("currentExamId"));

      if (fromQuery) return fromQuery;
      if (fromCurrentUser) return fromCurrentUser;
      if (fromStorage) return fromStorage;

      try {
        const contextRes = await axios.get("/candidate/exam-context", { withCredentials: true });
        return parseExamId(contextRes.data?.examId);
      } catch (error) {
        return null;
      }
    };

    const loadCandidateExam = async () => {
      try {
        setLoading(true);
        setLoadError("");

        const userRes = await axios.get("/auth/current-user", { withCredentials: true });
        if (!userRes.data?.userId) {
          setLoadError("Session has expired. Please login again.");
          setLoading(false);
          return;
        }

        setUserId(userRes.data.userId);

        const resolvedExamId = await resolveExamId(userRes.data);
        if (!resolvedExamId) {
          setLoadError("No exam is assigned for your session. Please use the exam link shared by admin.");
          setLoading(false);
          return;
        }

        setActiveExamId(resolvedExamId);
        sessionStorage.setItem("currentExamId", String(resolvedExamId));

        const examRes = await axios.get(`/admin/exams/${resolvedExamId}`, { withCredentials: true });
        const examData = examRes.data;

        setExam(examData);
        setExamStats({
          totalQuestions: Array.isArray(examData?.questions) ? examData.questions.length : 0,
          duration: examData?.duration || 60,
        });
      } catch (error) {
        setLoadError(
          error.response?.data?.error ||
            error.response?.data?.message ||
            "Failed to load exam. Please contact support."
        );
      } finally {
        setLoading(false);
      }
    };

    loadCandidateExam();
  }, [searchParams]);

  const handleStart = () => {
    if (!userId || !activeExamId) {
      alert("Unable to start exam. Please refresh and try again.");
      return;
    }

    const keys = Object.keys(sessionStorage);
    keys.forEach((key) => {
      if (key.includes(`_${userId}`) || key.includes("exam_") || key.includes("quiz_")) {
        sessionStorage.removeItem(key);
      }
    });

    const sessionKey = `exam_flow_started_${userId}`;
    sessionStorage.setItem(sessionKey, "true");
    sessionStorage.setItem("currentExamId", String(activeExamId));

    navigate("/camera", {
      state: { userId, examId: activeExamId },
      replace: true,
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen">
        <CandidateNavbar />
        <div className="pt-24 flex items-center justify-center min-h-[80vh]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
            <p className="text-lg text-gray-600 font-medium">Loading exam details...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!exam) {
    return (
      <div className="min-h-screen">
        <CandidateNavbar />
        <div className="pt-24 text-center mt-20 text-red-600 text-lg">
          {loadError || "Failed to load exam. Please contact support."}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <CandidateNavbar />

      <div className="pt-28 container mx-auto px-6 py-8 max-w-7xl">
        <div className="text-center mb-14 mt-6">
          <h1 className="text-4xl font-semibold text-slate-900 mb-3">Welcome to Talent Truerize Portal</h1>
          <p className="text-lg text-slate-600">
            Please review the exam details and instructions carefully before proceeding
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white/80 rounded-2xl shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] border border-white/70 overflow-hidden">
              <div className="bg-gradient-to-r from-blue-600 to-cyan-500 p-6 text-white">
                <h2 className="text-2xl font-bold text-white">{exam.title || "Assigned Exam"}</h2>
                <p className="text-blue-100 mt-1">Online Assessment</p>
              </div>

              <div className="p-6">
                <div className="grid grid-cols-2 gap-4 mb-6">
                  <div className="bg-white/90 border border-blue-100 rounded-xl p-4 text-center shadow-sm">
                    <div className="text-3xl font-bold text-blue-600 mb-1">{examStats.duration}</div>
                    <div className="text-sm text-slate-600 font-medium">Minutes</div>
                  </div>
                  <div className="bg-white/90 border border-blue-100 rounded-xl p-4 text-center shadow-sm">
                    <div className="text-3xl font-bold text-blue-600 mb-1">{examStats.totalQuestions}</div>
                    <div className="text-sm text-slate-600 font-medium">Questions</div>
                  </div>
                </div>

                <div className="text-sm text-gray-500">Exam ID: {activeExamId}</div>
              </div>
            </div>

            <div className="bg-white/80 rounded-2xl shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] border border-white/70 p-6">
              <h3 className="text-xl font-semibold text-slate-900 mb-4">Exam Instructions</h3>
              <ul className="space-y-2 ml-5 list-disc text-sm text-slate-600">
                <li>Ensure a stable internet connection</li>
                <li>Use only one device and do not refresh browser tabs</li>
                <li>Allow camera/microphone and follow proctoring instructions</li>
                <li>Once started, exam cannot be paused</li>
              </ul>
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-white/80 rounded-2xl shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] border border-white/70 p-6">
              <button
                onClick={handleStart}
                disabled={!systemReady || !userId || !activeExamId}
                className={`w-full py-4 px-6 rounded-xl font-semibold text-lg transition-all ${
                  systemReady && userId && activeExamId
                    ? "bg-gradient-to-r from-blue-600 to-cyan-500 text-white shadow-lg hover:shadow-xl"
                    : "bg-slate-200 text-slate-500 cursor-not-allowed"
                }`}
              >
                {!userId ? "Loading..." : systemReady ? "Start Examination" : "Preparing System..."}
              </button>
            </div>

            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-slate-700">
              <p>
                <strong>Important Notice:</strong> Once started, the exam cannot be paused. Please ensure you have
                sufficient time.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CandidateExamPortal;
