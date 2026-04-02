package com.truerize.service;

import java.io.IOException;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.entity.StoredFile;
import com.truerize.repository.StoredFileRepository;

@Service
public class StoredFileService {

    @Autowired
    private StoredFileRepository storedFileRepository;

    public StoredFile storeFile(
            MultipartFile multipartFile,
            String category,
            Integer relatedUserId,
            Integer relatedExamId,
            Integer relatedSlotId) {

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        try {
            StoredFile storedFile = new StoredFile();
            storedFile.setOriginalFileName(resolveSafeFileName(multipartFile.getOriginalFilename()));
            storedFile.setContentType(multipartFile.getContentType());
            storedFile.setFileSize(multipartFile.getSize());
            storedFile.setFileCategory(category);
            storedFile.setRelatedUserId(relatedUserId);
            storedFile.setRelatedExamId(relatedExamId);
            storedFile.setRelatedSlotId(relatedSlotId);
            storedFile.setFileData(multipartFile.getBytes());
            return storedFileRepository.save(storedFile);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store uploaded file in database", ex);
        }
    }

    public StoredFile getFile(Long fileId) {
        return storedFileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("Stored file not found with id: " + fileId));
    }

    public String buildFileUrl(Long fileId) {
        return "/api/files/" + fileId;
    }

    private String resolveSafeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }

        String baseName = Paths.get(originalFilename).getFileName().toString();
        String cleaned = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "upload.bin" : cleaned;
    }
}
