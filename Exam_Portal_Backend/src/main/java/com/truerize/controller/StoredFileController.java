package com.truerize.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.StoredFile;
import com.truerize.service.StoredFileService;

@RestController
@RequestMapping("/api/files")
public class StoredFileController {

    @Autowired
    private StoredFileService storedFileService;

    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> getStoredFile(@PathVariable Long fileId) {
        StoredFile storedFile = storedFileService.getFile(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(storedFile.getContentType()));
        headers.setContentLength(storedFile.getFileData().length);
        headers.setContentDisposition(ContentDisposition.inline()
            .filename(storedFile.getOriginalFileName(), StandardCharsets.UTF_8)
            .build());

        return new ResponseEntity<>(storedFile.getFileData(), headers, HttpStatus.OK);
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
