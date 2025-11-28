package com.truerize.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.Result;
import com.truerize.repository.ResultRepository;
import com.truerize.service.MailService;
import com.truerize.service.ResultService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);
    
   
    private static final int PASSING_SCORE = 20; 

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private ResultService resultService;

    /**
     * Get all results
     */
    @GetMapping("/results/all")
    public ResponseEntity<?> getAllResults() {
        try {
            log.info("📋 Fetching all results");
            List<Result> results = resultRepository.findAll();
            log.info("✅ Found {} results", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("❌ Error fetching results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch results"));
        }
    }

    @PutMapping("/results/{id}/release")
    public ResponseEntity<String> releaseResult(@PathVariable Long id) {
        try {
           
            
            Optional<Result> optionalResult = resultRepository.findById(id);
            
            if (optionalResult.isEmpty()) {
                log.warn("⚠️ Result not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Result not found with ID: " + id);
            }

            Result result = optionalResult.get();
            
            
            if ("Result Released".equalsIgnoreCase(result.getStatus())) {
                log.warn("⚠️ Result already released for: {}", result.getEmail());
                return ResponseEntity.ok("Result already released for " + result.getEmail());
            }
            
            
            boolean passed = result.getScore() >= PASSING_SCORE;
            String originalStatus = passed ? "Pass" : "Failed";
            
            log.info("📊 Score: {} | Passing Score: {} | Status: {}", 
                result.getScore(), PASSING_SCORE, originalStatus);
            
            
            result.setStatus("Result Released");
            resultRepository.save(result);
           
            try {
                if (passed) {
                    log.info("✅ PASS - Sending congratulatory email to: {}", result.getEmail());
                    mailService.sendResultReleasedEmail(result.getEmail());
                    return ResponseEntity.ok(
                        String.format("✅ Result released! Congratulations email sent to %s (Score: %d)", 
                            result.getEmail(), result.getScore())
                    );
                } else {
                    log.info("❌ FAIL - Sending regret email to: {}", result.getEmail());
                    mailService.sendRegretEmail(result.getEmail());
                    return ResponseEntity.ok(
                        String.format("✅ Result released! Regret email sent to %s (Score: %d)", 
                            result.getEmail(), result.getScore())
                    );
                }
            } catch (Exception emailError) {
                log.error("❌ Failed to send email to {}", result.getEmail(), emailError);
                return ResponseEntity.ok(
                    "Result released but email sending failed: " + emailError.getMessage()
                );
            }
            
        } catch (Exception e) {
            log.error("❌ Error releasing result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to release result: " + e.getMessage());
        }
    }

    @PutMapping("/results/send-mails")
    public ResponseEntity<String> sendAutomaticMails() {
        try {
           
            
            List<Result> results = resultRepository.findAll();
            int passed = 0, failed = 0, errors = 0, alreadyReleased = 0;

            for (Result result : results) {
                try {
                  
                    if ("Result Released".equalsIgnoreCase(result.getStatus())) {
                        alreadyReleased++;
                        continue;
                    }
                    
                    boolean didPass = result.getScore() >= PASSING_SCORE;
                    
                    if (didPass) {
                        log.info("✅ PASS - Sending congratulatory email to: {} (Score: {})", 
                            result.getEmail(), result.getScore());
                        mailService.sendResultReleasedEmail(result.getEmail());
                        passed++;
                    } else {
                        log.info("❌ FAIL - Sending regret email to: {} (Score: {})", 
                            result.getEmail(), result.getScore());
                        mailService.sendRegretEmail(result.getEmail());
                        failed++;
                    }
                    
 
                    result.setStatus("Result Released");
                    resultRepository.save(result);
                    
                } catch (Exception e) {
                    log.error("❌ Error sending mail to {}: {}", result.getEmail(), e.getMessage());
                    errors++;
                }
            }

            log.info("========================================");
            log.info("✅ EMAIL SENDING COMPLETE");
            log.info("   Passed (Score >= {}): {}", PASSING_SCORE, passed);
            log.info("   Failed (Score < {}): {}", PASSING_SCORE, failed);
            log.info("   Already Released: {}", alreadyReleased);
            log.info("   Errors: {}", errors);
            log.info("========================================");

            if (errors == results.size() && results.size() > 0) {
                return ResponseEntity.ok(
                    "⚠️ Email service not configured. Please check application.properties for SMTP settings."
                );
            }
            
            return ResponseEntity.ok(
                String.format("✅ Emails sent! Pass: %d, Fail: %d, Already Released: %d, Errors: %d", 
                    passed, failed, alreadyReleased, errors)
            );
            
        } catch (Exception e) {
            log.error("❌ Error in automatic email sending", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send emails: " + e.getMessage());
        }
    }

    @DeleteMapping("/results/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        try {
            
            Optional<Result> result = resultRepository.findById(id);
            
            if (result.isEmpty()) {
                log.warn("⚠️ Result not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Result not found with ID: " + id));
            }
            
            resultRepository.deleteById(id);
            
            log.info("✅ Result deleted successfully: {}", id);
          
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Deleted result with ID: " + id
            ));
            
        } catch (Exception e) {
            log.error("❌ Error deleting result {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete result: " + e.getMessage()));
        }
    }

    @PutMapping("/results/{id}")
    public ResponseEntity<?> updateResult(@PathVariable Long id, @RequestBody Result updatedResult) {
        try {
            log.info("📝 Updating result: {}", id);
            
            Optional<Result> existingResult = resultRepository.findById(id);
            
            if (existingResult.isEmpty()) {
                log.warn("⚠️ Result not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Result not found with ID: " + id));
            }
            
            Result result = existingResult.get();
            result.setName(updatedResult.getName());
            result.setEmail(updatedResult.getEmail());
            result.setCollegeName(updatedResult.getCollegeName());
            result.setScore(updatedResult.getScore());
            result.setStatus(updatedResult.getStatus());
            
            if (updatedResult.getExam() != null) {
                result.setExam(updatedResult.getExam());
            }
            
            resultRepository.save(result);
            
            log.info("✅ Result updated successfully: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Result updated successfully",
                "result", result
            ));
            
        } catch (Exception e) {
            log.error("❌ Error updating result {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update result: " + e.getMessage()));
        }
    }
}