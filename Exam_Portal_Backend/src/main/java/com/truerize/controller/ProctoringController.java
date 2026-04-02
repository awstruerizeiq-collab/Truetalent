package com.truerize.controller;

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
import com.truerize.entity.StoredFile;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.UserRepository;
import com.truerize.service.ProctoringService;
import com.truerize.service.StoredFileService;

@RestController
@RequestMapping("/api/proctoring")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ProctoringController {
    
    private static final Logger log = LoggerFactory.getLogger(ProctoringController.class);
    
    @Autowired
    private ProctoringService proctoringService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StoredFileService storedFileService;

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
        log.info("PROCTORING SAVE REQUEST RECEIVED");
        log.info("========================================");
        log.info("User ID: {}", userId);
        log.info("Exam ID: {}", examId);
        log.info("Photo: {} ({} bytes)", photo.getOriginalFilename(), photo.getSize());
        log.info("ID Proof: {} ({} bytes)", idProof.getOriginalFilename(), idProof.getSize());
        log.info("Camera: {}, Mic: {}, Screen: {}", cameraEnabled, microphoneEnabled, screenSharingEnabled);
        
        try {
            if (photo.isEmpty()) {
                log.error("Verification photo is empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Verification photo is required"));
            }
            
            if (idProof.isEmpty()) {
                log.error("ID proof photo is empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "ID proof photo is required"));
            }
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

            StoredFile storedPhoto = storedFileService.storeFile(
                photo,
                "PROCTORING_PHOTO",
                userId,
                examId,
                null);
            StoredFile storedIdProof = storedFileService.storeFile(
                idProof,
                "PROCTORING_ID_PROOF",
                userId,
                examId,
                null);

            String photoUrl = storedFileService.buildFileUrl(storedPhoto.getId());
            String idProofUrl = storedFileService.buildFileUrl(storedIdProof.getId());

            Proctoring proctoring = new Proctoring();
            proctoring.setUser(user);
            proctoring.setExam(exam);
            proctoring.setPhotoUrl(photoUrl);
            proctoring.setIdProofUrl(idProofUrl);
            proctoring.setCameraEnabled(cameraEnabled);
            proctoring.setMicrophoneEnabled(microphoneEnabled);
            proctoring.setScreenSharingEnabled(screenSharingEnabled);

            Proctoring savedProctoring = proctoringService.saveProctoring(proctoring);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Proctoring data saved successfully");
            response.put("proctoringId", savedProctoring.getId());
            response.put("photoFileId", storedPhoto.getId());
            response.put("idProofFileId", storedIdProof.getId());
            response.put("photoUrl", photoUrl);
            response.put("idProofUrl", idProofUrl);
            response.put("userId", userId);
            response.put("examId", examId);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Runtime error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
                
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred: " + e.getMessage()
                ));
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> test() {
        log.info("Proctoring test endpoint called successfully");
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Proctoring controller is accessible"
        ));
    }
}
