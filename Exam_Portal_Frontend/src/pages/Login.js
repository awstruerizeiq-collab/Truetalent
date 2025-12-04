import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "../api/axiosConfig"; // uses baseURL=/api from axiosConfig
import "../index.css";
import logo from "../assets/images/Truerize_Logo.png";
import helpVideo from "../assets/images/help-video.mp4";

function LoginPageInternal() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showWelcomeMessage, setShowWelcomeMessage] = useState(false);

  // Apply login page body class
  useEffect(() => {
    document.body.classList.remove("admin-dashboard-body", "candidate-body");
    document.body.classList.add("login-page-body");
    return () => {
      document.body.classList.remove("login-page-body");
    };
  }, []);

  // Clear any old session and prevent back navigation
  useEffect(() => {
    localStorage.clear();
    sessionStorage.clear();

    // Always drop any existing backend session when landing on login
    const clearServerSession = async () => {
      try {
        await axios.post("/auth/logout", {}, { withCredentials: true });
      } catch (e) {
        console.log("No server session to clear on load");
      }
    };
    clearServerSession();

    window.history.pushState(null, document.title, window.location.href);
    const handlePopState = () => {
      window.history.pushState(null, document.title, window.location.href);
    };

    window.addEventListener("popstate", handlePopState);
    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, []);

  // Check if there is an already active session
  useEffect(() => {
    const checkSession = async () => {
      const justLoggedOut = sessionStorage.getItem("justLoggedOut");
      if (justLoggedOut) {
        sessionStorage.removeItem("justLoggedOut");
        return;
      }

      try {
        // NOTE: relative path -> /api/auth/current-user via axiosConfig baseURL
        const res = await axios.get("/auth/current-user", {
          withCredentials: true,
        });
        const user = res.data;

        console.log("Current session user:", user);

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
    setError("");
    setIsLoading(true);

    try {
      if (!email.trim() || !password.trim()) {
        setError("Please enter both email and password");
        setIsLoading(false);
        return;
      }

      console.log("Attempting login for:", email);

      // IMPORTANT: relative URL, goes to /api/auth/login through nginx
      const response = await axios.post(
        "/auth/login",
        {
          email: email.trim(),
          password,
        },
        {
          withCredentials: true,
        }
      );

      console.log("Login API Response:", response.data);

      if (!response.data.success) {
        setError(response.data.error || "Login failed");
        setIsLoading(false);
        return;
      }

      // Clear logout marker if any
      sessionStorage.removeItem("justLoggedOut");

      // Fetch logged in user details
      const userRes = await axios.get("/auth/current-user", {
        withCredentials: true,
      });
      const user = userRes.data;

      console.log("User data after login:", user);

      if (user.role === "ADMIN") {
        console.log("Redirecting to admin dashboard");
        navigate("/dashboard", { replace: true });
      } else if (user.role === "CANDIDATE") {
        console.log("Redirecting to candidate dashboard");
        navigate("/candidate", { replace: true });
      } else {
        console.log("Role not recognized, redirecting to candidate dashboard");
        navigate("/candidate", { replace: true });
      }
    } catch (err) {
      console.error("Login error:", err);

      if (err.response) {
        const errorMessage =
          err.response.data?.error ||
          err.response.data?.message ||
          "Invalid email or password";
        setError(errorMessage);
      } else if (err.request) {
        setError("Cannot connect to server. Please check if backend is running.");
      } else {
        setError("An unexpected error occurred");
      }
    } finally {
      setIsLoading(false);
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
        {/* LEFT PANEL */}
        <div className="login-left">
          <div className="logo-wrapper" onClick={handleLogoClick}>
            <img src={logo} alt="Logo" className="logo" />
          </div>

          <h1 className="welcome-heading">Welcome to Truerize Exam Portal</h1>
          <h2>Proceed to your exam</h2>

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
              <p
                className="error-message"
                style={{
                  color: "#dc2626",
                  marginTop: "10px",
                  fontSize: "14px",
                  fontWeight: "500",
                }}
              >
                {error}
              </p>
            )}

            {isLoading && (
              <p
                className="loading-message"
                style={{
                  color: "#0066cc",
                  marginTop: "10px",
                  fontSize: "14px",
                }}
              >
                Logging in...
              </p>
            )}

            <button
              type="submit"
              className="login-btn"
              disabled={isLoading}
              style={{ opacity: isLoading ? 0.6 : 1 }}
            >
              {isLoading ? "Logging in..." : "Login"}
            </button>
          </form>

          <footer>
            <p>© Copyright Truerize 2025</p>
            <div className="footer-links">
              <Link to="/terms">Terms & Condition</Link>
              <Link to="/privacy">Privacy Policy</Link>
              <Link to="/help">Help</Link>
            </div>
          </footer>
        </div>

        {/* RIGHT PANEL */}
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
    </div>
  );
}

export default LoginPageInternal;
