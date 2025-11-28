import React, { useEffect } from "react";
import logo from "../assets/images/Truerize_Logo.png";

function Help() {
  useEffect(() => {
    document.body.classList.add("info-page");
    return () => {
      document.body.classList.remove("info-page");
    };
  }, []);

  return (
    <div
      style={{
        minHeight: "100vh",
        backgroundColor: "#ffffff", 
        padding: "20px 40px",
      }}
    >
     
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          position: "relative",
          marginBottom: "40px",
        }}
      >
        
        <img
          src={logo}
          alt="Logo"
          style={{
            position: "absolute",
            left: "0",
            height: "100px",
            width: "auto",
            cursor: "pointer",
          }}
          onClick={() => (window.location.href = "/")}
        />

        
        <h1
          style={{
            fontSize: "3rem",
            margin: 0,
            textAlign: "center",
            color: "black",
          }}
        >
          🆘 Help Section
        </h1>
      </div>

      
      <div
        style={{
          maxWidth: "1000px",
          margin: "0 auto",
          backgroundColor: "#360afaff", 
          padding: "30px",
          borderRadius: "10px",
          fontSize: "1.2rem",
          lineHeight: "2",
          boxShadow: "0 4px 10px rgba(0, 0, 0, 0.1)",
          color: "white", 
        }}
      >
        <ul style={{ paddingLeft: "25px", listStyleType: "disc", color: "white" }}>
          <li>Credentials will be provided, verify your details and set a secure password.</li>
          <li>Use office-provided credentials to log in, with reset option if forgotten.</li>
          <li>Update photo, ID, and contact details to match institution records.</li>
          <li>Access exams from your homepage, check start time, and follow join instructions.</li>
          <li>Read exam rules carefully; identity verification may include photo, ID, or OTP.</li>
          <li>Navigate with next/previous buttons; timer shows remaining time; auto-submit at end.</li>
          <li>Auto-save protects data; re-login if disconnected; allow camera/mic and stay in exam window.</li>
          <li>Use supported devices (desktop/laptop, Chrome, Edge, Firefox) with 2 Mbps+ internet and permissions enabled.</li>
          <li>View results in “My Results” after evaluation; scorecard available if enabled.</li>
          <li>For issues (forgot password, camera, loading, internet), follow fixes or contact 📧 support@truerize.com / 📞 +91-08041708341.</li>
        </ul>
      </div>

      
      <div style={{ textAlign: "center" }}>
        <button
          style={{
            marginTop: "40px",
            padding: "15px 40px",
            backgroundColor: "#3a5bd8", 
            border: "none",
            borderRadius: "8px",
            color: "white",
            fontSize: "1.3rem",
            fontWeight: "bold",
            cursor: "pointer",
          }}
          onClick={() => (window.location.href = "/")}
        >
          Back
        </button>
      </div>
    </div>
  );
}

export default Help;