package com.truerize.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Result;
import com.truerize.repository.ResultRepository;

@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);

    private static final int PASSING_SCORE = 80;

    private final ResultRepository resultRepository;
    private final MailService mailService;

    public ResultService(ResultRepository resultRepository, MailService mailService) {
        this.resultRepository = resultRepository;
        this.mailService = mailService;
    }

    public List<Result> getAllResults() {
        return resultRepository.findAll();
    }

    public Optional<Result> getResultById(Long id) {
        return resultRepository.findById(id);
    }

    public Result saveResult(Result result) {

        log.info("💾 Saving result for email={} score={}", 
                 result.getEmail(), result.getScore());

       
        if (result.getSlot() == null || result.getSlot().getId() == null) {
            throw new IllegalStateException("❌ Slot is mandatory while saving result");
        }

        if (result.getScore() == null) {
            throw new IllegalStateException("❌ Score cannot be null");
        }

        if (result.getScore() >= PASSING_SCORE) {
            result.setStatus("Passed");
        } else {
            result.setStatus("Failed");
        }

        Result saved = resultRepository.save(result);

        log.info("✅ Result saved | id={} | slot={} | status={}",
                 saved.getId(),
                 saved.getSlot().getSlotNumber(),
                 saved.getStatus());

        return saved;
    }

    
    @Transactional
    public String updateResult(Long id, Result updatedResult) {

        log.info("📝 Updating result id={}", id);

        Result result = resultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Result not found: " + id));

        result.setName(updatedResult.getName());
        result.setEmail(updatedResult.getEmail());
        result.setCollegeName(updatedResult.getCollegeName());
        result.setExam(updatedResult.getExam());
        result.setScore(updatedResult.getScore());

       
        if (updatedResult.getScore() >= PASSING_SCORE) {
            result.setStatus("Passed");
        } else {
            result.setStatus("Failed");
        }

        resultRepository.save(result);

        log.info("✅ Result updated successfully for {}", result.getEmail());
        return "Result updated successfully";
    }

   
    @Transactional
    public void deleteResult(Long id) {

        log.info("🗑️ Deleting result id={}", id);

        if (!resultRepository.existsById(id)) {
            throw new IllegalArgumentException("Result not found: " + id);
        }

        resultRepository.deleteById(id);
        log.info("✅ Result deleted successfully");
    }

   
    @Transactional
    public String releaseResult(Long id) {

        Result result = resultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Result not found: " + id));

        if ("Result Released".equalsIgnoreCase(result.getStatus())) {
            return "Result already released for " + result.getEmail();
        }

        boolean passed = result.getScore() >= PASSING_SCORE;

        result.setStatus("Result Released");
        resultRepository.save(result);

        try {
            if (passed) {
                mailService.sendResultReleasedEmail(result.getEmail());
                return "✅ Result released & PASS email sent to " + result.getEmail();
            } else {
                mailService.sendRegretEmail(result.getEmail());
                return "✅ Result released & FAIL email sent to " + result.getEmail();
            }
        } catch (Exception e) {
            log.error("❌ Email sending failed", e);
            return "Result released but email sending failed";
        }
    }

   
    @Transactional
    public String sendMailsAutomatically() {

        log.info("📧 Auto mail sending started");

        int passed = 0, failed = 0, alreadyReleased = 0, errors = 0;

        List<Result> results = resultRepository.findAll();

        for (Result result : results) {

            if ("Result Released".equalsIgnoreCase(result.getStatus())) {
                alreadyReleased++;
                continue;
            }

            try {
                boolean isPassed = result.getScore() >= PASSING_SCORE;

                result.setStatus("Result Released");
                resultRepository.save(result);

                if (isPassed) {
                    mailService.sendResultReleasedEmail(result.getEmail());
                    passed++;
                } else {
                    mailService.sendRegretEmail(result.getEmail());
                    failed++;
                }
            } catch (Exception e) {
                errors++;
                log.error("❌ Failed sending mail to {}", result.getEmail(), e);
            }
        }

        return String.format(
            "✅ Mail summary → Passed: %d, Failed: %d, Already Released: %d, Errors: %d",
            passed, failed, alreadyReleased, errors
        );
    }

    public int getPassingScore() {
        return PASSING_SCORE;
    }
}
