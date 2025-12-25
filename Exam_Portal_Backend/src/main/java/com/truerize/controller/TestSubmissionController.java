package com.truerize.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TestSubmissionController {

    private static final Logger log = LoggerFactory.getLogger(TestSubmissionController.class);

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

        log.info("========== 📝 EXAM SUBMISSION REQUEST ==========");
        log.info("DTO: {}", dto);

        try {
            // Get userId from session
            Object sessionUserId = session.getAttribute("userId");
            if (sessionUserId == null) {
                log.error("❌ ERROR: Unauthorized - No session user ID");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // Convert session userId to Integer
            Integer userId;
            if (sessionUserId instanceof Integer) {
                userId = (Integer) sessionUserId;
            } else if (sessionUserId instanceof String) {
                userId = Integer.parseInt((String) sessionUserId);
            } else {
                userId = Integer.parseInt(sessionUserId.toString());
            }

            log.info("👤 Session User ID: {}", userId);

            // Find user by Integer ID
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            log.info("✅ User found: {}", user.getName());

            // Get exam
            int examId = dto.getExam().getId();
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

            log.info("✅ Exam found: {}", exam.getTitle());

            // Get answers and video URL
            String answersJson = dto.getAnswersJson();
            String videoUrl = dto.getVideoUrl();

            // Handle empty answers
            if (answersJson == null || answersJson.trim().isEmpty()) {
                answersJson = "{}";
            }

            log.info("📹 Video URL from DTO: {}", videoUrl);
            log.info("📄 Answers JSON length: {}", answersJson.length());

            // Create TestSubmission entity
            TestSubmission submission = new TestSubmission();
            submission.setUser(user);
            submission.setExam(exam);
            submission.setAnswersJson(answersJson);
            submission.setVideoUrl(videoUrl); 
            submission.setStatus("Completed");
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setScore(0); // Will be calculated in service

            log.info("🔄 Saving submission to database...");

         
            TestSubmission savedSubmission = testSubmissionService.submitTest(submission);

            log.info("========== ✅ EXAM SUBMITTED SUCCESSFULLY ==========");
            log.info("Submission ID: {}", savedSubmission.getId());
            log.info("Video URL Saved: {}", savedSubmission.getVideoUrl());
            log.info("Score: {}", savedSubmission.getScore());

           
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

        } catch (NumberFormatException e) {
            log.error("❌ ERROR: Invalid user ID format", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            
        } catch (RuntimeException e) {
            log.error("❌ ERROR: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            
        } catch (Exception e) {
            log.error("❌ ERROR: Failed to submit exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}