package com.truerize.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.entity.Exam;
import com.truerize.entity.Proctoring;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.UserRepository;
import com.truerize.service.ProctoringService;

@RestController
@RequestMapping("/api/proctoring")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ProctoringController {
    
    private static final Logger log = LoggerFactory.getLogger(ProctoringController.class);
    
    // Update this path to match your system
    private static final String BASE_DIR = "uploads/proctoring/";
    
    @Autowired
    private ProctoringService proctoringService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExamRepository examRepository;

    
    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<?> saveProctoring(
            @RequestParam(value = "photo", required = true) MultipartFile photo,
            @RequestParam(value = "idProof", required = true) MultipartFile idProof,
            @RequestParam(value = "userId", required = true) Integer userId,
            @RequestParam(value = "examId", required = true) Integer examId,
            @RequestParam(value = "cameraEnabled", required = false, defaultValue = "true") boolean cameraEnabled,
            @RequestParam(value = "microphoneEnabled", required = false, defaultValue = "true") boolean microphoneEnabled,
            @RequestParam(value = "screenSharingEnabled", required = false, defaultValue = "false") boolean screenSharingEnabled) {
        
        log.info("========================================");
        log.info("📸 PROCTORING SAVE REQUEST RECEIVED");
        log.info("========================================");
        log.info("👤 User ID: {}", userId);
        log.info("📝 Exam ID: {}", examId);
        log.info("📁 Photo: {} ({} bytes)", photo.getOriginalFilename(), photo.getSize());
        log.info("📁 ID Proof: {} ({} bytes)", idProof.getOriginalFilename(), idProof.getSize());
        log.info("📊 Camera: {}, Mic: {}, Screen: {}", cameraEnabled, microphoneEnabled, screenSharingEnabled);
        
        try {
           
            if (photo.isEmpty()) {
                log.error("❌ Verification photo is empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Verification photo is required"));
            }
            
            if (idProof.isEmpty()) {
                log.error("❌ ID proof photo is empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "ID proof photo is required"));
            }
            
           
            log.info("🔍 Looking up user with ID: {}", userId);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ User not found with ID: {}", userId);
                    return new RuntimeException("User not found with ID: " + userId);
                });
            
            log.info("✅ User found: {} (Email: {})", user.getName(), user.getEmail());
            
            
            log.info("🔍 Looking up exam with ID: {}", examId);
            Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> {
                    log.error("❌ Exam not found with ID: {}", examId);
                    return new RuntimeException("Exam not found with ID: " + examId);
                });
            
            log.info("✅ Exam found: {} (Duration: {} min)", exam.getTitle(), exam.getDuration());
            
            
            File uploadDir = new File(BASE_DIR);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (created) {
                    log.info("✅ Created upload directory: {}", uploadDir.getAbsolutePath());
                } else {
                    log.warn("⚠️ Could not create upload directory: {}", uploadDir.getAbsolutePath());
                }
            }
            
         
            String timestamp = String.valueOf(System.currentTimeMillis());
            String userPrefix = "user" + userId + "_exam" + examId + "_";
            
            String photoFilename = userPrefix + timestamp + "_verification.jpg";
            Path photoPath = Paths.get(BASE_DIR + photoFilename);
            Files.copy(photo.getInputStream(), photoPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ Verification photo saved: {}", photoPath.toAbsolutePath());
            
          
            String idProofFilename = userPrefix + timestamp + "_idproof.jpg";
            Path idProofPath = Paths.get(BASE_DIR + idProofFilename);
            Files.copy(idProof.getInputStream(), idProofPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ ID proof photo saved: {}", idProofPath.toAbsolutePath());
            
          
            Proctoring proctoring = new Proctoring();
            proctoring.setUser(user);
            proctoring.setExam(exam);
            proctoring.setPhotoUrl(photoPath.toString());
            proctoring.setIdProofUrl(idProofPath.toString());
            proctoring.setCameraEnabled(cameraEnabled);
            proctoring.setMicrophoneEnabled(microphoneEnabled);
            proctoring.setScreenSharingEnabled(screenSharingEnabled);
            
           
            log.info("💾 Saving proctoring record to database...");
            Proctoring savedProctoring = proctoringService.saveProctoring(proctoring);
            log.info("✅ Proctoring record saved with ID: {}", savedProctoring.getId());
            
            log.info("========================================");
            log.info("✅ PROCTORING SETUP COMPLETED SUCCESSFULLY");
            log.info("========================================");
            
           
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Proctoring data saved successfully");
            response.put("proctoringId", savedProctoring.getId());
            response.put("photoFilename", photoFilename);
            response.put("idProofFilename", idProofFilename);
            response.put("userId", userId);
            response.put("examId", examId);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Runtime error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
                
        } catch (IOException e) {
            log.error("❌ IO error saving files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to save photos: " + e.getMessage()
                ));
                
        } catch (Exception e) {
            log.error("❌ Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred: " + e.getMessage()
                ));
        }
    }
    
   
    @PostMapping("/test")
    public ResponseEntity<?> test() {
        log.info("✅ Test endpoint called successfully");
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Proctoring controller is accessible"
        ));
    }
}