import React, { useState, useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import axios from '../api/axiosConfig';

const ExamFlowProtection = ({ children, requiresExamSession = false }) => {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);
  const [hasExamSession, setHasExamSession] = useState(false);
  const location = useLocation();

  useEffect(() => {
    checkExamFlow();
  }, []);

  const checkExamFlow = async () => {
    try {
      
      const authRes = await axios.get("/auth/current-user", {
        withCredentials: true
      });

      if (!authRes.data || !authRes.data.userId) {
        setUser(null);
        setLoading(false);
        return;
      }

      setUser(authRes.data);

      
      if (requiresExamSession) {
        const sessionKey = `exam_flow_started_${authRes.data.userId}`;
        const examStarted = sessionStorage.getItem(sessionKey);
        setHasExamSession(!!examStarted);
      } else {
        setHasExamSession(true); 
      }

    } catch (err) {
      console.error("Exam flow check failed:", err);
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
          <p className="text-lg text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }


  if (!user) {
    return <Navigate to="/" replace />;
  }

  if (user.role !== 'CANDIDATE') {
    return <Navigate to="/dashboard" replace />;
  }

  
  if (requiresExamSession && !hasExamSession) {
    return <Navigate to="/candidate" replace />;
  }

  return children;
};

export default ExamFlowProtection;
