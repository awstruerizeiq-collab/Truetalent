package com.truerize.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.entity.Result;
import com.truerize.entity.Slot;
import com.truerize.entity.TestSubmission;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.ResultRepository;
import com.truerize.repository.SlotRepository;
import com.truerize.repository.TestSubmissionRepository;
import com.truerize.repository.UserRepository;

@Service
public class TestSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(TestSubmissionService.class);
    private static final int PASSING_SCORE = 20;

    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ResultRepository resultRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private SlotRepository slotRepository;

    public TestSubmission submitTest(TestSubmission submission) {
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("Completed");

        int calculatedScore = calculateScore(submission);
        submission.setScore(calculatedScore);

        TestSubmission savedSubmission = testSubmissionRepository.save(submission);
       
        User user = userRepository.findById(savedSubmission.getUser().getId())
                                  .orElseThrow(() -> new RuntimeException("User not found"));
                                  
        Exam exam = examRepository.findById(savedSubmission.getExam().getId())
                                  .orElseThrow(() -> new RuntimeException("Exam not found"));
      
        // 🔥 CRITICAL: Get the user's slot
        Slot userSlot = user.getSlot();
        
        if (userSlot == null) {
            log.error("❌ User {} has no slot assigned!", user.getId());
            throw new RuntimeException("User must have a slot assigned");
        }
        
        log.info("✅ User slot found: Slot #{} (ID: {})", userSlot.getSlotNumber(), userSlot.getId());
        
        // Create Result entity with slot
        Result result = new Result();
        result.setName(user.getName());
        result.setCollegeName(user.getCollegeName());
        result.setEmail(user.getEmail());
        result.setExam(exam.getTitle());
        result.setScore(savedSubmission.getScore());
        result.setSlot(userSlot); // 🔥 LINK RESULT TO SLOT
        
        // Set status based on score
        if (savedSubmission.getScore() >= PASSING_SCORE) {
            result.setStatus("Passed");
        } else {
            result.setStatus("Failed");
        }

        log.info("💾 Saving result: user={}, college={}, email={}, exam={}, score={}, slot={}",
                user.getName(), user.getCollegeName(), user.getEmail(), 
                exam.getTitle(), savedSubmission.getScore(), userSlot.getSlotNumber());

        resultRepository.save(result);
        
        log.info("✅ Result saved successfully with Slot #{}", userSlot.getSlotNumber());

        return savedSubmission;
    }
        
    public int calculateScore(TestSubmission submission) {
        int totalScore = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, String> candidateAnswers = mapper.readValue(
                submission.getAnswersJson(),
                new TypeReference<Map<String, String>>() {}
            );

            List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(
                submission.getExam().getId()
            );

            for (Question q : questions) {
                String givenAnswer = candidateAnswers.get(String.valueOf(q.getId()));
                String correctAnswer = q.getAnswer();

                if (givenAnswer == null || givenAnswer.trim().isEmpty()) continue;

                switch (q.getType().toLowerCase()) {
                    case "mcq":
                    case "coding":
                        if (correctAnswer != null && givenAnswer.trim().equalsIgnoreCase(correctAnswer.trim())) {
                            totalScore += q.getMarks();
                        }
                        break;

                    case "verbal":
                        if (!givenAnswer.equalsIgnoreCase("audio_file_not_recorded")
                                && !givenAnswer.equalsIgnoreCase("blank_answer")) {
                            totalScore += q.getMarks();
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("❌ Error calculating score: {}", e.getMessage());
        }

        return totalScore;
    }

    public List<TestSubmission> getAllSubmissions() {
        return testSubmissionRepository.findAll();
    }

    public Optional<TestSubmission> getSubmissionById(int id) {
        return testSubmissionRepository.findById(id);
    }

    public List<TestSubmission> getSubmissionsByUser(int userId) {
        return testSubmissionRepository.findByUserId(userId);
    }

    public List<TestSubmission> getSubmissionsByExam(int examId) {
        return testSubmissionRepository.findByExamId(examId);
    }

    public void deleteSubmission(int id) {
        testSubmissionRepository.deleteById(id);
    }
}