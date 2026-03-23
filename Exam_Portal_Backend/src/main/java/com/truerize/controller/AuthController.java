package com.truerize.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.truerize.dto.LoginRequest;
import com.truerize.entity.Role;
import com.truerize.entity.Slot;
import com.truerize.entity.User;
import com.truerize.entity.StudentExamAssignment;
import com.truerize.service.UserService;
import com.truerize.service.ExamService;
import com.truerize.service.ExamSetService;
import com.truerize.service.SlotService;
import com.truerize.repository.StudentExamAssignmentRepo;
import com.truerize.repository.TestSubmissionRepository;
import com.truerize.entity.TestSubmission;

import jakarta.servlet.http.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int SLOT_WINDOW_MINUTES = 30; // Login window duration
    private static final int SLOT_GRACE_MINUTES = 1; // Grace for boundary seconds and network delay
    private static final ZoneId EXAM_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired private UserService userService;
    @Autowired private ExamService examService;
    @Autowired private ExamSetService examSetService;
    @Autowired private SlotService slotService;
    @Autowired private StudentExamAssignmentRepo assignmentRepo;
    @Autowired private TestSubmissionRepository testSubmissionRepository;

    @PostMapping("/addCandidate")
    public ResponseEntity<?> addCandidate(@RequestBody User user) {
        log.info("📝 Add Candidate Request: {}", user.getEmail());
        
        try {
            if (userService.existsByEmail(user.getEmail())) {
                log.warn("❌ Email already exists: {}", user.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "success", false,
                        "error", "Email Already Exists",
                        "message", "This email is already registered. Please use a different email."
                    ));
            }

            User newUser = userService.createUser(user);
            
            log.info("✅ Candidate created successfully: {} (ID: {})", newUser.getEmail(), newUser.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "User created successfully",
                "user", newUser
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", "Validation Error",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            log.error("❌ Error adding candidate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Server Error",
                    "message", "An unexpected error occurred: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session,
                                   HttpServletRequest httpRequest) {

        try {
            log.info("🔐 Login attempt for: {}", request.getEmail());
            
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty())
                return ResponseEntity.status(400).body(Map.of(
                    "success", false, 
                    "error", "Validation Error",
                    "message", "Email is required"
                ));

            if (request.getPassword() == null || request.getPassword().trim().isEmpty())
                return ResponseEntity.status(400).body(Map.of(
                    "success", false, 
                    "error", "Validation Error",
                    "message", "Password is required"
                ));

           
            Optional<User> userOptional = userService.authenticate(request.getEmail(), request.getPassword());
            if (userOptional.isEmpty()) {
                log.warn("❌ Invalid credentials for: {}", request.getEmail());
                return ResponseEntity.status(401).body(Map.of(
                    "success", false, 
                    "error", "Invalid Credentials",
                    "message", "Invalid email or password"
                ));
            }

            User user = userOptional.get();
            String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("CANDIDATE");

            
            if ("Blocked".equalsIgnoreCase(user.getStatus())) {
                log.warn("❌ Blocked user attempted login: {}", request.getEmail());
                return ResponseEntity.status(403).body(Map.of(
                    "success", false, 
                    "error", "Account Blocked",
                    "message", "Your account has been blocked. Please contact support."
                ));
            }

           
            if ("CANDIDATE".equals(role)) {
                String studentId = String.valueOf(user.getId());
                int studentIdInt = user.getId();
                
                log.info("👨‍🎓 Candidate login - checking exam status and slot timing");
                
               
                try {
                    List<TestSubmission> completedSubmissions = testSubmissionRepository.findByUserId(studentIdInt);
                    
                    for (TestSubmission submission : completedSubmissions) {
                        if ("Completed".equalsIgnoreCase(submission.getStatus())) {
                            log.warn("🚫 Student {} has completed exam {}", studentId, submission.getExam().getId());
                            
                            return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "error", "Exam Already Submitted",
                                "message", "Your exam has been successfully submitted and cannot be retaken.",
                                "examCompleted", true,
                                "examSubmitted", true,
                                "completedAt", submission.getSubmittedAt() != null 
                                        ? submission.getSubmittedAt().toString() 
                                        : "N/A",
                                "examId", submission.getExam().getId()
                            ));
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Could not check TestSubmission: {}", e.getMessage());
                }
                
              
                try {
                    List<StudentExamAssignment> completedExams = 
                        assignmentRepo.findByStudentIdAndHasCompletedTrue(studentId);
                    
                    if (!completedExams.isEmpty()) {
                        StudentExamAssignment lastCompleted = completedExams.get(0);
                        
                        log.warn("🚫 Student {} has completed exam {}", studentId, lastCompleted.getExamId());
                        
                        return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "error", "Exam Already Submitted",
                            "message", "Your exam has been successfully submitted and cannot be retaken.",
                            "examCompleted", true,
                            "examSubmitted", true,
                            "completedAt", lastCompleted.getCompletedAt() != null 
                                    ? lastCompleted.getCompletedAt().toString() 
                                    : "N/A",
                            "examId", lastCompleted.getExamId()
                        ));
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Could not check StudentExamAssignment: {}", e.getMessage());
                }
                
                
                try {
                    List<StudentExamAssignment> activeExams = 
                        assignmentRepo.findByStudentIdAndHasStartedTrueAndHasCompletedFalse(studentId);
                    
                    if (!activeExams.isEmpty()) {
                        StudentExamAssignment activeExam = activeExams.get(0);
                        
                        log.warn("🚨 Student {} has active exam session for exam {}", 
                                 studentId, activeExam.getExamId());
                        
                        return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "error", "Exam Already in Progress",
                            "message", "You have an exam session in progress. Please complete your exam or contact support.",
                            "activeSession", true,
                            "examInProgress", true,
                            "startedAt", activeExam.getStartedAt() != null 
                                    ? activeExam.getStartedAt().toString() 
                                    : "N/A",
                            "examId", activeExam.getExamId()
                        ));
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Could not check active exams: {}", e.getMessage());
                }
                
              
                Integer userSlotNumber = user.getSlotNumber();
                
                if (userSlotNumber == null) {
                    log.warn("⚠️ Student {} has no slot number assigned", studentId);
                    return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "No Slot Assigned",
                        "message", "You have not been assigned an exam slot. Please contact support."
                    ));
                }
                
                Optional<Slot> currentSlotOpt = slotService.getSlotBySlotNumber(userSlotNumber);
                
                if (currentSlotOpt.isEmpty()) {
                    log.warn("⚠️ Slot number {} not found in database", userSlotNumber);
                    return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Slot Not Found",
                        "message", "Your assigned slot (Slot " + userSlotNumber + ") no longer exists. Please contact support."
                    ));
                }
                
                Slot currentSlot = currentSlotOpt.get();
                
                
                if (currentSlot.getDate() == null || currentSlot.getTime() == null) {
                    log.warn("⚠️ Slot {} has no date/time configured", userSlotNumber);
                    return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Slot Not Configured",
                        "message", "Your exam slot (Slot " + userSlotNumber + ") does not have a scheduled date/time. Please contact support."
                    ));
                }
                
                // Evaluate slot timings in exam timezone (IST) without manual offsets
                LocalDateTime now = LocalDateTime.now(EXAM_TIME_ZONE);
                LocalDate slotDate = currentSlot.getDate();
                LocalTime slotTime = currentSlot.getTime();
                
                LocalDateTime slotStart = LocalDateTime.of(slotDate, slotTime);
                LocalDateTime slotEnd = slotStart.plusMinutes(SLOT_WINDOW_MINUTES);
                LocalDateTime slotStartWithGrace = slotStart.minusMinutes(SLOT_GRACE_MINUTES);
                LocalDateTime slotEndWithGrace = slotEnd.plusMinutes(SLOT_GRACE_MINUTES);
                
                log.info("🕐 Slot {} validation - Current: {}, Slot Start: {}, Slot End: {}", 
                        userSlotNumber, now, slotStart, slotEnd);
                
             
                if (now.isBefore(slotStartWithGrace)) {
                    log.warn("⏰ REJECTED - Login too early. Student: {}, Slot: {}", 
                            studentId, userSlotNumber);
                    
                    return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Slot Not Yet Started",
                        "message", String.format(
                            "⏰ Your exam slot has not started yet.\n\n" +
                            "You can only login between %s and %s on %s.\n" +
                            "You have a 30-minute window to login.\n" +
                            "Once you login, you will have 1 hour to complete the exam.\n\n" +
                            "Please wait until your assigned slot time.\n\n" +
                            "Current time: %s",
                            slotTime.toString(),
                            slotEnd.toLocalTime().toString(),
                            slotDate.toString(),
                            now.toLocalTime().toString()
                        ),
                        "slotNumber", userSlotNumber,
                        "slotDate", slotDate.toString(),
                        "slotTime", slotTime.toString(),
                        "slotEndTime", slotEnd.toLocalTime().toString(),
                        "currentTime", now.toString(),
                        "tooEarly", true
                    ));
                }
                
             
                if (now.isAfter(slotEndWithGrace)) {
                    log.warn("⏰ REJECTED - Login too late. Student: {}, Slot: {}", 
                            studentId, userSlotNumber);
                    
                    return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Slot Time Expired",
                        "message", String.format(
                            "⏰ Your exam slot has expired.\n\n" +
                            "The login window was between %s and %s on %s.\n" +
                            "You had 30 minutes to login and start the exam.\n" +
                            "The exam duration is 1 hour after login.\n\n" +
                            "Please contact support if you missed your slot.\n" +
                            "Email: hr@truerize.com",
                            slotTime.toString(),
                            slotEnd.toLocalTime().toString(),
                            slotDate.toString()
                        ),
                        "slotNumber", userSlotNumber,
                        "slotDate", slotDate.toString(),
                        "slotTime", slotTime.toString(),
                        "slotEndTime", slotEnd.toLocalTime().toString(),
                        "currentTime", now.toString(),
                        "tooLate", true
                    ));
                }
                
              
                log.info("✅ SLOT TIMING VALIDATED - Student {} can access exam (within 30-min window for Slot {})", 
                         studentId, userSlotNumber);
               
                if (request.getExamId() != null) {
                    Integer examId = request.getExamId();
                    log.info("📝 Starting exam {} for student {}", examId, studentId);
                    
                    try {
                        boolean examAssignedToUser = user.getAssignedExams() != null
                            && user.getAssignedExams().stream().anyMatch(e -> e.getId() == examId);

                        if (!examAssignedToUser) {
                            return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "error", "Access Denied",
                                "message", "You are not registered for this exam."
                            ));
                        }

                        Optional<StudentExamAssignment> assignOpt = 
                            assignmentRepo.findByStudentIdAndExamId(studentId, examId);

                        if (assignOpt.isEmpty()) {
                            Map<String, Object> autoAssignResult =
                                examSetService.autoAssignStudentToSet(studentId, examId.longValue());

                            if (!Boolean.TRUE.equals(autoAssignResult.get("success"))) {
                                log.warn("Auto-assignment failed for student {} exam {}: {}",
                                    studentId, examId, autoAssignResult.get("error"));
                            }

                            assignOpt = assignmentRepo.findByStudentIdAndExamId(studentId, examId);
                        }
                        
                        if (assignOpt.isEmpty()) {
                            Map<String, Object> autoAssignResult =
                                examSetService.autoAssignStudentToSet(studentId, examId.longValue());

                            if (!Boolean.TRUE.equals(autoAssignResult.get("success"))) {
                                log.warn("Auto-assignment failed for student {} exam {}: {}",
                                    studentId, examId, autoAssignResult.get("error"));
                            }

                            assignOpt = assignmentRepo.findByStudentIdAndExamId(studentId, examId);

                            if (assignOpt.isEmpty()) {
                                return ResponseEntity.status(403).body(Map.of(
                                    "success", false,
                                    "error", "Exam Setup Pending",
                                    "message", "Your exam is assigned, but question set setup is incomplete. Please contact admin."
                                ));
                            }
                        }
                        
                        StudentExamAssignment exam = assignOpt.get();
                        
                        if (exam.getHasCompleted()) {
                            log.warn("⚠️ Exam {} already completed by student {}", examId, studentId);
                            return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "error", "Exam Already Submitted",
                                "message", "You have already submitted this exam.",
                                "examCompleted", true,
                                "examSubmitted", true
                            ));
                        }
                        
                        examService.startExam(studentId, examId);
                        
                        session.setAttribute("examStarted", true);
                        session.setAttribute("currentExamId", examId);
                        
                        log.info("✅ Exam {} started for student {}", examId, studentId);
                        
                    } catch (Exception e) {
                        log.error("❌ Error starting exam", e);
                        return ResponseEntity.status(500).body(Map.of(
                            "success", false,
                            "error", "Failed to Start Exam",
                            "message", "Could not start the exam. Please try again or contact support."
                        ));
                    }
                }
            }

          
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", role);
            session.setAttribute("email", user.getEmail());
            session.setAttribute("userName", user.getName());

            log.info("✅ Login successful for: {} (Role: {})", user.getEmail(), role);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login Successful",
                    "role", role,
                    "userName", user.getName(),
                    "userId", user.getId()
            ));

        } catch(Exception e) {
            log.error("❌ Login error: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false, 
                "error", "Login Failed",
                "message", "An unexpected error occurred. Please try again or contact support."
            ));
        }
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No active session"));
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("email", session.getAttribute("email"));
        userData.put("role", session.getAttribute("role"));
        userData.put("userName", session.getAttribute("userName"));
        
        if (session.getAttribute("examStarted") != null) {
            userData.put("examStarted", session.getAttribute("examStarted"));
        }
        if (session.getAttribute("currentExamId") != null) {
            userData.put("currentExamId", session.getAttribute("currentExamId"));
        }
        
        return ResponseEntity.ok(userData);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        try {
            if (session.getAttribute("examStarted") != null && 
                session.getAttribute("userId") != null &&
                session.getAttribute("currentExamId") != null) {
                
                String studentId = String.valueOf(session.getAttribute("userId"));
                Integer examId = (Integer) session.getAttribute("currentExamId");
                
                log.info("🏁 Auto-completing exam {} for student {}", examId, studentId);
                
                try {
                    examService.completeExam(studentId, examId);
                    log.info("✅ Exam completed successfully");
                } catch (Exception e) {
                    log.error("❌ Error completing exam on logout", e);
                }
            }

            session.invalidate();
            log.info("✅ Logout successful");
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Logout successful"));

        } catch(Exception e) {
            log.error("❌ Logout error: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false, 
                "error", "Logout failed"
            ));
        }
    }
}

