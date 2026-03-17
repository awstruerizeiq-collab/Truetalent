package com.truerize.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.entity.Question;
import com.truerize.service.QuestionService;

@RestController
@RequestMapping("/api/admin/exams/{examId}/questions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class QuestionController {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);
    
    @Autowired
    private QuestionService questionService;
   
    @GetMapping
    public ResponseEntity<?> getQuestionsForExam(@PathVariable int examId) {
        try {
            log.info("📝 GET request: Fetching questions for exam {}", examId);
            List<Question> questions = questionService.getQuestionsByExamId(examId);
            
            log.info("✅ Retrieved {} questions for exam {}", questions.size(), examId);
            return ResponseEntity.ok(questions);
            
        } catch (Exception e) {
            log.error("❌ Error fetching questions for exam {}", examId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to fetch questions",
                    "message", e.getMessage()
                ));
        }
    }
   
    @PostMapping
    public ResponseEntity<?> addQuestionToExam(
            @PathVariable int examId, 
            @RequestBody Question question) {
        try {
            log.info("➕ POST request: Adding question to exam {}", examId);
            log.info("   Question type: {}, section: {}", question.getType(), question.getSection());
            
            Question newQuestion = questionService.addQuestion(examId, question);
            
            log.info("✅ Added question {} to exam {}", newQuestion.getId(), examId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "success", true,
                    "message", "Question added successfully",
                    "question", newQuestion
                ));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error adding question to exam {}: {}", examId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", "Validation error",
                    "message", e.getMessage()
                ));
                
        } catch (Exception e) {
            log.error("❌ Error adding question to exam {}", examId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to add question",
                    "message", e.getMessage()
                ));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadQuestionsFile(
            @PathVariable int examId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "section", required = false) String section) {
        try {
            Map<String, Object> result = questionService.importQuestionsFromFile(examId, file, section);
            int failedCount = ((Number) result.getOrDefault("failedCount", 0)).intValue();
            HttpStatus status = failedCount > 0 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", "Validation error",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            log.error("Error uploading questions for exam {}", examId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to upload questions",
                    "message", e.getMessage()
                ));
        }
    }
    
    
    @PutMapping("/{questionId}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable int examId,
            @PathVariable int questionId,
            @RequestBody Question questionDetails) {
        try {
            log.info("✏️ PUT request: Updating question {} in exam {}", questionId, examId);
            
            Question updatedQuestion = questionService.updateQuestion(questionId, questionDetails);
            
            log.info("✅ Updated question {} in exam {}", questionId, examId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Question updated successfully",
                "question", updatedQuestion
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error updating question {}: {}", questionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", "Validation error",
                    "message", e.getMessage()
                ));
                
        } catch (RuntimeException e) {
            log.error("❌ Question not found: {}", questionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "success", false,
                    "error", "Question not found",
                    "message", e.getMessage()
                ));
                
        } catch (Exception e) {
            log.error("❌ Error updating question {}", questionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to update question",
                    "message", e.getMessage()
                ));
        }
    }
    
   
    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> deleteQuestion(
            @PathVariable int examId, 
            @PathVariable int questionId) {
        try {
            log.info("🗑️ DELETE request: Deleting question {} from exam {}", questionId, examId);
            
            questionService.deleteQuestion(questionId);
            
            log.info("✅ Deleted question {} from exam {}", questionId, examId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Question deleted successfully",
                "questionId", questionId
            ));
            
        } catch (RuntimeException e) {
            log.error("❌ Question not found: {}", questionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "success", false,
                    "error", "Question not found",
                    "message", e.getMessage()
                ));
                
        } catch (Exception e) {
            log.error("❌ Error deleting question {}", questionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to delete question",
                    "message", e.getMessage()
                ));
        }
    }
}
