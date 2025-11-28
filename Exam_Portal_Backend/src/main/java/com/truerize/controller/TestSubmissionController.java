package com.truerize.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.dto.TestSubmissionDTO;
import com.truerize.dto.TestSubmissionResponseDTO;
import com.truerize.entity.Exam;
import com.truerize.entity.TestSubmission;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.UserRepository;
import com.truerize.service.TestSubmissionService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/candidate")
public class TestSubmissionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private TestSubmissionService testSubmissionService;

    @PostMapping("/submit")
    public ResponseEntity<TestSubmissionResponseDTO> submitExam(
            @RequestBody TestSubmissionDTO dto,
            HttpSession session) {

        System.out.println("========== 📝 EXAM SUBMISSION REQUEST ==========");
        System.out.println("DTO: " + dto);

        try {
            
            Object sessionUserId = session.getAttribute("userId");
            if (sessionUserId == null) {
                System.out.println("❌ ERROR: Unauthorized - No session user ID");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            int userId;
            if (sessionUserId instanceof Integer) {
                userId = (Integer) sessionUserId;
            } else if (sessionUserId instanceof String) {
                userId = Integer.parseInt((String) sessionUserId);
            } else {
                userId = Integer.parseInt(sessionUserId.toString());
            }

            System.out.println("👤 Session User ID: " + userId);

            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            System.out.println("✅ User found: " + user.getName());

            int examId = dto.getExam().getId();
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

            System.out.println("✅ Exam found: " + exam.getTitle());

            
            String answersJson = dto.getAnswersJson();
            String videoUrl = dto.getVideoUrl();

            
            if (answersJson == null || answersJson.trim().isEmpty()) {
                answersJson = "{}";
            }

            System.out.println("📹 Video URL from DTO: " + videoUrl);
            System.out.println("📄 Answers JSON length: " + answersJson.length());

           
            TestSubmission submission = new TestSubmission();
            submission.setUser(user);
            submission.setExam(exam);
            submission.setAnswersJson(answersJson);
            submission.setVideoUrl(videoUrl); 
            submission.setStatus("Completed");
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setScore(0); 

            System.out.println("🔄 Saving submission to database...");

           
            TestSubmission savedSubmission = testSubmissionService.submitTest(submission);

            System.out.println("========== ✅ EXAM SUBMITTED SUCCESSFULLY ==========");
            System.out.println("Submission ID: " + savedSubmission.getId());
            System.out.println("Video URL Saved: " + savedSubmission.getVideoUrl());

            
            TestSubmissionResponseDTO responseDTO = new TestSubmissionResponseDTO();
            responseDTO.setSubmissionId(savedSubmission.getId());
            responseDTO.setStudentName(user.getName());
            responseDTO.setCollegeName(user.getCollegeName());
            responseDTO.setEmail(user.getEmail());
            responseDTO.setExamTitle(exam.getTitle());
            responseDTO.setScore(savedSubmission.getScore());
            responseDTO.setStatus(savedSubmission.getStatus());
            responseDTO.setSubmittedAt(savedSubmission.getSubmittedAt());
            responseDTO.setVideoUrl(savedSubmission.getVideoUrl());

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to submit exam");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
