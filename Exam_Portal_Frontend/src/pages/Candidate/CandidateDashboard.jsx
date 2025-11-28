import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import truerizeLogo from "../../assets/images/Truerize_Logo.png";

function CandidateNavbar() {
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-8 py-4 bg-white shadow-lg">
      <div className="flex items-center space-x-4">
        <img
          src={truerizeLogo}
          alt="Truerize Logo"
          className="h-16 w-16 object-contain"
        />
        <div className="text-3xl font-bold text-blue-900 tracking-wide">
          Truerize Exam Portal
        </div>
      </div>
      <div className="flex items-center space-x-4">
        <span className="text-lg font-semibold text-gray-700">
          Candidate Dashboard
        </span>
      </div>
    </nav>
  );
}

function CandidateExamPortal() {
  const [systemReady, setSystemReady] = useState(false);
  const [exam, setExam] = useState(null);
  const [loading, setLoading] = useState(true);
  const [userId, setUserId] = useState(null);
  const [examStats, setExamStats] = useState({
    totalQuestions: 0,
    duration: 0
  });
  const navigate = useNavigate();

  
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
    const clearAllExamSessions = () => {
      try {
        
        const keys = Object.keys(sessionStorage);
        
        
        keys.forEach(key => {
          if (key.includes('exam_flow_started_') || 
              key.includes('quiz_access_') ||
              key.includes('exam_')) {
            sessionStorage.removeItem(key);
            console.log("🧹 Cleared session key:", key);
          }
        });
        
        console.log("✅ All exam sessions cleared on dashboard mount");
      } catch (err) {
        console.error("❌ Error clearing sessions:", err);
      }
    };

    clearAllExamSessions();
  }, []);

  
  useEffect(() => {
    const getUser = async () => {
      try {
        const res = await axios.get("http://localhost:8080/api/auth/current-user", {
          withCredentials: true
        });
        if (res.data && res.data.userId) {
          setUserId(res.data.userId);
          console.log("✅ User ID loaded:", res.data.userId);
        }
      } catch (err) {
        console.error("❌ Failed to get user:", err);
      }
    };
    getUser();
  }, []);

 
  useEffect(() => {
    const fetchExam = async () => {
      try {
        const examRes = await axios.get("http://localhost:8080/api/admin/exams/12");
        const examData = examRes.data;
        
        const questionsRes = await axios.get("http://localhost:8080/api/admin/exams/12/questions");
        const questions = questionsRes.data;
        
        setExam(examData);
        setExamStats({
          totalQuestions: questions.length,
          duration: examData.duration || 60
        });
        
        setLoading(false);
        console.log("✅ Exam data loaded:", {
          title: examData.title,
          questions: questions.length,
          duration: examData.duration
        });
      } catch (err) {
        console.error("❌ Failed to load exam:", err);
        setLoading(false);
      }
    };
    fetchExam();
  }, []);

  
  useEffect(() => {
    const timer = setTimeout(() => {
      setSystemReady(true);
      console.log("✅ System ready");
    }, 3000);
    return () => clearTimeout(timer);
  }, []);

  const handleStart = () => {
    if (!userId) {
      alert("Unable to start exam. Please refresh and try again.");
      console.error("❌ No user ID available");
      return;
    }

    if (!exam || !exam.id) {
      alert("Unable to start exam. Exam data not loaded properly.");
      console.error("❌ No exam data available");
      return;
    }

    console.log("🚀 Starting exam for user:", userId, "Exam ID:", exam.id);

    try {
      
      const allKeys = Object.keys(sessionStorage);
      allKeys.forEach(key => {
        if (key.includes(`_${userId}`) || key.includes('exam_') || key.includes('quiz_')) {
          sessionStorage.removeItem(key);
          console.log("🧹 Removed:", key);
        }
      });

      
      const sessionKey = `exam_flow_started_${userId}`;
      sessionStorage.setItem(sessionKey, 'true');
      console.log("✅ Created exam flow session:", sessionKey);
      
     
      const verifySession = sessionStorage.getItem(sessionKey);
      if (!verifySession) {
        console.error("❌ Failed to create session!");
        alert("Session creation failed. Please try again.");
        return;
      }
      console.log("🔍 Session verified:", verifySession);

      
      setTimeout(() => {
        console.log("🚀 Navigating to camera page...");
        
        
        navigate("/camera", { 
          state: { 
            userId: userId, 
            examId: exam.id 
          },
          replace: true 
        });
      }, 200);

    } catch (error) {
      console.error("❌ Error in handleStart:", error);
      alert("Failed to start exam. Please try again.");
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <CandidateNavbar />
        <div className="pt-24 flex items-center justify-center min-h-[80vh]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
            <p className="text-lg text-gray-600 font-medium">
              Loading exam details...
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (!exam) {
    return (
      <div className="min-h-screen bg-gray-50">
        <CandidateNavbar />
        <div className="pt-24 text-center mt-20 text-red-600 text-lg">
          Failed to load exam. Please contact support.
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <CandidateNavbar />

      <div className="pt-28 container mx-auto px-6 py-8 max-w-7xl">
        
        <div className="text-center mb-14 mt-6">
          <h1 className="text-4xl font-bold text-gray-800 mb-3">
            Welcome to Truerize Assessment Portal
          </h1>
          <p className="text-lg text-gray-600">
            Please review the exam details and instructions carefully before proceeding
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          <div className="lg:col-span-2 space-y-6">
            
            <div className="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden">
              <div className="bg-gradient-to-r from-blue-600 to-blue-700 p-6 text-white">
                <h2 className="text-2xl font-bold text-white">{exam.title}</h2>
                <p className="text-blue-100 mt-1">Online Assessment</p>
              </div>
              
              <div className="p-6">
                <div className="grid grid-cols-2 gap-4 mb-6">
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
                    <div className="text-3xl font-bold text-blue-600 mb-1">
                      {examStats.duration}
                    </div>
                    <div className="text-sm text-gray-600 font-medium">Minutes</div>
                  </div>
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
                    <div className="text-3xl font-bold text-blue-600 mb-1">
                      {examStats.totalQuestions}
                    </div>
                    <div className="text-sm text-gray-600 font-medium">Questions</div>
                  </div>
                </div>

                <div className="space-y-3 text-sm text-gray-700">
                  <div className="flex items-start">
                    <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    <span>Multiple choice questions covering various topics</span>
                  </div>
                  <div className="flex items-start">
                    <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    <span>Automatic submission when time expires</span>
                  </div>
                  <div className="flex items-start">
                    <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    <span>Results will be shared via email within 48 hours</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-xl shadow-md border border-gray-200 p-6">
              <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center">
                <svg className="w-6 h-6 text-blue-600 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Exam Instructions
              </h3>

              <div className="space-y-4 text-sm text-gray-600">
                <div>
                  <h4 className="font-semibold text-gray-700 mb-2">Before Starting:</h4>
                  <ul className="space-y-2 ml-5 list-disc">
                    <li>Ensure a stable internet connection</li>
                    <li>Close unnecessary applications and tabs</li>
                    <li>Find a quiet room with minimal distractions</li>
                    <li>Keep your ID ready for verification</li>
                  </ul>
                </div>

                <div>
                  <h4 className="font-semibold text-gray-700 mb-2">During the Exam:</h4>
                  <ul className="space-y-2 ml-5 list-disc">
                    <li>Stay focused and avoid switching tabs</li>
                    <li>No communication with others allowed</li>
                    <li>Unauthorized materials are prohibited</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            
            <div className="bg-white rounded-xl shadow-md border border-gray-200 p-6">
              <button
                onClick={handleStart}
                disabled={!systemReady || !userId || !exam}
                className={`w-full py-4 px-6 rounded-lg font-semibold text-lg transition-all ${
                  (systemReady && userId && exam)
                    ? "bg-blue-600 hover:bg-blue-700 text-white shadow-lg hover:shadow-xl"
                    : "bg-gray-300 text-gray-500 cursor-not-allowed"
                }`}
              >
                {!userId ? "Loading..." : systemReady ? "Start Examination" : "Preparing System..."}
              </button>
              {systemReady && userId && (
                <p className="text-xs text-gray-500 mt-3 text-center">
                  By clicking "Start Examination", you agree to the terms and conditions
                </p>
              )}
            </div>

            <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-gray-700">
              <div className="flex items-start">
                <svg className="w-5 h-5 text-yellow-600 mr-2 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                <p>
                  <strong>Important Notice:</strong> Once started, the exam cannot be paused. Please ensure you have sufficient time.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CandidateExamPortal;