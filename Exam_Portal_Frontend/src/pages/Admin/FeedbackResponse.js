// import React, { useState, useEffect, useMemo } from "react";
// import axios from "axios";

// export default function AdminFeedback() {
//   const [feedbackList, setFeedbackList] = useState([]);
//   const [searchTerm, setSearchTerm] = useState("");

//   // Fetch feedbacks from API
//   const fetchFeedback = async () => {
//     try {
//       const res = await axios.get("http://localhost:8080/api/feedback");
//       setFeedbackList(res.data);
//     } catch (err) {
//       console.error("Error fetching feedback:", err);
//     }
//   };

//   useEffect(() => {
//     fetchFeedback();
//   }, []);

//   const filteredFeedback = useMemo(() => {
//     if (!searchTerm) return feedbackList;
//     return feedbackList.filter((fb) =>
//       fb.description.toLowerCase().includes(searchTerm.toLowerCase())
//     );
//   }, [feedbackList, searchTerm]);

//   const formatDate = (dateString) => {
//     const options = {
//       year: "numeric",
//       month: "long",
//       day: "numeric",
//       hour: "2-digit",
//       minute: "2-digit",
//     };
//     return new Date(dateString).toLocaleDateString(undefined, options);
//   };

//   return (
//     <div className="p-8 bg-gray-50 min-h-full">
//       <h1 className="text-4xl font-bold mb-6 text-gray-800">
//         Candidate Feedback
//       </h1>

//       {/* Search Bar */}
//       <div className="mb-6">
//         <input
//           type="text"
//           placeholder="Search feedback..."
//           value={searchTerm}
//           onChange={(e) => setSearchTerm(e.target.value)}
//           className="w-full max-w-md px-4 py-2 border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
//         />
//       </div>

//       {/* Feedback List */}
//       <div className="space-y-4">
//         {filteredFeedback.length > 0 ? (
//           filteredFeedback.map((fb) => (
//             <div
//               key={fb.id}
//               className="bg-white p-6 rounded-xl shadow-md border-l-4 border-blue-500"
//             >
//               <p className="text-gray-700 text-lg">
//                 “{fb.description}”
//               </p>
//               <p className="text-right text-sm text-gray-500 mt-4">
//                 Submitted On: {formatDate(fb.submittedOn)}
//               </p>
//             </div>
//           ))
//         ) : (
//           <div className="text-center py-10">
//             <p className="text-gray-500">No feedback matching your search.</p>
//           </div>
//         )}
//       </div>
//     </div>
//   );
// }
