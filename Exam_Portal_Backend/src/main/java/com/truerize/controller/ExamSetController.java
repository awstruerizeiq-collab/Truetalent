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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.service.ExamSetService;

@RestController
@RequestMapping("/api/examset")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExamSetController {

    private static final Logger log = LoggerFactory.getLogger(ExamSetController.class);

    @Autowired
    private ExamSetService examSetService;

    @PostMapping("/{examId}/generate")
    public ResponseEntity<?> generateQuestionSets(@PathVariable Long examId) {
        
        log.info("========================================");
        log.info("🎯 GENERATE QUESTION SETS REQUEST");
        log.info("   Exam ID: {}", examId);
        log.info("========================================");
        
        try {
            if (examId == null || examId <= 0) {
                log.error("❌ Invalid exam ID: {}", examId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false, 
                        "error", "Invalid exam ID: " + examId
                    ));
            }
            
            Map<String, Object> result = examSetService.generateQuestionSets(examId);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                log.info("✅ SUCCESS: Question sets generated");
                return ResponseEntity.ok(result);
            } else {
                log.error("❌ FAILED: {}", result.get("error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (IllegalStateException e) {
            log.error("❌ Business logic error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "examId", examId
                ));
            
        } catch (Exception e) {
            log.error("❌ Unexpected error generating question sets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to generate question sets: " + e.getMessage(),
                    "examId", examId
                ));
        }
    }

    @PostMapping("/auto-assign")
    public ResponseEntity<?> autoAssignStudent(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Long examId = Long.parseLong(request.get("examId").toString());
            
            log.info("🎯 Auto-assign request: User {} to Exam {}", userId, examId);
            
            Map<String, Object> result = examSetService.autoAssignStudentToSet(userId, examId);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            log.error("❌ Error in auto-assignment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Auto-assignment failed: " + e.getMessage()));
        }
    }

   
    @GetMapping("/{examId}/sets")
    public ResponseEntity<?> getQuestionSets(@PathVariable Long examId) {
        try {
            log.info("📋 Fetching question sets for exam: {}", examId);
            
            List<Map<String, Object>> sets = examSetService.getQuestionSetsForExam(examId);
            
            return ResponseEntity.ok(sets);
        } catch (Exception e) {
            log.error("❌ Error fetching question sets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch question sets: " + e.getMessage()));
        }
    }

    @GetMapping("/assignment")
    public ResponseEntity<?> getAssignment(
            @RequestParam String userId,
            @RequestParam Long examId) {
        try {
            log.info("📋 Fetching assignment for user {} in exam {}", userId, examId);
            
            var assignment = examSetService.getStudentAssignment(userId, examId);
            
            if (assignment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No assignment found for student " + userId + " in exam " + examId));
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
        } catch (Exception e) {
            log.error("❌ Error fetching assignment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch assignment: " + e.getMessage()));
        }
    }

   
    @PostMapping("/assign")
    public ResponseEntity<?> assignStudent(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Long examId = Long.parseLong(request.get("examId").toString());
            Integer setNumber = Integer.parseInt(request.get("setNumber").toString());
            Integer slotNumber = Integer.parseInt(request.get("slotNumber").toString());
            
            log.info("📋 Manual assignment: Student {} to Exam {} Set {}", userId, examId, setNumber);
            
            Map<String, Object> result = examSetService.assignStudentToSet(userId, examId, setNumber, slotNumber);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error assigning student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to assign student: " + e.getMessage()));
        }
    }

   
    @GetMapping("/{examId}/stats")
    public ResponseEntity<?> getExamStats(@PathVariable Long examId) {
        try {
            Map<String, Object> stats = examSetService.getExamStatistics(examId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Error fetching stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch statistics: " + e.getMessage()));
        }
    }

    
    @PostMapping("/{examId}/delete-sets")
    public ResponseEntity<?> deleteQuestionSetsPost(@PathVariable Long examId) {
        try {
            log.info("🗑️ Deleting question sets for exam: {}", examId);
            
            Map<String, Object> result = examSetService.deleteQuestionSetsForExam(examId);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            log.error("❌ Error deleting question sets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to delete question sets: " + e.getMessage()));
        }
    }

   
    @DeleteMapping("/{examId}")
    public ResponseEntity<?> deleteQuestionSets(@PathVariable Long examId) {
        try {
            log.info("🗑️ Deleting question sets for exam: {}", examId);
            
            Map<String, Object> result = examSetService.deleteQuestionSetsForExam(examId);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            log.error("❌ Error deleting question sets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to delete question sets: " + e.getMessage()));
        }
    }
    
    
    @GetMapping("/{examId}/set/{setNumber}/debug")
    public ResponseEntity<?> debugSetDetails(
            @PathVariable Long examId,
            @PathVariable Integer setNumber) {
        try {
            log.info("🔍 Debug request for Exam {} Set {}", examId, setNumber);
            
            Map<String, Object> debug = examSetService.getSetDetailsWithQuestions(examId, setNumber);
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("❌ Error in debug endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", e.getMessage(),
                    "examId", examId,
                    "setNumber", setNumber
                ));
        }
    }
    
    
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ExamSetService",
            "timestamp", System.currentTimeMillis()
        ));
    }
}