package com.truerize.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.Question;
import com.truerize.service.QuestionService;

@RestController
@RequestMapping("/api/admin/exams/{examId}/questions")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    @GetMapping
    public List<Question> getQuestionsForExam(@PathVariable int examId) {
        return questionService.getQuestionsByExamId(examId);
    }
    
    @PostMapping
    public ResponseEntity<Question> addQuestionToExam(@PathVariable int examId, @RequestBody Question question) {
        Question newQuestion = questionService.addQuestion(examId, question);
        return ResponseEntity.ok(newQuestion);
    }
    
    @PutMapping("/{questionId}")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable int examId,
            @PathVariable int questionId,
            @RequestBody Question questionDetails) {
        Question updatedQuestion = questionService.updateQuestion(questionId, questionDetails);
        return ResponseEntity.ok(updatedQuestion);
    }
    
    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable int examId, @PathVariable int questionId) {
        questionService.deleteQuestion(questionId);
        return ResponseEntity.ok("Question deleted successfully.");
    }
}
