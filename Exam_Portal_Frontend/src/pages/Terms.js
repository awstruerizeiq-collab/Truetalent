import React, { useEffect } from "react";
import logo from "../assets/images/Truerize_Logo.png";

function Terms() {
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

        <h1 style={{ fontSize: "3rem", margin: 0, textAlign: "center", color: "black" }}>
          📑 Terms and Conditions
        </h1>
      </div>

   
      <div
        style={{
          maxWidth: "1000px",
          margin: "0 auto",
          backgroundColor: "#360bf7ff",
          padding: "30px",
          borderRadius: "10px",
          fontSize: "1.2rem",
          lineHeight: "2",
          boxShadow: "0 4px 10px rgba(0, 0, 0, 0.1)",
        }}
      >
        <p style={{ marginBottom: "20px" }}>
          Welcome to the Truerize Exam Portal. By accessing or using our services, you agree to the following terms and conditions:
        </p>

<ul style={{ paddingLeft: "25px", listStyleType: "disc", color: "white" }}>
  <li>All users must adhere to the Terms & Conditions outlined herein when using the Truerize Exam Portal.</li>
  <li>Only registered candidates with valid credentials are authorized to access exams. Users must maintain the confidentiality of their login credentials.</li>
  <li>The platform is intended solely for online assessments. Any form of tampering, unauthorized access, or malicious activity is strictly prohibited.</li>
  <li>Candidates are expected to follow exam instructions, comply with proctoring requirements, and refrain from unfair practices.</li>
  <li>Users must ensure reliable internet connectivity and compatible devices. Truerize is not responsible for disruptions due to technical limitations.</li>
  <li>Personal and exam-related data are collected and managed in accordance with our Privacy Policy and shared only with authorized institutions or employers.</li>
  <li>All exam content is the intellectual property of Truerize. Copying, sharing, or reproducing content without authorization is strictly prohibited.</li>
  <li>Exam results are system-generated and may be reviewed by the corresponding institution or employer, who holds the final authority on decisions.</li>
  <li>Truerize provides the platform "as is" and is not liable for data loss, connectivity issues, or technical failures during use.</li>
  <li>Access may be suspended or revoked for violations of these terms. Truerize reserves the right to update these Terms & Conditions in accordance with applicable laws.</li>
</ul>


        <p style={{ marginTop: "20px" }}>
          For any questions or clarifications, please contact{" "}
          <a href="mailto:support@truerize.com" style={{ color: "white" }}>
            support@truerize.com
          </a>
        </p>
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

export default Terms;
