package com.truerize.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;

@Service
public class QuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    public List<Question> getQuestionsByExamId(int examId) {
        return questionRepository.findByExam_IdOrderByQNoAsc(examId);
    }
    
    public List<Question> getQuestionsByExamIdForStudent(int examId) {
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
        return questions;
    }
    
    public Question addQuestion(int examId, Question question) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));
        question.setExam(exam);
        return questionRepository.save(question);
    }
    
    public Question updateQuestion(int questionId, Question questionDetails) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));
        
        existingQuestion.setQuestionText(questionDetails.getQuestionText());
        existingQuestion.setMarks(questionDetails.getMarks());
        existingQuestion.setAnswer(questionDetails.getAnswer());
        existingQuestion.setSection(questionDetails.getSection());
        existingQuestion.setType(questionDetails.getType());
        existingQuestion.setOptions(questionDetails.getOptions());
        
        return questionRepository.save(existingQuestion);
    }
    
    public void deleteQuestion(int questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new RuntimeException("Question not found with id: " + questionId);
        }
        questionRepository.deleteById(questionId);
    }
}