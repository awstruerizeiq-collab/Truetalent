import React, { Suspense, lazy } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import ErrorBoundary from "./components/ErrorBoundary";
import PageLayout from "./components/PageLayout";
import ProtectedRoute from "./components/ProtectedRoute";

const LoginPage = lazy(() => import("./pages/Login"));
const Terms = lazy(() => import("./pages/Terms"));
const Privacy = lazy(() => import("./pages/Privacy"));
const Help = lazy(() => import("./pages/Help"));
const Dashboard = lazy(() => import("./pages/Admin/AdminDashboard"));
const Users = lazy(() => import("./pages/Admin/ManageUsers"));
const Exams = lazy(() => import("./pages/Admin/ManageExams"));
const Questions = lazy(() => import("./pages/Admin/QuestionBank"));
const Results = lazy(() => import("./pages/Admin/ManageResults"));
const CandidateDashboard = lazy(() => import("./pages/Candidate/CandidateDashboard"));
const CameraMic = lazy(() => import("./pages/Candidate/Camera-mic"));
const QuizInterface = lazy(() => import("./pages/Candidate/QuizInterface"));
const Feedback = lazy(() => import("./pages/Candidate/Feedback"));
const Run = lazy(() => import("./pages/Candidate/Run"));



const LoadingSpinner = () => (
  <div className="min-h-screen bg-gray-50 flex items-center justify-center">
    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
  </div>
);

function App() {
  return (
    <ErrorBoundary>
      <Router>
        <Suspense fallback={<LoadingSpinner />}>
          <Routes>
            <Route path="/" element={<LoginPage />} />
            <Route path="/terms" element={<Terms />} />
            <Route path="/privacy" element={<Privacy />} />
            <Route path="/help" element={<Help />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <PageLayout>
                    <Dashboard />
                  </PageLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <PageLayout>
                    <Users />
                  </PageLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/exams"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <PageLayout>
                    <Exams />
                  </PageLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/questions"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <PageLayout>
                    <Questions />
                  </PageLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/results"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <PageLayout>
                    <Results />
                  </PageLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/candidate"
              element={
                <ProtectedRoute allowedRoles={['CANDIDATE']}>
                  <CandidateDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/camera"
              element={
                <ProtectedRoute
                  allowedRoles={['CANDIDATE']}
                  requiresExamFlow={true}
                >
                  <CameraMic />
                </ProtectedRoute>
              }
            />
            <Route
              path="/quiz"
              element={
                <ProtectedRoute
                  allowedRoles={['CANDIDATE']}
                  requiresQuizAccess={true}
                >
                  <QuizInterface />
                </ProtectedRoute>
              }
            />
            <Route
              path="/submit"
              element={
                <ProtectedRoute allowedRoles={['CANDIDATE']}>
                  <Feedback />
                </ProtectedRoute>
              }
            />
            <Route
              path="/run"
              element={
                <ProtectedRoute allowedRoles={['CANDIDATE']}>
                  <Run />
                </ProtectedRoute>
              }
            />
          
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </Router>
    </ErrorBoundary>
  );
}

export default App;
