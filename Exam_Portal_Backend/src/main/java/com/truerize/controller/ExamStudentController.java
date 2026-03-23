package com.truerize.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.Question;
import com.truerize.entity.StudentExamAssignment;
import com.truerize.entity.User;
import com.truerize.repository.StudentExamAssignmentRepo;
import com.truerize.repository.UserRepository;
import com.truerize.service.ExamSetService;
import com.truerize.service.ExamService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/candidate")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExamStudentController {

    private static final Logger log = LoggerFactory.getLogger(ExamStudentController.class);

    @Autowired
    private ExamSetService examSetService;

    @Autowired
    private StudentExamAssignmentRepo studentExamAssignmentRepo;

    @Autowired
    private ExamService examService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/exam-context")
    public ResponseEntity<?> getExamContext(HttpSession session) {
        try {
            Object userIdObj = session.getAttribute("userId");
            if (userIdObj == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(errorResponse("Not authenticated"));
            }

            String studentId = String.valueOf(userIdObj);
            Integer studentIdInt;
            try {
                studentIdInt = Integer.valueOf(studentId);
            } catch (NumberFormatException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(errorResponse("Invalid candidate session"));
            }

            Integer currentExamId = safeInteger(session.getAttribute("currentExamId"));

            Optional<StudentExamAssignment> assignmentOpt = Optional.empty();

            if (currentExamId != null) {
                assignmentOpt = studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, currentExamId);
                if (assignmentOpt.isPresent() && Boolean.TRUE.equals(assignmentOpt.get().getHasCompleted())) {
                    assignmentOpt = Optional.empty();
                }
            }

            if (assignmentOpt.isEmpty()) {
                assignmentOpt = studentExamAssignmentRepo
                        .findTopByStudentIdAndHasStartedTrueAndHasCompletedFalseOrderByAssignedAtDesc(studentId);
            }

            if (assignmentOpt.isEmpty()) {
                assignmentOpt = studentExamAssignmentRepo
                        .findTopByStudentIdAndHasCompletedFalseOrderByAssignedAtDesc(studentId);
            }

            Integer resolvedExamId = null;
            if (assignmentOpt.isPresent()) {
                resolvedExamId = assignmentOpt.get().getExamId();
            } else {
                Optional<User> userOpt = userRepository.findByIdWithExams(studentIdInt);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getAssignedExams() != null && !user.getAssignedExams().isEmpty()) {
                        if (currentExamId != null && user.getAssignedExams().stream()
                                .anyMatch(exam -> Objects.equals(exam.getId(), currentExamId))) {
                            resolvedExamId = currentExamId;
                        } else {
                            resolvedExamId = user.getAssignedExams().stream()
                                    .map(exam -> exam.getId())
                                    .filter(Objects::nonNull)
                                    .max(Integer::compareTo)
                                    .orElse(null);
                        }
                    }
                }

                if (resolvedExamId != null) {
                    assignmentOpt = studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, resolvedExamId);
                    if (assignmentOpt.isEmpty()) {
                        Map<String, Object> autoAssignResult = examSetService.autoAssignStudentToSet(
                                studentId,
                                resolvedExamId.longValue());
                        if (!Boolean.TRUE.equals(autoAssignResult.get("success"))) {
                            log.warn("Auto-assign failed for student {} exam {}: {}", studentId, resolvedExamId,
                                    autoAssignResult.get("error"));
                        }
                        assignmentOpt = studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, resolvedExamId);
                    }
                }
            }

            if (resolvedExamId == null && assignmentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorResponse("No exam assignment found for this candidate"));
            }

            Integer examId = resolvedExamId != null ? resolvedExamId : assignmentOpt.get().getExamId();
            session.setAttribute("currentExamId", examId);

            Map<String, Object> response = new HashMap<>();
            response.put("examId", examId);
            response.put("studentId", studentId);

            if (assignmentOpt.isPresent()) {
                StudentExamAssignment assignment = assignmentOpt.get();
                response.put("assignedSetNumber", assignment.getAssignedSetNumber());
                response.put("slotNumber", assignment.getSlotNumber());
                response.put("hasStarted", assignment.getHasStarted());
                response.put("hasCompleted", assignment.getHasCompleted());
            } else {
                response.put("assignedSetNumber", null);
                response.put("slotNumber", null);
                response.put("hasStarted", false);
                response.put("hasCompleted", false);
            }

            examService.findExam(examId).ifPresent(exam -> {
                response.put("examTitle", exam.getTitle());
                response.put("duration", exam.getDuration());
            });

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching exam context", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Failed to fetch exam context"));
        }
    }

    @GetMapping("/exams/{examId}/shuffled-questions")
    public ResponseEntity<?> getShuffledQuestions(
            @PathVariable String examId,
            HttpSession session) {
        
        try {
            
            Object userIdObj = session.getAttribute("userId");
            
            if (userIdObj == null) {
                log.error("❌ No user in session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Not authenticated. Please login first."));
            }
            
            String studentId = String.valueOf(userIdObj);
            Long examIdLong = Long.parseLong(examId);
            
            log.info(" Student ID: {}", studentId);
            log.info(" Exam ID: {}", examIdLong);
            
           
            Optional<StudentExamAssignment> assignment = examSetService.getStudentAssignment(studentId, examIdLong);

            if (assignment.isEmpty()) {
                log.info("No set assignment for student {} exam {}. Attempting auto-assignment.",
                        studentId, examIdLong);
                Map<String, Object> autoAssignResult = examSetService.autoAssignStudentToSet(studentId, examIdLong);
                if (!Boolean.TRUE.equals(autoAssignResult.get("success"))) {
                    log.warn("Auto-assignment failed for student {} exam {}: {}",
                            studentId, examIdLong, autoAssignResult.get("error"));
                }
                assignment = examSetService.getStudentAssignment(studentId, examIdLong);
            }
            
            if (assignment.isEmpty()) {
                log.error("❌ Student not assigned to exam {}", examIdLong);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("You are not assigned to this exam. Please contact administrator."));
            }
            
            log.info("✅ Student is assigned to Set {}", assignment.get().getAssignedSetNumber());
            
           
            try {
                examSetService.markExamStarted(studentId, examIdLong);
                log.info("✅ Marked exam as started");
            } catch (Exception e) {
                log.warn("⚠️ Could not mark exam as started: {}", e.getMessage());
            }
            
            
            List<Question> questions = examSetService.getShuffledQuestionsForStudent(studentId, examIdLong);
            
            if (questions == null || questions.isEmpty()) {
                log.error("❌ No questions returned from ExamSetService");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("No questions found for your assigned set. Please contact administrator."));
            }
            
            log.info("✅ Returning {} shuffled questions", questions.size());
            log.info("   First question ID: {}", questions.get(0).getId());
            log.info("   Last question ID: {}", questions.get(questions.size() - 1).getId());
           
            return ResponseEntity.ok(questions);
            
        } catch (NumberFormatException e) {
            log.error("❌ Invalid exam ID format: {}", examId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("Invalid exam ID format"));
                
        } catch (IllegalStateException e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ Error fetching shuffled questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("Failed to load questions: " + e.getMessage()));
        }
    }

    @GetMapping("/exams/{examId}/assignment")
    public ResponseEntity<?> getExamAssignment(
            @PathVariable String examId,
            HttpSession session) {
        
        try {
            Object userIdObj = session.getAttribute("userId");
            
            if (userIdObj == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse("Not authenticated"));
            }
            
            String studentId = String.valueOf(userIdObj);
            Long examIdLong = Long.parseLong(examId);
            
            Optional<StudentExamAssignment> assignment = examSetService.getStudentAssignment(studentId, examIdLong);
            
            if (assignment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse("Not assigned to this exam"));
            }
            
            StudentExamAssignment data = assignment.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("examId", data.getExamId());
            response.put("studentId", data.getStudentId());
            response.put("assignedSetNumber", data.getAssignedSetNumber());
            response.put("slotNumber", data.getSlotNumber());
            response.put("hasStarted", data.getHasStarted());
            response.put("hasCompleted", data.getHasCompleted());
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("Invalid exam ID format"));
        } catch (Exception e) {
            log.error("❌ Error fetching assignment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("Failed to fetch assignment"));
        }
    }

       @GetMapping("/exams/{examId}/can-access")
    public ResponseEntity<?> canAccessExam(
            @PathVariable String examId,
            HttpSession session) {
        
        try {
            Object userIdObj = session.getAttribute("userId");
            
            if (userIdObj == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("canAccess", false);
                response.put("reason", "Not authenticated");
                return ResponseEntity.ok(response);
            }
            
            String studentId = String.valueOf(userIdObj);
            Long examIdLong = Long.parseLong(examId);
            
            Optional<StudentExamAssignment> assignment = examSetService.getStudentAssignment(studentId, examIdLong);
            
            if (assignment.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("canAccess", false);
                response.put("reason", "Not assigned to this exam");
                return ResponseEntity.ok(response);
            }
            
            StudentExamAssignment data = assignment.get();
            
            if (data.getHasCompleted()) {
                Map<String, Object> response = new HashMap<>();
                response.put("canAccess", false);
                response.put("reason", "Exam already completed");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("canAccess", true);
            response.put("assignedSetNumber", data.getAssignedSetNumber());
            response.put("hasStarted", data.getHasStarted());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error checking access", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("canAccess", false);
            errorResponse.put("reason", "Error checking access: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private Integer safeInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
