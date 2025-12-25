import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from "react-router-dom";
import axios from "../api/axiosConfig";
import "../index.css";
import logo from "../assets/images/Truerize_Logo.png";
import helpVideo from "../assets/images/help-video.mp4";

function LoginPageInternal() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [errorTitle, setErrorTitle] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showWelcomeMessage, setShowWelcomeMessage] = useState(false);
    const [examId, setExamId] = useState(null);

    useEffect(() => {
        document.body.classList.remove('admin-dashboard-body', 'candidate-body');
        document.body.classList.add('login-page-body');
        
        const examIdParam = searchParams.get('examId');
        if (examIdParam) {
            setExamId(parseInt(examIdParam));
        }
        
        return () => {
            document.body.classList.remove('login-page-body');
        };
    }, [searchParams]);

    useEffect(() => {
        localStorage.clear();
        sessionStorage.clear();
        
        window.history.pushState(null, document.title, window.location.href);
        
        const handlePopState = () => {
            window.history.pushState(null, document.title, window.location.href);
        };

        window.addEventListener('popstate', handlePopState);

        return () => {
            window.removeEventListener('popstate', handlePopState);
        };
    }, []);

    useEffect(() => {
        const checkSession = async () => {
            const justLoggedOut = sessionStorage.getItem('justLoggedOut');
            if (justLoggedOut) {
                sessionStorage.removeItem('justLoggedOut');
                return;
            }

            try {
                const res = await axios.get("/auth/current-user", {
                    withCredentials: true
                });
                const user = res.data;
                
                if (user && user.userId) {
                    if (user.role === "ADMIN") {
                        navigate("/dashboard", { replace: true });
                    } else if (user.role === "CANDIDATE") {
                        navigate("/candidate", { replace: true });
                    }
                }
            } catch (err) {
                console.log("No active session found");
            }
        };
        
        checkSession();
    }, [navigate]);

    const handleLogoClick = () => {
        setShowWelcomeMessage(true);
        setTimeout(() => setShowWelcomeMessage(false), 3000);
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setErrorTitle('');
        setIsLoading(true);

        try {
            if (!email.trim() || !password.trim()) {
                setErrorTitle("Validation Error");
                setError("Please enter both email and password");
                setIsLoading(false);
                return;
            }

            const loginData = { 
                email: email.trim(), 
                password 
            };
            
            if (examId) {
                loginData.examId = examId;
            }

            const response = await axios.post("/auth/login", loginData, {
                withCredentials: true
            });

            if (!response.data.success) {
                handleErrorResponse(response.data);
                setIsLoading(false);
                return;
            }

            sessionStorage.removeItem('justLoggedOut');

            const userRes = await axios.get("/auth/current-user", {
                withCredentials: true
            });
            const user = userRes.data;

            if (user.role === "ADMIN") {
                navigate("/dashboard", { replace: true });
            } else if (user.role === "CANDIDATE") {
                if (user.examStarted && user.currentExamId) {
                    sessionStorage.setItem('examWarningShown', 'false');
                    sessionStorage.setItem('currentExamId', user.currentExamId);
                }
                
                navigate("/candidate", { replace: true });
            } else {
                navigate("/candidate", { replace: true });
            }

        } catch (err) {
            console.error("Login error:", err);
            
            if (err.response && err.response.data) {
                handleErrorResponse(err.response.data);
            } else if (err.request) {
                setErrorTitle("Connection Error");
                setError("Unable to connect to the server. Please check your internet connection and try again.");
            } else {
                setErrorTitle("Error");
                setError("An unexpected error occurred. Please try again.");
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleErrorResponse = (data) => {
        if (data.examCompleted) {
            setErrorTitle(data.error || "Exam Already Completed");
            setError(data.message || "You have already completed this exam. Multiple attempts are not allowed.");
        } else if (data.activeSession) {
            setErrorTitle(data.error || "Exam Already in Progress");
            setError(data.message || "You have an active exam session. Please complete it on the original device.");
        } else if (data.examLocked) {
            setErrorTitle(data.error || "Exam Locked");
            setError(data.message || "This exam has been locked. Please contact support.");
        } else {
            setErrorTitle(data.error || "Login Failed");
            setError(data.message || "Invalid email or password");
        }
    };

    return (
        <div className="login-page">
            {showWelcomeMessage && (
                <div className="toast-message">
                    Welcome to Truerize Online Exam Portal !!!
                </div>
            )}

            <div className="login-wrapper">
                <div className="login-left">
                    <div className="logo-wrapper" onClick={handleLogoClick}>
                        <img src={logo} alt="Logo" className="logo" />
                    </div>

                    <h1 className="welcome-heading">Welcome to Truerize Talent</h1>
                    <h2>Proceed to your exam</h2>

                    {examId && (
                        <div style={{
                            backgroundColor: '#fff3cd',
                            border: '1px solid #ffc107',
                            borderRadius: '8px',
                            padding: '15px',
                            marginBottom: '20px',
                            fontSize: '13px',
                            color: '#856404'
                        }}>
                            <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>
                                ⚠️ Important Exam Instructions:
                            </div>
                            <ul style={{ margin: '0', paddingLeft: '20px', lineHeight: '1.8' }}>
                                <li>You can only attempt this exam <strong>ONCE</strong></li>
                                <li>Once you login, the exam timer will start automatically</li>
                                <li>Ensure stable internet connection before starting</li>
                                <li>Use only ONE device - multiple device login is not allowed</li>
                                <li>Do not refresh the page or close the browser</li>
                                <li>The exam will auto-submit when time expires</li>
                            </ul>
                        </div>
                    )}

                    <form className="login-form" onSubmit={handleLogin}>
                        <input
                            type="email"
                            placeholder="Enter your email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            disabled={isLoading}
                            autoComplete="email"
                        />

                        <div className="password-field">
                            <input
                                type={showPassword ? "text" : "password"}
                                placeholder="Password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                                autoComplete="current-password"
                            />
                            <button
                                type="button"
                                className="toggle-password"
                                onClick={() => setShowPassword(!showPassword)}
                                disabled={isLoading}
                            >
                                {showPassword ? "👁" : "👁"}
                            </button>
                        </div>

                        {error && (
                            <div style={{ 
                                backgroundColor: '#fee2e2',
                                border: '1px solid #dc2626',
                                borderRadius: '8px',
                                padding: '14px',
                                marginTop: '12px'
                            }}>
                                {errorTitle && (
                                    <p style={{ 
                                        color: '#dc2626', 
                                        margin: '0 0 8px 0',
                                        fontSize: '15px',
                                        fontWeight: '600'
                                    }}>
                                        {errorTitle}
                                    </p>
                                )}
                                <p style={{ 
                                    color: '#991b1b', 
                                    margin: '0',
                                    fontSize: '13px',
                                    lineHeight: '1.6'
                                }}>
                                    {error}
                                </p>
                            </div>
                        )}
                        
                        {isLoading && (
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                marginTop: '12px',
                                color: '#0066cc'
                            }}>
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    border: '3px solid #e5e7eb',
                                    borderTop: '3px solid #0066cc',
                                    borderRadius: '50%',
                                    animation: 'spin 1s linear infinite',
                                    marginRight: '10px'
                                }}></div>
                                <p style={{ margin: '0', fontSize: '14px' }}>
                                    {examId ? 'Starting exam...' : 'Logging in...'}
                                </p>
                            </div>
                        )}

                        <button 
                            type="submit" 
                            className="login-btn" 
                            disabled={isLoading}
                            style={{ 
                                opacity: isLoading ? 0.6 : 1,
                                cursor: isLoading ? 'not-allowed' : 'pointer'
                            }}
                        >
                            {isLoading ? (examId ? "Starting Exam..." : "Logging in...") : "Login"}
                        </button>
                    </form>

                    {examId && (
                        <div style={{
                            marginTop: '15px',
                            padding: '10px',
                            backgroundColor: '#e0f2fe',
                            borderRadius: '6px',
                            fontSize: '12px',
                            color: '#0369a1',
                            textAlign: 'center'
                        }}>
                            📝 Exam ID: {examId}
                        </div>
                    )}

                    <footer>
                        <p>© Copyright Truerize 2025</p>
                        <div className="footer-links">
                            <Link to="/terms">Terms & Condition</Link>
                            <Link to="/privacy">Privacy Policy</Link>
                            <Link to="/help">Help</Link>
                        </div>
                    </footer>
                </div>

                <div className="login-right">
                    <video 
                        src={helpVideo} 
                        autoPlay 
                        muted 
                        loop 
                        playsInline 
                        className="login-video" 
                    />
                </div>
            </div>

            <style>{`
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            `}</style>
        </div>
    );
}

export default LoginPageInternal;