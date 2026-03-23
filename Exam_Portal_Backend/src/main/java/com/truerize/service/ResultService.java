package com.truerize.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.entity.Result;
import com.truerize.entity.Slot;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.ResultRepository;

@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);
    private static final int DEFAULT_PASSING_SCORE = 80;

    private final ResultRepository resultRepository;
    private final MailService mailService;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;

    public ResultService(
            ResultRepository resultRepository,
            MailService mailService,
            ExamRepository examRepository,
            QuestionRepository questionRepository) {
        this.resultRepository = resultRepository;
        this.mailService = mailService;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
    }

    public List<Result> getAllResults() {
        return resultRepository.findAll();
    }

    public Optional<Result> getResultById(Long id) {
        return resultRepository.findById(id);
    }

    public Result saveResult(Result result) {
        log.info("Saving result for email={} score={}", result.getEmail(), result.getScore());

        if (result.getSlot() == null || result.getSlot().getId() == null) {
            throw new IllegalStateException("Slot is mandatory while saving result");
        }

        if (result.getScore() == null) {
            throw new IllegalStateException("Score cannot be null");
        }

        result.setScorePercentage(calculateScorePercentage(result));
        result.setStatus(evaluatePassFailStatus(result));
        Result saved = resultRepository.save(result);

        log.info("Result saved | id={} | slot={} | status={}",
            saved.getId(),
            saved.getSlot() != null ? saved.getSlot().getSlotNumber() : null,
            saved.getStatus());

        return saved;
    }

    @Transactional
    public String updateResult(Long id, Result updatedResult) {
        log.info("Updating result id={}", id);

        Result result = resultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Result not found: " + id));

        result.setName(updatedResult.getName());
        result.setEmail(updatedResult.getEmail());
        result.setCollegeName(updatedResult.getCollegeName());
        result.setExam(updatedResult.getExam());
        result.setScore(updatedResult.getScore());
        result.setScorePercentage(calculateScorePercentage(result));
        result.setStatus(evaluatePassFailStatus(result));

        resultRepository.save(result);

        log.info("Result updated successfully for {}", result.getEmail());
        return "Result updated successfully";
    }

    @Transactional
    public void deleteResult(Long id) {
        log.info("Deleting result id={}", id);

        if (!resultRepository.existsById(id)) {
            throw new IllegalArgumentException("Result not found: " + id);
        }

        resultRepository.deleteById(id);
        log.info("Result deleted successfully");
    }

    @Transactional
    public String releaseResult(Long id) {
        Result result = resultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Result not found: " + id));

        if ("Result Released".equalsIgnoreCase(result.getStatus())) {
            return "Result already released for " + result.getEmail();
        }

        boolean passed = isPass(result);
        result.setStatus("Result Released");
        resultRepository.save(result);

        try {
            if (passed) {
                mailService.sendResultReleasedEmail(result.getEmail());
                return "Result released and PASS email sent to " + result.getEmail();
            } else {
                mailService.sendRegretEmail(result.getEmail());
                return "Result released and FAIL email sent to " + result.getEmail();
            }
        } catch (Exception e) {
            log.error("Email sending failed", e);
            return "Result released but email sending failed";
        }
    }

    @Transactional
    public String sendMailsAutomatically() {
        log.info("Auto mail sending started");

        int passed = 0;
        int failed = 0;
        int alreadyReleased = 0;
        int errors = 0;

        List<Result> results = resultRepository.findAll();

        for (Result result : results) {
            if ("Result Released".equalsIgnoreCase(result.getStatus())) {
                alreadyReleased++;
                continue;
            }

            try {
                boolean isPassed = isPass(result);

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
                log.error("Failed sending mail to {}", result.getEmail(), e);
            }
        }

        return String.format(
            "Mail summary -> Passed: %d, Failed: %d, Already Released: %d, Errors: %d",
            passed, failed, alreadyReleased, errors
        );
    }

    public int getPassingScore() {
        return DEFAULT_PASSING_SCORE;
    }

    public int resolvePassingScore(Slot slot) {
        if (slot == null || slot.getPassPercentage() == null) {
            return DEFAULT_PASSING_SCORE;
        }
        return slot.getPassPercentage();
    }

    public int resolvePassingScore(Result result) {
        if (result == null) {
            return DEFAULT_PASSING_SCORE;
        }
        return resolvePassingScore(result.getSlot());
    }

    public boolean isPass(Result result) {
        if (result == null || result.getScore() == null) {
            return false;
        }
        Double scorePercentage = calculateScorePercentage(result);
        if (scorePercentage == null) {
            return result.getScore() >= resolvePassingScore(result);
        }
        return scorePercentage >= resolvePassingScore(result);
    }

    public String evaluatePassFailStatus(Result result) {
        if (result == null) {
            return "Fail";
        }
        Double scorePercentage = calculateScorePercentage(result);
        if (scorePercentage == null) {
            return evaluatePassFailStatus(result.getScore(), result.getSlot());
        }
        return evaluatePassFailStatus(scorePercentage, result.getSlot());
    }

    public String evaluatePassFailStatus(Integer score, Slot slot) {
        if (score == null) {
            return "Fail";
        }
        return score >= resolvePassingScore(slot) ? "Pass" : "Fail";
    }

    public String evaluatePassFailStatus(Double scorePercentage, Slot slot) {
        if (scorePercentage == null) {
            return "Fail";
        }
        return scorePercentage >= resolvePassingScore(slot) ? "Pass" : "Fail";
    }

    public void enrichResultsWithScorePercentage(List<Result> results) {
        if (results == null) {
            return;
        }
        for (Result result : results) {
            result.setScorePercentage(calculateScorePercentage(result));
        }
    }

    private Double calculateScorePercentage(Result result) {
        Integer totalMarks = resolveTotalMarks(result);
        if (totalMarks == null || totalMarks <= 0 || result == null || result.getScore() == null) {
            return null;
        }
        double percentage = (result.getScore() * 100.0) / totalMarks;
        return Math.round(percentage * 100.0) / 100.0;
    }

    private Integer resolveTotalMarks(Result result) {
        if (result == null) {
            return null;
        }

        Optional<Exam> examOpt = Optional.empty();
        if (result.getExamId() != null) {
            examOpt = examRepository.findByIdWithQuestions(result.getExamId());
        } else if (result.getExam() != null && !result.getExam().trim().isEmpty()) {
            examOpt = examRepository.findByTitle(result.getExam().trim());
        }

        if (examOpt.isEmpty()) {
            return null;
        }

        Exam exam = examOpt.get();
        if (exam.getTotalMarks() != null && exam.getTotalMarks() > 0) {
            return exam.getTotalMarks();
        }

        List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(exam.getId());
        if (questions == null || questions.isEmpty()) {
            return null;
        }

        return questions.stream()
                .mapToInt(q -> q.getMarks() == null ? 0 : q.getMarks())
                .sum();
    }
}
