package com.truerize.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Result;
import com.truerize.repository.ResultRepository;

@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);
    
    private static final int PASSING_SCORE = 20;
    
    private final ResultRepository resultRepository;
    
    @Autowired
    private MailService mailService;

    public ResultService(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    
    public List<Result> getAllResults() {
        return resultRepository.findAll();
    }

    public Optional<Result> getResultById(Long id) {
        return resultRepository.findById(id);
    }

   
    @Transactional
    public Result saveResult(Result result) {
        log.info("💾 Saving result for: {} (Score: {})", result.getEmail(), result.getScore());
        
        if (result.getScore() >= PASSING_SCORE) {
            result.setStatus("Pass");
            log.info("✅ Status set to: Pass (Score: {} >= {})", result.getScore(), PASSING_SCORE);
        } else {
            result.setStatus("Failed");
            log.info("❌ Status set to: Failed (Score: {} < {})", result.getScore(), PASSING_SCORE);
        }
        
        Result savedResult = resultRepository.save(result);
        log.info("✅ Result saved with status: {}", savedResult.getStatus());
        
        return savedResult;
    }

    @Transactional
    public String updateResult(Long id, Result updatedResult) {
        log.info("📝 Updating result with ID: {}", id);
        
        return resultRepository.findById(id).map(result -> {
            result.setName(updatedResult.getName());
            result.setEmail(updatedResult.getEmail());
            result.setScore(updatedResult.getScore());
            
            if (result.getScore() != updatedResult.getScore()) {
                if (updatedResult.getScore() >= PASSING_SCORE) {
                    result.setStatus("Pass");
                    log.info("✅ Score updated - New status: Pass");
                } else {
                    result.setStatus("Failed");
                    log.info("❌ Score updated - New status: Failed");
                }
            } else {
                
                result.setStatus(updatedResult.getStatus());
            }
            
            resultRepository.save(result);
            log.info("✅ Result updated successfully for: {}", result.getEmail());
            
            return "Result updated successfully!";
        }).orElse("Result not found with ID: " + id);
    }

    @Transactional
    public void deleteResult(Long id) {
        log.info("🗑️ Deleting result with ID: {}", id);
        
        if (!resultRepository.existsById(id)) {
            throw new IllegalArgumentException("Result not found with ID: " + id);
        }
        
        resultRepository.deleteById(id);
        log.info("✅ Result deleted successfully");
    }

    @Transactional
    public String releaseResult(Long id) {
        log.info("📤 Releasing result with ID: {}", id);
        
        Optional<Result> resultOpt = resultRepository.findById(id);
        
        if (resultOpt.isEmpty()) {
            return "Result not found with ID: " + id;
        }
        
        Result result = resultOpt.get();
        
       
        if ("Result Released".equalsIgnoreCase(result.getStatus())) {
            log.warn("⚠️ Result already released for: {}", result.getEmail());
            return "Result already released for " + result.getEmail();
        }
        
        String originalStatus = result.getStatus();
        if (!"Pass".equalsIgnoreCase(originalStatus) && !"Failed".equalsIgnoreCase(originalStatus)) {
            if (result.getScore() >= PASSING_SCORE) {
                originalStatus = "Pass";
            } else {
                originalStatus = "Failed";
            }
        }
        
     
        result.setStatus("Result Released");
        resultRepository.save(result);
        
        try {
            if ("Pass".equalsIgnoreCase(originalStatus)) {
                log.info("📧 Sending congratulatory email to: {} (Score: {})", 
                    result.getEmail(), result.getScore());
                mailService.sendResultReleasedEmail(result.getEmail());
                return String.format("✅ Result released! Congratulations email sent to %s (Score: %d)", 
                    result.getEmail(), result.getScore());
            } else {
                log.info("📧 Sending regret email to: {} (Score: {})", 
                    result.getEmail(), result.getScore());
                mailService.sendRegretEmail(result.getEmail());
                return String.format("✅ Result released! Regret email sent to %s (Score: %d)", 
                    result.getEmail(), result.getScore());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send email for result release: {}", e.getMessage());
            return "Result released but email sending failed: " + e.getMessage();
        }
    }

    @Transactional
    public String sendMailsAutomatically() {
        log.info("📧 Starting automatic mail sending for all unreleased results");
        
        int passCount = 0;
        int failCount = 0;
        int errorCount = 0;
        int alreadyReleased = 0;
        
        List<Result> allResults = resultRepository.findAll();
        
        for (Result result : allResults) {
            if ("Result Released".equalsIgnoreCase(result.getStatus())) {
                alreadyReleased++;
                continue;
            }
            
            try {
              
                boolean passed = result.getScore() >= PASSING_SCORE;
                
               
                result.setStatus("Result Released");
                resultRepository.save(result);
                
              
                if (passed) {
                    mailService.sendResultReleasedEmail(result.getEmail());
                    passCount++;
                    log.info("✅ Congratulations email sent to: {} (Score: {})", 
                        result.getEmail(), result.getScore());
                } else {
                    mailService.sendRegretEmail(result.getEmail());
                    failCount++;
                    log.info("✅ Regret email sent to: {} (Score: {})", 
                        result.getEmail(), result.getScore());
                }
            } catch (Exception e) {
                errorCount++;
                log.error("❌ Failed to send email to {}: {}", result.getEmail(), e.getMessage());
            }
        }
        
        String message = String.format(
            "✅ Automatic mail sending completed! " +
            "Pass emails (Score >= %d): %d, Fail emails (Score < %d): %d, " +
            "Already Released: %d, Errors: %d",
            PASSING_SCORE, passCount, PASSING_SCORE, failCount, alreadyReleased, errorCount
        );
        
        log.info(message);
        return message;
    }
    public int getPassingScore() {
        return PASSING_SCORE;
    }
}