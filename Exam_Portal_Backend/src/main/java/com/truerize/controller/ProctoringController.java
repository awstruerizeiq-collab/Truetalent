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
    
    private static final String BASE_DIR = "C:\\Users\\Dell\\Desktop\\truerize-exam-portal\\truerize-exam-portal\\uploads\\";
    @Autowired
    private ProctoringService proctoringService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExamRepository examRepository;

    /**
     * Save proctoring data with photos
     * POST /api/proctoring/save
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveProctoring(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("idProof") MultipartFile idProof,
            @RequestParam("userId") Integer userId,
            @RequestParam("examId") Integer examId,
            @RequestParam("cameraEnabled") boolean cameraEnabled,
            @RequestParam("microphoneEnabled") boolean microphoneEnabled,
            @RequestParam("screenSharingEnabled") boolean screenSharingEnabled) {
        
        log.info("========================================");
        log.info("📸 SAVING PROCTORING DATA");
        log.info("========================================");
        log.info("👤 User ID: {}", userId);
        log.info("📝 Exam ID: {}", examId);
        log.info("📁 Verification Photo: {}", photo.getOriginalFilename());
        log.info("📁 ID Proof Photo: {}", idProof.getOriginalFilename());
        log.info("📊 Photo size: {} bytes", photo.getSize());
        log.info("📊 ID Proof size: {} bytes", idProof.getSize());
        
        try {
            // Verify User exists
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            // Verify Exam exists
            Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));
            
            log.info("✅ User found: {}", user.getId());
            log.info("✅ Exam found: {}", exam.getId());
            
            // Create upload directory if it doesn't exist
            File uploadDir = new File(BASE_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                log.info("✅ Created upload directory: {}", BASE_DIR);
            }
            
            // Generate timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // Save verification photo with format: timestamp + "verification" + "verification.jpg"
            String photoFilename = timestamp + "verificationverification.jpg";
            Path photoPath = Paths.get(BASE_DIR + photoFilename);
            Files.copy(photo.getInputStream(), photoPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ Verification photo saved: {}", photoPath.toAbsolutePath());
            
            // Save ID proof photo with format: timestamp + "idproof" + "idproof.jpg"
            String idProofFilename = timestamp + "idproofidproof.jpg";
            Path idProofPath = Paths.get(BASE_DIR + idProofFilename);
            Files.copy(idProof.getInputStream(), idProofPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ ID proof photo saved: {}", idProofPath.toAbsolutePath());
            
            // Create Proctoring entity
            Proctoring proctoring = new Proctoring();
            proctoring.setUser(user);
            proctoring.setExam(exam);
            proctoring.setPhotoUrl(photoPath.toString());  // Full absolute path
            proctoring.setIdProofUrl(idProofPath.toString());  // Full absolute path
            proctoring.setCameraEnabled(cameraEnabled);
            proctoring.setMicrophoneEnabled(microphoneEnabled);
            proctoring.setScreenSharingEnabled(true); // Always set to true by default
            
            // Save to database
            Proctoring savedProctoring = proctoringService.saveProctoring(proctoring);
            log.info("✅ Proctoring data saved to database with ID: {}", savedProctoring.getId());
            
            log.info("========================================");
            log.info("✅ PROCTORING SETUP COMPLETED SUCCESSFULLY");
            log.info("========================================");
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Proctoring data saved successfully");
            response.put("proctoringId", savedProctoring.getId());
            response.put("photoFilename", photoFilename);
            response.put("idProofFilename", idProofFilename);
            response.put("photoPath", photoPath.toString());
            response.put("idProofPath", idProofPath.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
                
        } catch (IOException e) {
            log.error("❌ Error saving files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save photos: " + e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}