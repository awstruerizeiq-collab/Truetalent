// import React, { useState, useRef } from "react";
// import { Mic, CheckCircle, XCircle, Play } from "lucide-react";
// import { useNavigate } from "react-router-dom";
// import verbal1 from "../../assets/images/Audio1.mp3";
// import verbal2 from "../../assets/images/Audio4.mp3";
// import verbal3 from "../../assets/images/Audio5.mp3";

// export default function SVAREThreeSessionExam() {
//     const [session, setSession] = useState(1);
//     const [responses, setResponses] = useState([]);
//     const [listeningIndex, setListeningIndex] = useState(null);
//     const [playedAudios, setPlayedAudios] = useState([]);
//     const recognitionRef = useRef(null);
//     const navigate = useNavigate();

//     // ---------------------- SESSIONS ----------------------
//     const session1 = [
//         { text: "Good morning, how are you today?", audio: verbal1 },
//         { text: "I would like to schedule a meeting for tomorrow.", audio: verbal2 },
//         { text: "Could you please send me the updated report?", audio: verbal3 },
//     ];

//     const session2 = [
//         "The defect has been logged successfully.",
//         "Regression testing will start tomorrow.",
//         "Integration testing follows unit testing.",
//     ];

//     const session3 = [
//         { text: "I would like to _____ a meeting for tomorrow.", answer: "schedule" },
//         { text: "Please send me the _____ report by evening.", answer: "updated" },
//         { text: "We will begin _____ testing soon.", answer: "regression" },
//     ];

//     const [fillAnswers, setFillAnswers] = useState(["", "", ""]);

//     // ---------------------- SPEECH RECOGNITION ----------------------
//     const initRecognition = () => {
//         const SpeechRecognition =
//             window.SpeechRecognition || window.webkitSpeechRecognition;
//         if (!SpeechRecognition) {
//             alert("Speech recognition not supported in this browser.");
//             return null;
//         }
//         const recog = new SpeechRecognition();
//         recog.lang = "en-US";
//         recog.interimResults = false;
//         recog.continuous = false;
//         return recog;
//     };

//     const startListening = (index, question) => {
//         if (listeningIndex !== null) return;
//         const recog = initRecognition();
//         if (!recog) return;

//         recognitionRef.current = recog;
//         setListeningIndex(index);

//         recog.start();

//         recog.onresult = (event) => {
//             const transcript = event.results[0][0].transcript;
//             const similarity = calculateSimilarity(question, transcript);
//             setResponses((prev) => [
//                 ...prev,
//                 { question, spoken: transcript, score: similarity },
//             ]);
//             setListeningIndex(null);
//         };

//         recog.onerror = () => setListeningIndex(null);
//         recog.onend = () => setListeningIndex(null);
//     };

//     const calculateSimilarity = (a, b) => {
//         const aw = a.toLowerCase().split(" ");
//         const bw = b.toLowerCase().split(" ");
//         const matches = aw.filter((w) => bw.includes(w)).length;
//         return Math.round((matches / aw.length) * 100);
//     };

//     const handleNextSession = () => {
//         if (session < 3) setSession(session + 1);
//     };

//     const handleFillChange = (i, val) => {
//         const newAns = [...fillAnswers];
//         newAns[i] = val;
//         setFillAnswers(newAns);
//     };

//     // ---------------------- AUDIO HANDLER ----------------------
//     const playAudioOnce = (index, audioUrl) => {
//         if (playedAudios.includes(index)) return; // already played

//         const audio = new Audio(audioUrl);
//         audio.play();
//         audio.onended = () => {
//             setPlayedAudios((prev) => [...prev, index]);
//         };
//     };

//     // ---------------------- RENDER ----------------------

//     // ======== SESSION 1: Verbal with audio playback ==========
//     if (session === 1) {
//         return (
//             <div className="min-h-screen bg-gray-100 p-8 flex flex-col items-center">
//                 <div className="bg-white shadow-xl rounded-lg p-10 max-w-3xl w-full">
//                     <h1 className="text-3xl font-bold mb-6 text-center">
//                         Session 1: Verbal Practice
//                     </h1>
//                     <p className="text-center text-gray-600 mb-6">
//                         Click the play button 🎧 to hear the sentence once. Then click the
//                         microphone 🎤 and repeat it aloud.
//                     </p>

//                     <div className="space-y-6">
//                         {session1.map((q, i) => {
//                             const response = responses.find((r) => r.question === q.text);
//                             const hasPlayed = playedAudios.includes(i);
//                             return (
//                                 <div
//                                     key={i}
//                                     className="p-6 border rounded-xl flex flex-col md:flex-row justify-between items-center bg-gray-50 gap-4"
//                                 >
//                                     <div className="flex items-center gap-3">
//                                         <button
//                                             onClick={() => playAudioOnce(i, q.audio)}
//                                             disabled={hasPlayed}
//                                             className={`rounded-full w-14 h-14 flex items-center justify-center ${hasPlayed
//                                                 ? "bg-gray-400 cursor-not-allowed"
//                                                 : "bg-blue-600 hover:bg-blue-700"
//                                                 }`}
//                                         >
//                                             <Play className="text-white w-7 h-7" />
//                                         </button>
//                                         <p className="text-lg font-medium text-gray-800">
//                                             Sentence {i + 1}
//                                         </p>
//                                     </div>

//                                     <div className="flex items-center gap-4">
//                                         <button
//                                             onClick={() => startListening(i, q.text)}
//                                             disabled={!hasPlayed || listeningIndex === i}
//                                             className={`rounded-full w-14 h-14 flex items-center justify-center ${listeningIndex === i
//                                                 ? "bg-red-500 animate-pulse"
//                                                 : !hasPlayed
//                                                     ? "bg-gray-300 cursor-not-allowed"
//                                                     : "bg-black hover:bg-gray-800"
//                                                 }`}
//                                         >
//                                             <Mic className="text-white w-7 h-7" />
//                                         </button>
//                                         {response && (
//                                             <div className="flex items-center gap-2">
//                                                 {response.score >= 80 ? (
//                                                     <CheckCircle className="text-green-500 w-6 h-6" />
//                                                 ) : (
//                                                     <XCircle className="text-red-500 w-6 h-6" />
//                                                 )}
//                                                 <span className="font-semibold text-gray-700">
//                                                     {response.score}%
//                                                 </span>
//                                             </div>
//                                         )}
//                                     </div>
//                                 </div>
//                             );
//                         })}
//                     </div>

//                     <button
//                         onClick={handleNextSession}
//                         className="mt-10 w-full bg-black text-white py-4 rounded-lg font-bold text-lg hover:bg-gray-800 transition"
//                     >
//                         Go to Session 2
//                     </button>
//                 </div>
//             </div>
//         );
//     }

//     // ======== SESSION 2 ==========
//     if (session === 2) {
//         return (
//             <div className="min-h-screen bg-gray-100 p-8 flex flex-col items-center">
//                 <div className="bg-white shadow-xl rounded-lg p-10 max-w-3xl w-full">
//                     <h1 className="text-3xl font-bold mb-6 text-center">
//                         Session 2: Sentence Repetition
//                     </h1>
//                     <p className="text-center text-gray-600 mb-6">
//                         Read each sentence aloud by clicking the microphone beside it.
//                     </p>

//                     <div className="space-y-6">
//                         {session2.map((q, i) => {
//                             const response = responses.find((r) => r.question === q);
//                             return (
//                                 <div
//                                     key={i}
//                                     className="p-6 border rounded-xl flex justify-between items-center bg-gray-50"
//                                 >
//                                     <p className="text-lg font-medium text-gray-800">{q}</p>
//                                     <button
//                                         onClick={() => startListening(i, q)}
//                                         disabled={listeningIndex === i}
//                                         className={`rounded-full w-14 h-14 flex items-center justify-center ${listeningIndex === i
//                                             ? "bg-red-500 animate-pulse"
//                                             : "bg-black hover:bg-gray-800"
//                                             }`}
//                                     >
//                                         <Mic className="text-white w-7 h-7" />
//                                     </button>
//                                     {response && (
//                                         <div className="ml-4 flex items-center gap-2">
//                                             {response.score >= 80 ? (
//                                                 <CheckCircle className="text-green-500 w-6 h-6" />
//                                             ) : (
//                                                 <XCircle className="text-red-500 w-6 h-6" />
//                                             )}
//                                             <span className="font-semibold text-gray-700">
//                                                 {response.score}%
//                                             </span>
//                                         </div>
//                                     )}
//                                 </div>
//                             );
//                         })}
//                     </div>

//                     <button
//                         onClick={handleNextSession}
//                         className="mt-10 w-full bg-black text-white py-4 rounded-lg font-bold text-lg hover:bg-gray-800 transition"
//                     >
//                         Go to Session 2
//                     </button>
//                 </div>
//             </div>
//         );
//     }

//     // ======== SESSION 3 ==========
//     if (session === 3) {
//         return (
//             <div className="min-h-screen bg-gray-100 p-8 flex flex-col items-center">
//                 <div className="bg-white shadow-xl rounded-lg p-10 max-w-3xl w-full">
//                     <h1 className="text-3xl font-bold mb-6 text-center">
//                         Session 3: Fill in the Blanks
//                     </h1>
//                     <p className="text-center text-gray-600 mb-6">
//                         Type the correct missing words in each sentence.
//                     </p>

//                     <div className="space-y-6">
//                         {session3.map((q, i) => (
//                             <div key={i} className="p-6 border rounded-xl bg-gray-50">
//                                 <p className="text-lg font-medium text-gray-800 mb-3">
//                                     {q.text}
//                                 </p>
//                                 <input
//                                     type="text"
//                                     value={fillAnswers[i]}
//                                     onChange={(e) => handleFillChange(i, e.target.value)}
//                                     placeholder="Your answer..."
//                                     className="border p-3 w-full rounded-lg focus:ring-2 focus:ring-black"
//                                 />
//                             </div>
//                         ))}
//                     </div>

//                     <button
//                         onClick={() => navigate("/exam")} // navigate to exam page
//                         className="mt-10 w-full bg-green-600 text-white py-4 rounded-lg font-bold text-lg hover:bg-green-700 transition"
//                     >
//                         Submit
//                     </button>
//                 </div>
//             </div>
//         );
//     }
// }