package com.truerize.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.truerize.entity.Exam;
import com.truerize.service.ExamService;

@RestController
@RequestMapping("/api/admin/exams")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExamController {
    
    private static final Logger log = LoggerFactory.getLogger(ExamController.class);
    
    @Autowired
    private ExamService examService;
    
   
    @GetMapping
    public ResponseEntity<?> getAllExams() {
        try {
            List<Exam> exams = examService.findAllExams();
            log.info("✅ Retrieved {} exams", exams.size());
            
            if (exams.isEmpty()) {
                log.warn("⚠️ No exams found in database");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "No exams found. Please create an exam first.",
                    "exams", exams
                ));
            }
            
            return ResponseEntity.ok(exams);
        } catch (Exception e) {
            log.error("❌ Error fetching all exams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to fetch exams",
                    "message", e.getMessage()
                ));
        }
    }
    
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getExam(@PathVariable int id) {
        try {
            Optional<Exam> exam = examService.findExam(id);
            
            if (exam.isPresent()) {
                log.info("Retrieved exam with id: {}", id);
                return ResponseEntity.ok(exam.get());
            } else {
                log.warn("Exam not found with id: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Exam not found with id: " + id));
            }
        } catch (Exception e) {
            log.error("Error fetching exam with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createExam(@RequestBody Exam exam) {
        try {
            // Validate required fields
            if (exam.getTitle() == null || exam.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Exam title is required"));
            }
            
            // Set exam reference for all questions
            if (exam.getQuestions() != null) {
                exam.getQuestions().forEach(question -> question.setExam(exam));
            }
            
            Exam newExam = examService.createExam(exam);
            log.info("✅ Created new exam with id: {} - Title: {}", newExam.getId(), newExam.getTitle());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Exam created successfully",
                "exam", newExam
            ));
        } catch (Exception e) {
            log.error("❌ Error creating exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create exam: " + e.getMessage()));
        }
    }
    
   
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExam(@PathVariable int id, @RequestBody Exam examDetails) {
        try {
            Exam updatedExam = examService.updateExam(id, examDetails);
            log.info("Updated exam with id: {}", id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Exam updated successfully",
                "exam", updatedExam
            ));
        } catch (RuntimeException e) {
            log.error("Error updating exam with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Exam not found with id: " + id));
        } catch (Exception e) {
            log.error("Unexpected error updating exam with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update exam"));
        }
    }
    
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable int id) {
        try {
            examService.deleteExam(id);
            log.info("Deleted exam with id: {}", id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Exam deleted successfully",
                "examId", id
            ));
        } catch (RuntimeException e) {
            log.error("Error deleting exam with id: {}", id, e);
            String message = e.getMessage() != null ? e.getMessage() : "Failed to delete exam";
            HttpStatus status = message.contains("Exam not found with id:")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                .body(Map.of("error", message));
        } catch (Exception e) {
            log.error("Unexpected error deleting exam with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete exam"));
        }
    }
}
