package com.truerize.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.ResultRepository;
import com.truerize.repository.UserRepository;
import com.truerize.entity.User;

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

        List<User> users = userRepository.findAllWithExamsAndRoles();
        long nonAdminUsers = users.stream()
            .filter(user -> user.getRoles() == null || user.getRoles().stream()
                .noneMatch(role -> "ADMIN".equalsIgnoreCase(role.getName())))
            .count();

        stats.put("totalUsers", nonAdminUsers);
        stats.put("totalExams", examRepository.count());
        stats.put("totalQuestions", questionRepository.count());

        
        stats.put("examsCompleted", resultRepository.count());

        return stats;
    }
}
