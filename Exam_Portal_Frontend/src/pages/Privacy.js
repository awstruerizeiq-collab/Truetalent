import React, { useEffect } from "react";
import logo from "../assets/images/Truerize_Logo.png";

function Privacy() {
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
        color: "white", 
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

        
        <h1 style={{ fontSize: "3rem", margin: 0, textAlign: "center" ,color:"black"}}>
          🔒 Privacy Policy
        </h1>
      </div>

      
      <div
        style={{
          maxWidth: "1000px",
          margin: "0 auto",
          backgroundColor: "#6a09fae2", 
          padding: "30px",
          borderRadius: "10px",
          fontSize: "1.3rem",
          lineHeight: "2",
          boxShadow: "0 4px 10px rgba(0, 0, 0, 0.1)", 
        }}
      >
        <ul style={{ paddingLeft: "25px", listStyleType: "disc", color: "white" }}>
          <li>Truerize ensures user privacy in secure exam delivery and participation.</li>
          <li>We collect personal info (name, contact, ID, photo if required).</li>
          <li>Exam data like answers, time, scores, and warnings are recorded.</li>
          <li>Device and usage data (IP, browser, OS, logs) may be collected.</li>
          <li>Proctoring data (webcam, mic, screen, location if enabled) may be used.</li>
          <li>Data is used to verify identity, maintain integrity, generate reports, and support users.</li>
          <li>Information is shared only with institutions, employers, or trusted service providers, never sold.</li>
          <li>Data is encrypted, stored securely on cloud servers, and access is restricted.</li>
          <li>Exam data is retained as per institution policy and may later be deleted or anonymized.</li>
          <li>
            Users have rights to access, correct, or request deletion of data, and can contact support for concerns.
          </li>
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
          Accept
        </button>
      </div>
    </div>
  );
}

export default Privacy;