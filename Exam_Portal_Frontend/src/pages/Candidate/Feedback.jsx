import React, { useEffect } from "react";
import truerizeLogo from "../../assets/images/Truerize_Logo.png";

export default function ThankYouPage() {
  useEffect(() => {
    document.body.classList.remove("admin-dashboard-body", "login-page-body");
    document.body.classList.add("candidate-body");
    return () => {
      document.body.classList.remove("candidate-body");
    };
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="bg-white/80 rounded-2xl shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)] border border-white/70 w-full max-w-lg text-center p-12">
        <img
          src={truerizeLogo}
          alt="Truerize Logo"
          className="h-24 w-24 mx-auto mb-6 rounded-2xl shadow-sm"
        />
        <h2 className="text-2xl font-semibold text-slate-900 mb-2">
          Thank You for submitting your Exam!
        </h2>
        <p className="text-slate-500">
          You may now close the window now.
        </p>
      </div>
    </div>
  );
}


