package com.truerize.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.repository.TestSubmissionRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/candidate")
public class VideoUploadController {

    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    
    @Value("${file.upload-dir:uploads/videos}")
    private String uploadDir;

    @PostMapping("/upload-video")
    public ResponseEntity<Map<String, Object>> uploadVideo(
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("examId") String examId,
            @RequestParam("studentId") String studentId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
           
            Object sessionUserId = session.getAttribute("userId");
            if (sessionUserId == null) {
                response.put("success", false);
                response.put("message", "Unauthorized access");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            
            if (videoFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Video file is empty");
                return ResponseEntity.badRequest().body(response);
            }

            
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = videoFile.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".webm";
            
            String uniqueFilename = String.format("exam_%s_student_%s_%s%s", 
                examId, 
                studentId, 
                UUID.randomUUID().toString(), 
                fileExtension);

           
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(videoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String videoUrl = "/uploads/videos/" + uniqueFilename;

            response.put("success", true);
            response.put("message", "Video uploaded successfully");
            response.put("videoUrl", videoUrl);
            response.put("filePath", filePath.toString());
            response.put("fileSize", videoFile.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Failed to upload video: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}