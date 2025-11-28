package com.truerize.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.entity.Result;
import com.truerize.entity.TestSubmission;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.ResultRepository;
import com.truerize.repository.TestSubmissionRepository;
import com.truerize.repository.UserRepository;

@Service
public class TestSubmissionService {

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
      
        Result result = new Result();
        result.setName(user.getName());
        result.setCollegeName(user.getCollegeName());
        result.setEmail(user.getEmail());
        result.setExam(exam.getTitle());
        result.setScore(savedSubmission.getScore());
        result.setStatus("Completed");

        System.out.println("Saving result: user=" + user.getName() +
                ", college=" + user.getCollegeName() +
                ", email=" + user.getEmail() +
                ", exam=" + exam.getTitle() +
                ", score=" + savedSubmission.getScore());

        resultRepository.save(result);

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
                        // Award marks if any valid audio response is provided
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
            System.out.println("Error calculating score: " + e.getMessage());
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