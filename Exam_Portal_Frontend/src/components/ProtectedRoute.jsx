import React, { useState, useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import axios from '../api/axiosConfig';
import NotificationModal from './NotificationModal';
import logger from '../utils/logger';

const ProtectedRoute = ({
  children,
  allowedRoles = [],
  requiresExamFlow = false,
  requiresQuizAccess = false
}) => {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState(null);
  const [hasAccess, setHasAccess] = useState(false);
  const [initialCheckDone, setInitialCheckDone] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [modalMessage, setModalMessage] = useState('');
  const location = useLocation();

  
  useEffect(() => {
    if (location.pathname === '/camera' || location.pathname === '/quiz') {
      
      window.history.pushState(null, '', window.location.href);
      
      const handlePopState = (event) => {
        
        window.history.pushState(null, '', window.location.href);
        
        
        if (location.pathname === '/camera') {
          setModalMessage('⚠️ You cannot navigate back during camera verification!\n\nPlease complete the verification process.');
          setShowModal(true);
        } else if (location.pathname === '/quiz') {
          setModalMessage('⚠️ You cannot navigate back during the exam!\n\nPlease use the "Submit Exam" button to exit.');
          setShowModal(true);
        }
      };

      window.addEventListener('popstate', handlePopState);
      
      return () => {
        window.removeEventListener('popstate', handlePopState);
      };
    }
  }, [location.pathname]);

  
  useEffect(() => {
    if (location.pathname === '/camera' || location.pathname === '/quiz') {
      const handleBeforeUnload = (e) => {
        e.preventDefault();
        e.returnValue = 'Are you sure you want to leave? Your exam progress may be lost.';
        return e.returnValue;
      };

      window.addEventListener('beforeunload', handleBeforeUnload);
      
      return () => {
        window.removeEventListener('beforeunload', handleBeforeUnload);
      };
    }
  }, [location.pathname]);

  
  useEffect(() => {
    checkAuth();
  }, [location.pathname]);

  const checkAuth = async () => {
    try {
      const res = await axios.get("/auth/current-user", {
        withCredentials: true
      });

      if (res.data && res.data.userId) {
        setUser(res.data);

        
        if (requiresExamFlow) {
          const sessionKey = `exam_flow_started_${res.data.userId}`;
          const hasValidFlow = sessionStorage.getItem(sessionKey);

          if (!hasValidFlow) {
            setHasAccess(false);
            setInitialCheckDone(true);
            
            setTimeout(() => {
              setModalMessage('⚠️ Access Denied!\n\nYou must start the exam from the Candidate Dashboard.\n\nPlease click "Start Examination" button to begin.');
              setShowModal(true);
            }, 100);
          } else {
            setHasAccess(true);
            setInitialCheckDone(true);
          }
        }
        
        else if (requiresQuizAccess) {
          const quizSessionKey = `quiz_access_${res.data.userId}`;
          const hasValidAccess = sessionStorage.getItem(quizSessionKey);

          if (!hasValidAccess) {
            setHasAccess(false);
            setInitialCheckDone(true);

            setTimeout(() => {
              setModalMessage('⚠️ Access Denied!\n\nYou must complete camera verification first.\n\nPlease go to Candidate Dashboard and follow the proper workflow.');
              setShowModal(true);
            }, 100);
          } else {
            setHasAccess(true);
            setInitialCheckDone(true);
          }
        }
        else {
          setHasAccess(true);
          setInitialCheckDone(true);
        }
      } else {
        setUser(null);
        setHasAccess(false);
        setInitialCheckDone(true);
      }
    } catch (err) {
      logger.error("Authentication check failed", err);
      setUser(null);
      setHasAccess(false);
      setInitialCheckDone(true);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !initialCheckDone) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
          <p className="text-lg text-gray-600">Verifying authentication...</p>
        </div>
      </div>
    );
  }

  
  if (!user) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  
  if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    if (user.role === 'ADMIN') {
      return <Navigate to="/dashboard" replace />;
    } else if (user.role === 'CANDIDATE') {
      return <Navigate to="/candidate" replace />;
    }
    return <Navigate to="/" replace />;
  }

  
  if ((requiresExamFlow || requiresQuizAccess) && !hasAccess) {
    return <Navigate to="/candidate" replace />;
  }

  return children;
};

export default ProtectedRoute;
