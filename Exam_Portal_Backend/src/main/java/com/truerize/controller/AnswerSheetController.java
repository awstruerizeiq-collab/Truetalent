package com.truerize.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.dto.AnswerSheetResponseDTO;
import com.truerize.exception.ResourceNotFoundException;
import com.truerize.service.AnswerSheetService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin/answersheet")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AnswerSheetController {

    private final AnswerSheetService answerSheetService;

    public AnswerSheetController(AnswerSheetService answerSheetService) {
        this.answerSheetService = answerSheetService;
    }

    @GetMapping("/{candidateId}/{examId}")
    public ResponseEntity<?> getAnswerSheet(
            @PathVariable Integer candidateId,
            @PathVariable Integer examId,
            @RequestParam(required = false) Integer slotId,
            HttpSession session) {

        ResponseEntity<Map<String, String>> authError = validateAdminSession(session);
        if (authError != null) {
            return authError;
        }

        if (candidateId == null || candidateId <= 0 || examId == null || examId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "candidateId and examId must be valid positive numbers"));
        }

        try {
            AnswerSheetResponseDTO response = answerSheetService.getAnswerSheet(candidateId, examId, slotId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch answer sheet"));
        }
    }

    @GetMapping("/download/{candidateId}/{examId}")
    public ResponseEntity<?> downloadAnswerSheet(
            @PathVariable Integer candidateId,
            @PathVariable Integer examId,
            @RequestParam(required = false) Integer slotId,
            HttpSession session) {

        ResponseEntity<Map<String, String>> authError = validateAdminSession(session);
        if (authError != null) {
            return authError;
        }

        if (candidateId == null || candidateId <= 0 || examId == null || examId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "candidateId and examId must be valid positive numbers"));
        }

        try {
            AnswerSheetResponseDTO sheet = answerSheetService.getAnswerSheet(candidateId, examId, slotId);
            byte[] fileData = answerSheetService.generateAnswerSheetExcel(candidateId, examId, slotId);

            String safeName = (sheet.getCandidateName() == null ? "candidate" : sheet.getCandidateName())
                    .replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = "answer_sheet_" + safeName + "_exam_" + examId + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build());

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download answer sheet"));
        }
    }

    private ResponseEntity<Map<String, String>> validateAdminSession(HttpSession session) {
        Object role = session.getAttribute("role");
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        if (!"ADMIN".equalsIgnoreCase(String.valueOf(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }
        return null;
    }
}
