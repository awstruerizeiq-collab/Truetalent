package com.truerize.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;

@Service
public class QuestionService {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamRepository examRepository;
   
    public List<Question> getQuestionsByExamId(int examId) {
        log.info("📝 Fetching questions for exam id: {}", examId);
        List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
        log.info("✅ Found {} questions", questions.size());
        return questions;
    }
    
   
    public List<Question> getQuestionsByExamIdForStudent(int examId) {
        log.info("👨‍🎓 Fetching questions for student - exam id: {}", examId);
        
        List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
        
       
        questions.forEach(q -> {
            String type = q.getType().toLowerCase();
            
            if (type.equals("mcq")) {
                
                if (q.getAnswer() != null && q.getAnswer().contains(",")) {
                    q.setType("multiple");
                } else {
                    q.setType("single");
                }
            } 
            else if (type.equals("verbal")) {
                q.setType("read-speak");
            } 
            else if (type.equals("coding")) {
                q.setType("coding");
            }
        });
        
        log.info("✅ Processed {} questions for student view", questions.size());
        return questions;
    }
    
    
    @Transactional
    public Question addQuestion(int examId, Question question) {
        log.info("➕ Adding question to exam id: {}", examId);
        
        try {
           
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId + " in 'exam' table"));
            
            
            question.setExam(exam);
            
             validateQuestion(question);
            
          
            if (question.getqNo() == null || question.getqNo() <= 0) {
                
                List<Question> existingQuestions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
                int maxQNo = existingQuestions.stream()
                    .mapToInt(Question::getqNo)
                    .max()
                    .orElse(0);
                question.setqNo(maxQNo + 1);
            }
            
            
            if (question.getMarks() == null || question.getMarks() <= 0) {
                question.setMarks(1);
            }
            
            Question savedQuestion = questionRepository.save(question);
            log.info("✅ Added question with id: {} to exam: {}", savedQuestion.getId(), exam.getTitle());
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("❌ Error adding question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add question: " + e.getMessage(), e);
        }
    }
    
    
    @Transactional
    public Question updateQuestion(int questionId, Question questionDetails) {
        log.info("✏️ Updating question with id: {}", questionId);
        
        try {
            Question existingQuestion = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));
            
            
            if (questionDetails.getQuestionText() != null) {
                existingQuestion.setQuestionText(questionDetails.getQuestionText());
            }
            
            if (questionDetails.getMarks() != null) {
                existingQuestion.setMarks(questionDetails.getMarks());
            }
            
            if (questionDetails.getAnswer() != null) {
                existingQuestion.setAnswer(questionDetails.getAnswer());
            }
            
            if (questionDetails.getSection() != null) {
                existingQuestion.setSection(questionDetails.getSection());
            }
            
            if (questionDetails.getType() != null) {
                existingQuestion.setType(questionDetails.getType());
            }
            
            if (questionDetails.getOptions() != null) {
                existingQuestion.setOptions(questionDetails.getOptions());
            }
            
            if (questionDetails.getqNo() != null) {
                existingQuestion.setqNo(questionDetails.getqNo());
            }
            
          
            validateQuestion(existingQuestion);
            
            Question savedQuestion = questionRepository.save(existingQuestion);
            log.info("✅ Updated question id: {}", savedQuestion.getId());
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("❌ Error updating question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update question: " + e.getMessage(), e);
        }
    }
   
    @Transactional
    public void deleteQuestion(int questionId) {
        log.info("🗑️ Deleting question with id: {}", questionId);
        
        try {
            if (!questionRepository.existsById(questionId)) {
                throw new RuntimeException("Question not found with id: " + questionId);
            }
            
            questionRepository.deleteById(questionId);
            log.info("✅ Deleted question id: {}", questionId);
            
        } catch (Exception e) {
            log.error("❌ Error deleting question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete question: " + e.getMessage(), e);
        }
    }
    
  
    private void validateQuestion(Question question) {
        if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
            throw new IllegalArgumentException("Question text is required");
        }
        
        if (question.getSection() == null || question.getSection().trim().isEmpty()) {
            throw new IllegalArgumentException("Section is required");
        }
        
        if (question.getType() == null || question.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
        }
        
        
        if (question.getType().equalsIgnoreCase("MCQ")) {
            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                throw new IllegalArgumentException("MCQ questions must have options");
            }
            if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
                throw new IllegalArgumentException("MCQ questions must have an answer");
            }
        }
    }
}