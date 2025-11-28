import React, { useState, useRef, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import axios from "axios";

function ExamStart() {
  const navigate = useNavigate();
  const location = useLocation();
  const { userId: passedUserId, examId } = location.state || {};

  const [userId, setUserId] = useState(passedUserId || null);
  const [step, setStep] = useState(1);
  const [error, setError] = useState("");

  const [cameraStream, setCameraStream] = useState(null);
  const [screenStream, setScreenStream] = useState(null);
  const [micStream, setMicStream] = useState(null);

  const [isRecording, setIsRecording] = useState(false);
  const [mediaRecorder, setMediaRecorder] = useState(null);
  const [recordedChunks, setRecordedChunks] = useState([]);

  const [capturedPhoto, setCapturedPhoto] = useState(null);
  const [idProofPhoto, setIdProofPhoto] = useState(null);

  const [uploadStatus, setUploadStatus] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [isExamStarted, setIsExamStarted] = useState(false);

  const verificationVideoRef = useRef(null);
  const idVideoRef = useRef(null);
  const verificationCanvasRef = useRef(null);
  const idCanvasRef = useRef(null);

  
  useEffect(() => {
    const getCurrentUser = async () => {
      try {
        const res = await axios.get("http://localhost:8080/api/auth/current-user", {
          withCredentials: true
        });
        if (res.data && res.data.userId) {
          setUserId(res.data.userId);
        }
      } catch (err) {
        console.error("❌ Failed to get user:", err);
        setError("Failed to authenticate. Please login again.");
      }
    };

    if (!userId) {
      getCurrentUser();
    }
  }, [userId]);

  

  
  const requestCameraAndMic = async () => {
    try {
      const camStream = await navigator.mediaDevices.getUserMedia({
        video: { width: 1280, height: 720 },
        audio: true,
      });
      setCameraStream(camStream);
      setMicStream(camStream);
      setError("");
      setStep(2);
    } catch (err) {
      console.error("Error accessing camera/mic:", err);
      setError("Camera and microphone access is required to proceed with the exam.");
    }
  };

  
  const requestScreenShare = async () => {
    try {
      const screenMediaStream = await navigator.mediaDevices.getDisplayMedia({
        video: { width: 1920, height: 1080 },
        audio: true,
      });
      setScreenStream(screenMediaStream);
      setError("");
      return true;
    } catch (err) {
      console.error("Screen share error:", err);
      setError("Screen sharing is required for exam proctoring.");
      return false;
    }
  };

  
  const startRecording = async () => {
    try {
      const hasScreenShare = await requestScreenShare();
      if (!hasScreenShare) return false;

      const combinedStream = new MediaStream();
      if (screenStream) screenStream.getVideoTracks().forEach((t) => combinedStream.addTrack(t));
      if (micStream) micStream.getAudioTracks().forEach((t) => combinedStream.addTrack(t));

      const recorder = new MediaRecorder(combinedStream, { mimeType: "video/webm;codecs=vp9,opus" });
      const chunks = [];
      recorder.ondataavailable = (e) => e.data.size > 0 && chunks.push(e.data);
      recorder.onstop = () => {
        const blob = new Blob(chunks, { type: "video/webm" });
      };

      recorder.start(1000);
      setMediaRecorder(recorder);
      setRecordedChunks(chunks);
      setIsRecording(true);
      return true;
    } catch (err) {
      setError("Unable to start recording session.");
      return false;
    }
  };

  
  const capturePhoto = () => {
    const video = verificationVideoRef.current;
    const canvas = verificationCanvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext("2d").drawImage(video, 0, 0, canvas.width, canvas.height);
    setCapturedPhoto(canvas.toDataURL("image/jpeg"));
  };

  const captureIdProof = () => {
    const video = idVideoRef.current;
    const canvas = idCanvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext("2d").drawImage(video, 0, 0, canvas.width, canvas.height);
    setIdProofPhoto(canvas.toDataURL("image/jpeg"));
  };

  const confirmPhoto = () => setStep(3);

  const retakePhoto = () => {
    setCapturedPhoto(null);
    if (verificationVideoRef.current && cameraStream) {
      verificationVideoRef.current.srcObject = cameraStream;
      verificationVideoRef.current.play().catch((e) => console.error("Error playing video:", e));
    }
  };

  const confirmIdProof = async () => {
    try {
      if (!capturedPhoto || !idProofPhoto) {
        setUploadStatus("Please capture both verification and ID proof photos.");
        return;
      }

      if (!userId || !examId) {
        setUploadStatus("Invalid user or exam credentials.");
        console.error("Invalid IDs:", { userId, examId });
        return;
      }

      const uploadSuccess = await uploadPhotosToDatabase(capturedPhoto, idProofPhoto, userId, examId);

      if (uploadSuccess) {
        setStep(4);
      }
    } catch (err) {
      console.error("Error confirming ID proof:", err);
      setUploadStatus("Verification failed. Please try again.");
    }
  };

  const retakeIdProof = () => {
    setIdProofPhoto(null);
    setUploadStatus("");
    if (idVideoRef.current && cameraStream) {
      idVideoRef.current.srcObject = cameraStream;
      idVideoRef.current.play().catch((e) => console.error("Error playing video:", e));
    }
  };

  const startExam = async () => {
    if (!userId) {
      alert("Unable to start exam. Please refresh and try again.");
      return;
    }

    if (await startRecording()) {
      setIsExamStarted(true);
      
      
      const quizSessionKey = `quiz_access_${userId}`;
      sessionStorage.setItem(quizSessionKey, 'true');
      
      
      setTimeout(() => {
        navigate("/quiz");
      }, 300);
    }
  };

  
  const uploadPhotosToDatabase = async (verificationPhoto, idPhoto, userId, examId) => {
    try {
      setIsUploading(true);
      setUploadStatus("Uploading verification documents...");

      const verificationBlob = await fetch(verificationPhoto).then((r) => r.blob());
      const idBlob = await fetch(idPhoto).then((r) => r.blob());

      const formData = new FormData();
      formData.append("photo", verificationBlob, "verification.jpg");
      formData.append("idProof", idBlob, "id_proof.jpg");
      formData.append("cameraEnabled", "true");
      formData.append("microphoneEnabled", "true");
      formData.append("screenSharingEnabled", screenStream ? "true" : "false");
      formData.append("userId", userId.toString());
      formData.append("examId", examId.toString());

      const response = await fetch("http://localhost:8080/api/proctoring/save", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error("Upload failed: " + errText);
      }

      const result = await response.json();

      setUploadStatus("Verification completed successfully");
      setIsUploading(false);
      return true;

    } catch (err) {
      console.error("Upload error:", err);
      setUploadStatus("Upload failed: " + err.message);
      setIsUploading(false);
      return false;
    }
  };

  useEffect(() => {
    if (verificationVideoRef.current && cameraStream && step === 2 && !capturedPhoto) {
      verificationVideoRef.current.srcObject = cameraStream;
      verificationVideoRef.current.play().catch((e) => console.error("Error playing verification video:", e));
    }
  }, [cameraStream, step, capturedPhoto]);

  useEffect(() => {
    if (idVideoRef.current && cameraStream && step === 3 && !idProofPhoto) {
      idVideoRef.current.srcObject = cameraStream;
      idVideoRef.current.play().catch((e) => console.error("Error playing ID video:", e));
    }
  }, [step, cameraStream, idProofPhoto]);

  useEffect(() => {
    return () => {
      if (cameraStream) cameraStream.getTracks().forEach((t) => t.stop());
      if (screenStream) screenStream.getTracks().forEach((t) => t.stop());
      if (isRecording && mediaRecorder) mediaRecorder.stop();
    };
  }, [cameraStream, screenStream, isRecording, mediaRecorder]);


  const renderInitialPrompt = () => (
    <div className="bg-white shadow-2xl rounded-3xl p-10 text-center max-w-md w-full border border-gray-100">
      <div className="mb-6">
        <div className="w-20 h-20 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
          <svg className="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Exam Setup</h1>
        <p className="text-gray-500 text-sm">System Prerequisites</p>
      </div>
      
      <div className="bg-blue-50 border border-blue-100 rounded-2xl p-6 mb-6 text-left">
        <p className="text-gray-700 text-sm leading-relaxed">
          To ensure a secure and proctored examination environment, we require access to your:
        </p>
        <div className="mt-4 space-y-3">
          <div className="flex items-center text-gray-800">
            <svg className="w-5 h-5 text-blue-600 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            <span className="font-semibold text-sm">Camera</span>
          </div>
          <div className="flex items-center text-gray-800">
            <svg className="w-5 h-5 text-blue-600 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
            </svg>
            <span className="font-semibold text-sm">Microphone</span>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6 flex items-start">
          <svg className="w-5 h-5 text-red-500 mr-3 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p className="text-red-700 text-sm leading-relaxed">{error}</p>
        </div>
      )}

      <button
        onClick={requestCameraAndMic}
        className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-semibold py-4 rounded-xl hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
      >
        Enable Camera & Microphone
      </button>
      
      <p className="text-xs text-gray-500 mt-4">
        Your privacy is protected. All data is encrypted and secure.
      </p>
    </div>
  );

  const renderMediaCapture = () => (
    <div className="bg-white shadow-2xl rounded-3xl p-8 w-full max-w-3xl border border-gray-100">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <h1 className="text-2xl font-bold text-gray-900">Identity Verification</h1>
          <span className="bg-blue-100 text-blue-700 text-xs font-semibold px-3 py-1 rounded-full">Step 1 of 3</span>
        </div>
        <p className="text-gray-600 text-sm">Please position yourself in the center of the frame</p>
      </div>

      {!capturedPhoto ? (
        <>
          <div className="relative w-full aspect-video mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-2xl overflow-hidden shadow-inner">
            {cameraStream ? (
              <>
                <video ref={verificationVideoRef} autoPlay playsInline muted className="w-full h-full object-cover" />
                <div className="absolute inset-0 border-4 border-blue-500 opacity-30 rounded-2xl pointer-events-none" 
                     style={{clipPath: "polygon(20% 20%, 80% 20%, 80% 80%, 20% 80%)"}}></div>
              </>
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-gray-500">
                <svg className="w-16 h-16 mb-4 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
                <p className="text-sm font-medium">Initializing camera...</p>
              </div>
            )}
          </div>
          
          <div className="flex flex-col items-center">
            <button
              onClick={capturePhoto}
              disabled={!cameraStream}
              className={`font-semibold py-4 px-10 rounded-xl transition-all duration-200 shadow-lg ${
                cameraStream
                  ? "bg-gradient-to-r from-green-500 to-emerald-600 text-white hover:from-green-600 hover:to-emerald-700 hover:shadow-xl transform hover:-translate-y-0.5"
                  : "bg-gray-300 text-gray-500 cursor-not-allowed"
              }`}
            >
              <span className="flex items-center">
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                Capture Photo
              </span>
            </button>
            <p className="text-xs text-gray-500 mt-3">Ensure your face is clearly visible</p>
          </div>
        </>
      ) : (
        <div>
          <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 mb-4">
            <p className="text-blue-800 text-sm font-medium">Please review your verification photo carefully</p>
          </div>
          <img src={capturedPhoto} alt="Verification" className="mx-auto max-w-2xl w-full rounded-2xl border-4 border-gray-200 mb-6 shadow-lg" />
          <div className="flex justify-center space-x-4">
            <button onClick={retakePhoto} className="bg-gray-600 text-white font-semibold py-3 px-8 rounded-xl hover:bg-gray-700 transition-all duration-200 shadow-md hover:shadow-lg flex items-center">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Retake Photo
            </button>
            <button onClick={confirmPhoto} className="bg-gradient-to-r from-green-500 to-emerald-600 text-white font-semibold py-3 px-8 rounded-xl hover:from-green-600 hover:to-emerald-700 transition-all duration-200 shadow-lg hover:shadow-xl flex items-center">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              Confirm & Continue
            </button>
          </div>
        </div>
      )}
      <canvas ref={verificationCanvasRef} style={{ display: "none" }} />
    </div>
  );

  const renderIdProofCapture = () => (
    <div className="bg-white shadow-2xl rounded-3xl p-8 w-full max-w-3xl border border-gray-100">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-2">
          <h1 className="text-2xl font-bold text-gray-900">ID Proof Verification</h1>
          <span className="bg-indigo-100 text-indigo-700 text-xs font-semibold px-3 py-1 rounded-full">Step 2 of 3</span>
        </div>
        <p className="text-gray-600 text-sm">Hold your government-issued ID clearly in frame</p>
      </div>

      {!idProofPhoto ? (
        <>
          <div className="relative w-full aspect-video mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-2xl overflow-hidden shadow-inner">
            {cameraStream ? (
              <video ref={idVideoRef} autoPlay playsInline muted className="w-full h-full object-cover" />
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-gray-500">
                <svg className="w-16 h-16 mb-4 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
                <p className="text-sm font-medium">Initializing camera...</p>
              </div>
            )}
          </div>
          
          <div className="flex flex-col items-center">
            <button
              onClick={captureIdProof}
              disabled={!cameraStream}
              className={`font-semibold py-4 px-10 rounded-xl transition-all duration-200 shadow-lg ${
                cameraStream
                  ? "bg-gradient-to-r from-indigo-500 to-blue-600 text-white hover:from-indigo-600 hover:to-blue-700 hover:shadow-xl transform hover:-translate-y-0.5"
                  : "bg-gray-300 text-gray-500 cursor-not-allowed"
              }`}
            >
              <span className="flex items-center">
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2" />
                </svg>
                Capture ID Proof
              </span>
            </button>
            <p className="text-xs text-gray-500 mt-3">Ensure all details are clearly readable</p>
          </div>
        </>
      ) : (
        <div>
          <div className="bg-indigo-50 border border-indigo-100 rounded-xl p-4 mb-4">
            <p className="text-indigo-800 text-sm font-medium">Verify that your ID details are clearly visible</p>
          </div>
          <img src={idProofPhoto} alt="ID Proof" className="mx-auto max-w-2xl w-full rounded-2xl border-4 border-gray-200 mb-6 shadow-lg" />
          
          {uploadStatus && (
            <div className={`mb-6 p-4 rounded-xl flex items-start ${
              uploadStatus.includes("successfully")
                ? "bg-green-50 border border-green-200"
                : uploadStatus.includes("failed")
                  ? "bg-red-50 border border-red-200"
                  : "bg-blue-50 border border-blue-200"
            }`}>
              <svg className={`w-5 h-5 mr-3 flex-shrink-0 mt-0.5 ${
                uploadStatus.includes("successfully")
                  ? "text-green-600"
                  : uploadStatus.includes("failed")
                    ? "text-red-600"
                    : "text-blue-600"
              }`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {uploadStatus.includes("successfully") ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                ) : uploadStatus.includes("failed") ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                )}
              </svg>
              <p className={`text-sm font-medium ${
                uploadStatus.includes("successfully")
                  ? "text-green-800"
                  : uploadStatus.includes("failed")
                    ? "text-red-800"
                    : "text-blue-800"
              }`}>
                {uploadStatus}
              </p>
            </div>
          )}

          <div className="flex justify-center space-x-4">
            <button
              onClick={retakeIdProof}
              disabled={isUploading}
              className="bg-gray-600 text-white font-semibold py-3 px-8 rounded-xl hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-md hover:shadow-lg flex items-center"
            >
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Retake Photo
            </button>
            <button
              onClick={confirmIdProof}
              disabled={isUploading}
              className="bg-gradient-to-r from-green-500 to-emerald-600 text-white font-semibold py-3 px-8 rounded-xl hover:from-green-600 hover:to-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg hover:shadow-xl flex items-center"
            >
              {isUploading ? (
                <>
                  <svg className="animate-spin w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Uploading...
                </>
              ) : (
                <>
                  <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  Confirm & Continue
                </>
              )}
            </button>
          </div>
        </div>
      )}
      <canvas ref={idCanvasRef} style={{ display: "none" }} />
    </div>
  );

  const renderSetupComplete = () => (
    <div className="bg-white shadow-2xl rounded-3xl p-10 text-center max-w-md w-full border border-gray-100">
      <div className="mb-6">
        <div className="w-20 h-20 bg-gradient-to-br from-green-500 to-emerald-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
          <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Setup Complete</h1>
        <p className="text-gray-500 text-sm">Ready to begin examination</p>
      </div>

      <div className="bg-green-50 border border-green-100 rounded-2xl p-6 mb-6">
        <p className="text-green-800 text-sm font-medium mb-4">Verification successful</p>
        <div className="flex justify-center space-x-4">
          {capturedPhoto && (
            <div className="text-center">
              <img src={capturedPhoto} alt="Verification" className="w-24 h-24 rounded-xl object-cover border-2 border-green-500 shadow-md mb-2" />
              <p className="text-xs text-gray-600 font-medium">Identity</p>
            </div>
          )}
          {idProofPhoto && (
            <div className="text-center">
              <img src={idProofPhoto} alt="ID Proof" className="w-24 h-24 rounded-xl object-cover border-2 border-blue-500 shadow-md mb-2" />
              <p className="text-xs text-gray-600 font-medium">ID Proof</p>
            </div>
          )}
        </div>
      </div>

      {uploadStatus && (
        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-xl">
          <p className="text-sm text-blue-800 font-medium">{uploadStatus}</p>
        </div>
      )}

      <button
        onClick={startExam}
        className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-bold py-4 rounded-xl hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 flex items-center justify-center"
      >
        <svg className="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        Start Exam
      </button>

      <div className="mt-6 pt-6 border-t border-gray-200">
        <div className="flex items-center justify-center space-x-6 text-xs text-gray-500">
          <div className="flex items-center">
            <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
            <span>Camera Active</span>
          </div>
          <div className="flex items-center">
            <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
            <span>Microphone Active</span>
          </div>
        </div>
      </div>

      <p className="text-xs text-gray-500 mt-4 leading-relaxed">
        Once you start, screen recording will begin. Do not switch tabs or close this window during the exam.
      </p>
    </div>
  );

  return (
    <div style={{ minHeight: '100vh', overflowY: 'scroll', height: '100vh' }} className="bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 p-4 py-8">
      <div className="flex items-center justify-center min-h-full">
        {step === 1 && renderInitialPrompt()}
        {step === 2 && renderMediaCapture()}
        {step === 3 && renderIdProofCapture()}
        {step === 4 && renderSetupComplete()}
      </div>
    </div>
  );
}

export default ExamStart