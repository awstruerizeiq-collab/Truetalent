package com.truerize.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.ResultRepository;
import com.truerize.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ResultRepository resultRepository;

    @GetMapping("/stats")
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalExams", examRepository.count());
        stats.put("totalQuestions", questionRepository.count());

        
        stats.put("examsCompleted", resultRepository.count());

        return stats;
    }
}