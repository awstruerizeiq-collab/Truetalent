package com.truerize.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.Result;
import com.truerize.entity.Slot;
import com.truerize.repository.ResultRepository;
import com.truerize.repository.SlotRepository;
import com.truerize.service.MailService;
import com.truerize.service.ResultService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);

    private static final int PASSING_SCORE = 80;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private ResultService resultService;

    @GetMapping("/results/all")
    public ResponseEntity<List<Result>> getAllResults(
            @RequestParam(required = false) Integer slotId,
            @RequestParam(required = false) Integer slotNumber) {

        log.info("📋 Fetching results - slotId: {}, slotNumber: {}", slotId, slotNumber);

        try {
            List<Result> results;

            if (slotId != null) {
                results = resultRepository.findBySlotId(slotId);
                log.info("✅ Found {} results for slot ID: {}", results.size(), slotId);
            } else if (slotNumber != null) {
                results = resultRepository.findBySlotNumber(slotNumber);
                log.info("✅ Found {} results for slot number: {}", results.size(), slotNumber);
            } else {
                results = resultRepository.findAllWithSlotOrderedBySlotAndScore();
                log.info("✅ Found {} total results", results.size());
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ Error fetching results", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of());
        }
    }

    @GetMapping("/results/by-slots")
    public ResponseEntity<?> getResultsBySlots() {
        try {
            log.info("📊 Fetching results grouped by slots");

            List<Slot> allSlots = slotRepository.findAllOrderedBySlotNumber();
            log.info("📊 Found {} slots in database", allSlots.size());

            List<Map<String, Object>> slotResults = new ArrayList<>();

            for (Slot slot : allSlots) {
                Map<String, Object> slotData = new HashMap<>();

                slotData.put("slotId", slot.getId());
                slotData.put("slotNumber", slot.getSlotNumber());
                slotData.put("collegeName", slot.getCollegeName() != null ? slot.getCollegeName() : "");

                slotData.put("date", slot.getDate() != null ? slot.getDate().toString() : "");
                slotData.put("time", slot.getTime() != null ? slot.getTime().toString() : "");

                List<Result> results = resultRepository.findBySlotId(slot.getId());
                log.info("   Slot {} (ID: {}): {} results",
                        slot.getSlotNumber(), slot.getId(), results.size());

                slotData.put("totalCandidates", results.size());

                if (!results.isEmpty()) {

                    double avgScore = results.stream()
                            .mapToInt(Result::getScore)
                            .average()
                            .orElse(0.0);

                    int maxScore = results.stream()
                            .mapToInt(Result::getScore)
                            .max()
                            .orElse(0);

                    int minScore = results.stream()
                            .mapToInt(Result::getScore)
                            .min()
                            .orElse(0);

                    long passed = results.stream()
                            .filter(r -> r.getScore() >= PASSING_SCORE)
                            .count();
                    long failed = results.size() - passed;

                    double passPercentage = (passed * 100.0 / results.size());

                    slotData.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
                    slotData.put("maxScore", maxScore);
                    slotData.put("minScore", minScore);
                    slotData.put("passedCount", passed);
                    slotData.put("failedCount", failed);
                    slotData.put("passPercentage", Math.round(passPercentage * 100.0) / 100.0);
                } else {

                    slotData.put("averageScore", 0.0);
                    slotData.put("maxScore", 0);
                    slotData.put("minScore", 0);
                    slotData.put("passedCount", 0);
                    slotData.put("failedCount", 0);
                    slotData.put("passPercentage", 0.0);
                }

                slotResults.add(slotData);
            }

            log.info("✅ Successfully grouped results for {} slots", slotResults.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalSlots", slotResults.size());
            response.put("slots", slotResults);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching slot-wise results", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch slot-wise results: " + e.getMessage());
            errorResponse.put("slots", new ArrayList<>());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/results/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            log.info("📊 Fetching overall statistics");

            List<Result> allResults = resultRepository.findAll();
            long totalResults = allResults.size();

            if (totalResults == 0) {
                Map<String, Object> emptyStats = new HashMap<>();
                emptyStats.put("totalResults", 0);
                emptyStats.put("totalPassed", 0);
                emptyStats.put("totalFailed", 0);
                emptyStats.put("passPercentage", 0.0);
                emptyStats.put("averageScore", 0.0);
                emptyStats.put("maxScore", 0);
                emptyStats.put("minScore", 0);
                emptyStats.put("passingScore", PASSING_SCORE);
                emptyStats.put("resultsReleased", 0);
                emptyStats.put("resultsUnreleased", 0);
                emptyStats.put("totalSlots", slotRepository.count());
                emptyStats.put("message", "No results available");
                return ResponseEntity.ok(emptyStats);
            }

            long passed = allResults.stream()
                    .filter(r -> r.getScore() >= PASSING_SCORE)
                    .count();
            long failed = totalResults - passed;

            double avgScore = allResults.stream()
                    .mapToInt(Result::getScore)
                    .average()
                    .orElse(0.0);

            int maxScore = allResults.stream()
                    .mapToInt(Result::getScore)
                    .max()
                    .orElse(0);

            int minScore = allResults.stream()
                    .mapToInt(Result::getScore)
                    .min()
                    .orElse(0);

            long releasedCount = allResults.stream()
                    .filter(r -> "Result Released".equalsIgnoreCase(r.getStatus()))
                    .count();

            // Slot statistics
            long totalSlots = slotRepository.count();
            Map<Integer, Long> resultsPerSlot = allResults.stream()
                    .filter(r -> r.getSlot() != null)
                    .collect(Collectors.groupingBy(
                            r -> r.getSlot().getSlotNumber(),
                            Collectors.counting()));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalResults", totalResults);
            stats.put("totalPassed", passed);
            stats.put("totalFailed", failed);
            stats.put("passPercentage", Math.round((passed * 100.0 / totalResults) * 100.0) / 100.0);
            stats.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
            stats.put("maxScore", maxScore);
            stats.put("minScore", minScore);
            stats.put("passingScore", PASSING_SCORE);
            stats.put("resultsReleased", releasedCount);
            stats.put("resultsUnreleased", totalResults - releasedCount);
            stats.put("totalSlots", totalSlots);
            stats.put("resultsPerSlot", resultsPerSlot);

            log.info("✅ Statistics generated: {} total results, {} slots", totalResults, totalSlots);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Error fetching statistics", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<?> getResultById(@PathVariable Long id) {
        try {
            Optional<Result> result = resultRepository.findById(id);

            if (result.isPresent()) {
                return ResponseEntity.ok(result.get());
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Result not found with ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorResponse);
            }
        } catch (Exception e) {
            log.error("❌ Error fetching result {}", id, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch result");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
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
                                    result.getEmail(), result.getScore()));
                } else {
                    log.info("❌ FAIL - Sending regret email to: {}", result.getEmail());
                    mailService.sendRegretEmail(result.getEmail());
                    return ResponseEntity.ok(
                            String.format("✅ Result released! Regret email sent to %s (Score: %d)",
                                    result.getEmail(), result.getScore()));
                }
            } catch (Exception emailError) {
                log.error("❌ Failed to send email to {}", result.getEmail(), emailError);
                return ResponseEntity.ok(
                        "Result released but email sending failed: " + emailError.getMessage());
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
                        "⚠️ Email service not configured. Please check application.properties for SMTP settings.");
            }

            return ResponseEntity.ok(
                    String.format("✅ Emails sent! Pass: %d, Fail: %d, Already Released: %d, Errors: %d",
                            passed, failed, alreadyReleased, errors));

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
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Result not found with ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorResponse);
            }

            resultRepository.deleteById(id);

            log.info("✅ Result deleted successfully: {}", id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deleted result with ID: " + id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error deleting result {}", id, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @PutMapping("/results/{id}")
    public ResponseEntity<?> updateResult(@PathVariable Long id, @RequestBody Result updatedResult) {
        try {
            log.info("📝 Updating result: {}", id);

            Optional<Result> existingResult = resultRepository.findById(id);

            if (existingResult.isEmpty()) {
                log.warn("⚠️ Result not found: {}", id);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Result not found with ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorResponse);
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

            // Update slot if provided
            if (updatedResult.getSlot() != null && updatedResult.getSlot().getId() != null) {
                Optional<Slot> slot = slotRepository.findById(updatedResult.getSlot().getId());
                slot.ifPresent(result::setSlot);
            }

            resultRepository.save(result);

            log.info("✅ Result updated successfully: {}", id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Result updated successfully");
            response.put("result", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error updating result {}", id, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update result: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}