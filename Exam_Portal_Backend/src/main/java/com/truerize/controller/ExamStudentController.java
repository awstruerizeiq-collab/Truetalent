package com.truerize.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.truerize.service.ExamSetService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/candidate")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExamStudentController {

    private static final Logger log = LoggerFactory.getLogger(ExamStudentController.class);

    @Autowired
    private ExamSetService examSetService;

    @GetMapping("/exams/{examId}/shuffled-questions")
    public ResponseEntity<?> getShuffledQuestions(
            @PathVariable String examId,
            HttpSession session) {
        
        try {
            
            Object userIdObj = session.getAttribute("userId");
            
            if (userIdObj == null) {
                log.error("❌ No user in session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated. Please login first."));
            }
            
            String studentId = String.valueOf(userIdObj);
            Long examIdLong = Long.parseLong(examId);
            
            log.info(" Student ID: {}", studentId);
            log.info(" Exam ID: {}", examIdLong);
            
           
            var assignment = examSetService.getStudentAssignment(studentId, examIdLong);
            
            if (assignment.isEmpty()) {
                log.error("❌ Student not assigned to exam {}", examIdLong);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not assigned to this exam. Please contact administrator."));
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
                    .body(Map.of("error", "No questions found for your assigned set. Please contact administrator."));
            }
            
            log.info("✅ Returning {} shuffled questions", questions.size());
            log.info("   First question ID: {}", questions.get(0).getId());
            log.info("   Last question ID: {}", questions.get(questions.size() - 1).getId());
           
            return ResponseEntity.ok(questions);
            
        } catch (NumberFormatException e) {
            log.error("❌ Invalid exam ID format: {}", examId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid exam ID format"));
                
        } catch (IllegalStateException e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ Error fetching shuffled questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to load questions: " + e.getMessage()));
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
                    .body(Map.of("error", "Not authenticated"));
            }
            
            String studentId = String.valueOf(userIdObj);
            Long examIdLong = Long.parseLong(examId);
            
            var assignment = examSetService.getStudentAssignment(studentId, examIdLong);
            
            if (assignment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not assigned to this exam"));
            }
            
            var data = assignment.get();
            
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
                .body(Map.of("error", "Invalid exam ID format"));
        } catch (Exception e) {
            log.error("❌ Error fetching assignment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch assignment"));
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
            
            var assignment = examSetService.getStudentAssignment(studentId, examIdLong);
            
            if (assignment.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("canAccess", false);
                response.put("reason", "Not assigned to this exam");
                return ResponseEntity.ok(response);
            }
            
            var data = assignment.get();
            
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
}