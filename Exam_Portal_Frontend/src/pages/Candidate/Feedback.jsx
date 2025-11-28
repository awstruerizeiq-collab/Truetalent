import React from "react";
import truerizeLogo from "../../assets/images/Truerize_Logo.png";

export default function ThankYouPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white rounded-xl shadow-lg w-full max-w-lg text-center p-12">
        <img
          src={truerizeLogo}
          alt="Truerize Logo"
          className="h-32 w-32 mx-auto mb-6"
        />
        <h2 className="text-2xl font-bold text-gray-900 mb-2">
          Thank You for submitting your Exam!
        </h2>
        <p className="text-gray-500">
          You may now close the window now.
        </p>
      </div>
    </div>
  );
}


