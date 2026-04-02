package com.truerize.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.entity.StoredFile;
import com.truerize.service.StoredFileService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/candidate")
public class VideoUploadController {

    @Autowired
    private StoredFileService storedFileService;

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

            StoredFile storedVideo = storedFileService.storeFile(
                videoFile,
                "EXAM_VIDEO",
                parseInteger(studentId),
                parseInteger(examId),
                null);
            String videoUrl = storedFileService.buildFileUrl(storedVideo.getId());

            response.put("success", true);
            response.put("message", "Video uploaded successfully");
            response.put("videoUrl", videoUrl);
            response.put("fileId", storedVideo.getId());
            response.put("fileSize", videoFile.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to upload video: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
