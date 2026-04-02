package com.truerize.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.config.FixedAdminCredentials;
import com.truerize.entity.Exam;
import com.truerize.entity.Role;
import com.truerize.entity.Slot;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.ProctoringRepository;
import com.truerize.repository.ResultRepository;
import com.truerize.repository.RoleRepository;
import com.truerize.repository.SlotRepository;
import com.truerize.repository.TestSubmissionRepository;
import com.truerize.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_EXTRACT_PATTERN =
        Pattern.compile("([A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+)");
    private static final DateTimeFormatter UPLOAD_TS_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    @Autowired
    private ProctoringRepository proctoringRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @Value("${file.user-upload-dir:uploads/users}")
    private String userUploadDir;

    @Transactional
    public Map<String, Object> assignExamToUsers(List<Integer> userIds, Integer examId) {
        log.info("🎯 Assigning exam {} to {} users", examId, userIds.size());
        
        Map<String, Object> response = new HashMap<>();
        List<String> successUsers = new ArrayList<>();
        List<String> failedUsers = new ArrayList<>();
        
        try {
            log.info("🔍 Step 1: Verifying exam exists with ID: {}", examId);
            
            if (!examRepository.existsById(examId)) {
                log.error("❌ Exam not found with id: {}", examId);
                response.put("success", false);
                response.put("error", "Exam not found");
                response.put("message", "The selected exam (ID: " + examId + ") does not exist. Please create the exam first before assigning it.");
                return response;
            }
           
            Optional<Exam> examOpt = examRepository.findById(examId);
            if (examOpt.isEmpty()) {
                log.error("❌ Failed to load exam with id: {}", examId);
                response.put("success", false);
                response.put("error", "Failed to load exam");
                response.put("message", "Exam exists but could not be loaded. Please contact support.");
                return response;
            }
            
            Exam exam = examOpt.get();
            log.info("✅ Found exam: '{}' (ID: {})", exam.getTitle(), exam.getId());
            
            String examLink = "http://localhost:3000/login?examId=" + examId;
            
            log.info("👥 Step 2: Processing {} users", userIds.size());
            
            for (Integer userId : userIds) {
                try {
                    log.info("   Processing user ID: {}", userId);
                    
                    Optional<User> userOpt = userRepository.findByIdWithExams(userId);
                    
                    if (userOpt.isEmpty()) {
                        log.warn("⚠️ User not found: {}", userId);
                        failedUsers.add("User ID " + userId + " not found");
                        continue;
                    }
                    
                    User user = userOpt.get();
                    log.info("   Found user: {}", user.getEmail());
                    
                    if (user.getAssignedExams() == null) {
                        user.setAssignedExams(new HashSet<>());
                    }
                    
                    boolean alreadyAssigned = user.getAssignedExams().stream()
                        .anyMatch(e -> e.getId() == examId);
                    
                    if (alreadyAssigned) {
                        log.info("ℹ️ Exam already assigned to user: {}", user.getEmail());
                        successUsers.add(user.getEmail() + " (already assigned)");
                        continue;
                    }
                    
                    if (user.getSlot() == null) {
                        log.warn("⚠️ User {} has no slot assigned", user.getEmail());
                        failedUsers.add(user.getEmail() + " (no slot assigned)");
                        continue;
                    }
                    
                    log.info("   Adding exam to user's assigned exams");
                    user.getAssignedExams().add(exam);
                    
                    User savedUser = userRepository.save(user);
                    log.info("✅ Assigned exam to user: {}", savedUser.getEmail());
                    
                    successUsers.add(savedUser.getEmail());
                    
                    try {
                        Slot userSlot = savedUser.getSlot();
                        mailService.sendExamAssignedEmail(
                            savedUser.getEmail(),
                            savedUser.getPassword(),
                            examLink,
                            userSlot.getDate(),
                            userSlot.getTime(),
                            userSlot.getSlotNumber()
                        );
                        log.info("📧 Email queued for: {} (slot {})", 
                                savedUser.getEmail(), userSlot.getSlotNumber());
                    } catch (Exception emailEx) {
                        log.warn("⚠️ Failed to queue email for {}: {}", 
                                savedUser.getEmail(), emailEx.getMessage());
                    }
                    
                } catch (Exception userEx) {
                    log.error("❌ Error processing user {}: {}", userId, userEx.getMessage(), userEx);
                    failedUsers.add("User ID " + userId + ": " + userEx.getMessage());
                }
            }
            
            response.put("success", !successUsers.isEmpty());
            response.put("successCount", successUsers.size());
            response.put("failedCount", failedUsers.size());
            response.put("successUsers", successUsers);
            response.put("failedUsers", failedUsers);
            response.put("examId", examId);
            response.put("examTitle", exam.getTitle());
            
            if (successUsers.isEmpty()) {
                response.put("message", "Failed to assign exam to any users. Please check the errors.");
            } else if (failedUsers.isEmpty()) {
                response.put("message", "Exam assigned successfully to all " + successUsers.size() + " user(s)!");
            } else {
                response.put("message", String.format(
                    "Exam assigned to %d user(s), but %d failed. Check details below.",
                    successUsers.size(), failedUsers.size()
                ));
            }
            
            log.info("✅ Assignment complete: {} success, {} failed", 
                    successUsers.size(), failedUsers.size());
            
        } catch (Exception e) {
            log.error("❌ Error in assignExamToUsers", e);
            response.put("success", false);
            response.put("error", "Failed to assign exam");
            response.put("message", e.getMessage());
            response.put("successUsers", successUsers);
            response.put("failedUsers", failedUsers);
        }
        
        return response;
    }

    @Transactional
    public Map<String, Object> assignExamToUsersAndSendEmails(List<Integer> userIds, Integer examId) {
        log.info("Assigning exam {} to {} users with reliable bulk email queueing", examId, userIds.size());

        Map<String, Object> response = new HashMap<>();
        List<String> successUsers = new ArrayList<>();
        List<String> failedUsers = new ArrayList<>();
        List<MailService.CandidateEmailData> emailCandidates = new ArrayList<>();

        try {
            if (!examRepository.existsById(examId)) {
                response.put("success", false);
                response.put("error", "Exam not found");
                response.put("message", "The selected exam (ID: " + examId + ") does not exist. Please create the exam first before assigning it.");
                return response;
            }

            Optional<Exam> examOpt = examRepository.findById(examId);
            if (examOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Failed to load exam");
                response.put("message", "Exam exists but could not be loaded. Please contact support.");
                return response;
            }

            Exam exam = examOpt.get();
            String examLink = "http://localhost:3000/login?examId=" + examId;

            for (Integer userId : userIds) {
                try {
                    Optional<User> userOpt = userRepository.findByIdWithExams(userId);
                    if (userOpt.isEmpty()) {
                        failedUsers.add("User ID " + userId + " not found");
                        continue;
                    }

                    User user = userOpt.get();
                    if (user.getAssignedExams() == null) {
                        user.setAssignedExams(new HashSet<>());
                    }

                    if (user.getSlot() == null) {
                        failedUsers.add(user.getEmail() + " (no slot assigned)");
                        continue;
                    }

                    boolean alreadyAssigned = user.getAssignedExams().stream()
                        .anyMatch(e -> e.getId() == examId);

                    User savedUser = user;
                    if (alreadyAssigned) {
                        successUsers.add(user.getEmail() + " (already assigned)");
                    } else {
                        user.getAssignedExams().add(exam);
                        savedUser = userRepository.save(user);
                        successUsers.add(savedUser.getEmail());
                    }

                    Slot userSlot = savedUser.getSlot();
                    emailCandidates.add(new MailService.CandidateEmailData(
                        savedUser.getEmail(),
                        savedUser.getPassword(),
                        userSlot.getDate(),
                        userSlot.getTime(),
                        userSlot.getSlotNumber()
                    ));
                } catch (Exception userEx) {
                    log.error("Error processing user {}: {}", userId, userEx.getMessage(), userEx);
                    failedUsers.add("User ID " + userId + ": " + userEx.getMessage());
                }
            }

            queueBulkExamAssignmentEmailsAfterCommit(emailCandidates, examLink, examId, exam.getTitle());

            response.put("success", !successUsers.isEmpty());
            response.put("successCount", successUsers.size());
            response.put("failedCount", failedUsers.size());
            response.put("emailQueuedCount", emailCandidates.size());
            response.put("emailDispatchStatus", emailCandidates.isEmpty() ? "SKIPPED" : "QUEUED");
            response.put("successUsers", successUsers);
            response.put("failedUsers", failedUsers);
            response.put("examId", examId);
            response.put("examTitle", exam.getTitle());

            if (successUsers.isEmpty()) {
                response.put("message", "Failed to assign exam to any users. Please check the errors.");
            } else if (failedUsers.isEmpty()) {
                response.put("message", "Exam assigned successfully and email queued for all " + emailCandidates.size() + " user(s).");
            } else {
                response.put("message", String.format(
                    "Exam processed for %d user(s), email queued for %d, and %d failed. Check details below.",
                    successUsers.size(), emailCandidates.size(), failedUsers.size()
                ));
            }
        } catch (Exception e) {
            log.error("Error in assignExamToUsersAndSendEmails", e);
            response.put("success", false);
            response.put("error", "Failed to assign exam");
            response.put("message", e.getMessage());
            response.put("successUsers", successUsers);
            response.put("failedUsers", failedUsers);
        }

        return response;
    }

    private void queueBulkExamAssignmentEmailsAfterCommit(
            List<MailService.CandidateEmailData> emailCandidates,
            String examLink,
            Integer examId,
            String examTitle) {

        if (emailCandidates == null || emailCandidates.isEmpty()) {
            log.info("No exam assignment emails to queue for exam {}", examId);
            return;
        }

        Runnable dispatch = () -> {
            log.info("Queueing bulk exam assignment emails for exam {} ({}) to {} recipient(s)",
                examId, examTitle, emailCandidates.size());

            CompletableFuture<MailService.BulkEmailResult> future =
                mailService.sendBulkExamAssignedEmails(emailCandidates, examLink);

            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Bulk exam assignment email dispatch failed for exam {}: {}",
                        examId, throwable.getMessage(), throwable);
                    return;
                }

                if (result == null) {
                    log.error("Bulk exam assignment email dispatch for exam {} completed without a result", examId);
                    return;
                }

                if (result.getFailureCount() > 0) {
                    log.warn(
                        "Bulk exam assignment email dispatch {} finished for exam {}: success={}, failed={}, durationMs={}",
                        result.getDispatchId(),
                        examId,
                        result.getSuccessCount(),
                        result.getFailureCount(),
                        result.getDurationMs()
                    );
                    log.warn("Bulk exam assignment email failures for exam {}: {}", examId, result.getFailureReasons());
                    return;
                }

                log.info(
                    "Bulk exam assignment email dispatch {} finished for exam {}: success={}, failed={}, durationMs={}",
                    result.getDispatchId(),
                    examId,
                    result.getSuccessCount(),
                    result.getFailureCount(),
                    result.getDurationMs()
                );
            });
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        log.warn("Skipped bulk exam assignment email dispatch for exam {} because transaction did not commit", examId);
                    }
                }
            });
            log.info("Registered bulk exam assignment email dispatch after transaction commit for exam {}", examId);
            return;
        }

        dispatch.run();
    }

    @Transactional
    public User createUser(User user) {
        log.info("➕ Creating new user: {}", user.getEmail());
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        String normalizedEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);
        user.setEmail(normalizedEmail);
        
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists: " + normalizedEmail);
        }
        
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("Active");
        }
        
        
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(getOrCreateCandidateRole()));
        } else {
           
            Set<Role> managedRoles = new HashSet<>();
            for (Role role : user.getRoles()) {
                Role managedRole = roleRepository.findByName(role.getName())
                    .orElseGet(() -> roleRepository.save(role));
                managedRoles.add(managedRole);
            }
            user.setRoles(managedRoles);
        }
        
        if (user.getSlotNumberInput() != null) {
            log.info("   Processing slot number input: {}", user.getSlotNumberInput());
            
            Optional<Slot> slotOpt = slotRepository.findBySlotNumber(user.getSlotNumberInput());
            if (slotOpt.isPresent()) {
                user.setSlot(slotOpt.get());
                log.info("✅ Assigned slot {} to user {}", user.getSlotNumberInput(), user.getEmail());
            } else {
                log.warn("⚠️ Slot number {} not found", user.getSlotNumberInput());
                throw new IllegalArgumentException("Slot number " + user.getSlotNumberInput() + " does not exist");
            }
        }
        
        if (user.getAssignedExams() == null) {
            user.setAssignedExams(new HashSet<>());
        }
        
        User savedUser = userRepository.save(user);
        log.info("✅ Created user with id: {}", savedUser.getId());
        
        return savedUser;
    }

    @Transactional
    public Map<String, Object> importUsersFromExcel(MultipartFile file, Integer slotId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        if (slotId == null) {
            throw new IllegalArgumentException("Slot is required");
        }

        Slot slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + slotId));

        if (slot.getSlotPassword() == null || slot.getSlotPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Selected slot does not have a password configured");
        }

        StoredUploadFile storedUploadFile = storeUploadFileForSlot(file, slot);

        DataFormatter formatter = new DataFormatter();
        Set<String> emailsInFile = new HashSet<>();
        List<String> failedRows = new ArrayList<>();
        int processedRows = 0;
        int createdCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file does not contain any sheet");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file is empty");
            }

            Map<String, Integer> headerIndex = buildHeaderIndexMap(headerRow, formatter);
            Integer nameColumn = findColumnIndex(headerIndex, "name", "candidate name", "student name", "full name");
            Integer emailColumn = findColumnIndex(headerIndex, "email", "email id", "mail");
            Integer collegeColumn = findColumnIndex(headerIndex, "college", "college name", "institution");
            Integer statusColumn = findColumnIndex(headerIndex, "status");

            if (nameColumn == null || emailColumn == null) {
                throw new IllegalArgumentException("Excel must contain 'Name' and 'Email' columns in the header row");
            }

            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isRowEmpty(row, formatter)) {
                    continue;
                }

                processedRows++;
                int excelRow = rowIndex + 1;

                String name = getCellValue(row, nameColumn, formatter);
                String email = normalizeUploadedEmail(getCellValue(row, emailColumn, formatter));
                String collegeName = collegeColumn != null ? getCellValue(row, collegeColumn, formatter) : "";
                String status = statusColumn != null ? getCellValue(row, statusColumn, formatter) : "";

                if (name.isBlank() || email.isBlank()) {
                    failedRows.add("Row " + excelRow + ": name and email are required");
                    continue;
                }

                if (!isValidEmail(email)) {
                    failedRows.add("Row " + excelRow + ": invalid email format (" + email + ")");
                    continue;
                }

                if (!emailsInFile.add(email)) {
                    failedRows.add("Row " + excelRow + ": duplicate email in file (" + email + ")");
                    continue;
                }

                if (userRepository.existsByEmailIgnoreCase(email)) {
                    failedRows.add("Row " + excelRow + ": email already exists (" + email + ")");
                    continue;
                }

                try {
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setCollegeName(!collegeName.isBlank() ? collegeName : slot.getCollegeName());
                    user.setPassword(slot.getSlotPassword());
                    user.setStatus(!status.isBlank() ? status : "Active");
                    user.setSlotNumber(slot.getSlotNumber());

                    createUser(user);
                    createdCount++;
                } catch (Exception rowException) {
                    failedRows.add("Row " + excelRow + ": " + rowException.getMessage());
                }
            }

        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read uploaded file", ioException);
        } catch (IllegalArgumentException badRequestException) {
            throw badRequestException;
        } catch (Exception parseException) {
            throw new RuntimeException("Unable to parse Excel file. Please upload a valid .xlsx or .xls file", parseException);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", createdCount > 0);
        response.put("slotId", slot.getId());
        response.put("slotNumber", slot.getSlotNumber());
        response.put("uploadedFileName", storedUploadFile.fileName);
        response.put("uploadedFilePath", storedUploadFile.filePath);
        response.put("processedRows", processedRows);
        response.put("createdCount", createdCount);
        response.put("failedCount", failedRows.size());
        response.put("failedRows", failedRows);

        if (createdCount > 0 && failedRows.isEmpty()) {
            response.put("message", "All users uploaded successfully");
        } else if (createdCount > 0) {
            response.put("message", "Upload completed with partial success");
        } else {
            response.put("message", "No users were created from the uploaded file");
        }

        return response;
    }

    public Optional<User> authenticate(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedPassword = password.trim();

        if (FixedAdminCredentials.EMAIL.equalsIgnoreCase(normalizedEmail)) {
            if (FixedAdminCredentials.PASSWORD.equals(normalizedPassword)) {
                return Optional.of(adminBootstrapService.ensureFixedAdminUser());
            }
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (user.getPassword() == null) {
            return Optional.empty();
        }

        if (user.getPassword().equals(password) || user.getPassword().equals(normalizedPassword)) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return userRepository.existsByEmailIgnoreCase(email.trim());
    }

    public Optional<User> findById(int id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAllWithExamsAndRoles();
    }

    @Transactional
    public User updateUser(int id, User userDetails) {
        log.info("✏️ Updating user with id: {}", id);
        
       
        User user = userRepository.findByIdWithExams(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
       
        String originalEmail = user.getEmail();
      
        if (userDetails.getName() != null && !userDetails.getName().trim().isEmpty()) {
            user.setName(userDetails.getName());
        }
        
        if (userDetails.getCollegeName() != null) {
            user.setCollegeName(userDetails.getCollegeName());
        }
       
        if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
            String newEmail = userDetails.getEmail().trim().toLowerCase(Locale.ROOT);
           
            if (!newEmail.equalsIgnoreCase(originalEmail)) {
                if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                    log.error("❌ Email already exists: {}", newEmail);
                    throw new IllegalArgumentException("Email already exists: " + newEmail);
                }
                user.setEmail(newEmail);
                log.info("✅ Email updated from {} to {}", originalEmail, newEmail);
            }
        }
        
        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }
        
        if (userDetails.getStatus() != null) {
            user.setStatus(userDetails.getStatus());
        }
        
        if (userDetails.getRoles() != null && !userDetails.getRoles().isEmpty()) {
            log.info("   Updating roles for user {}", user.getEmail());
           
            user.getRoles().clear();
            
          
            Set<Role> managedRoles = new HashSet<>();
            for (Role role : userDetails.getRoles()) {
                try {
                   
                    Role managedRole;
                    if (role.getName() != null && !role.getName().trim().isEmpty()) {
                        managedRole = roleRepository.findByName(role.getName())
                            .orElseThrow(() -> new IllegalArgumentException("Role not found with name: " + role.getName()));
                    } else {
                      
                        managedRole = roleRepository.findAll().stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No roles found in database"));
                    }
                    managedRoles.add(managedRole);
                } catch (Exception e) {
                    log.error("❌ Error processing role: {}", e.getMessage());
                    throw new IllegalArgumentException("Failed to process role: " + e.getMessage());
                }
            }
            
            user.getRoles().addAll(managedRoles);
            log.info("✅ Updated roles for user {}", user.getEmail());
        }
        
       
        if (userDetails.getSlotNumberInput() != null) {
            log.info("   Updating slot to number {}", userDetails.getSlotNumberInput());
            
            Optional<Slot> slotOpt = slotRepository.findBySlotNumber(userDetails.getSlotNumberInput());
            if (slotOpt.isPresent()) {
                user.setSlot(slotOpt.get());
                log.info("✅ Updated slot to {} for user {}", userDetails.getSlotNumberInput(), user.getEmail());
            } else {
                log.warn("⚠️ Slot number {} not found during update", userDetails.getSlotNumberInput());
                throw new IllegalArgumentException("Slot number " + userDetails.getSlotNumberInput() + " does not exist");
            }
        }
        
        
        User savedUser = userRepository.save(user);
        log.info("✅ Updated user: {}", savedUser.getEmail());
        
        return savedUser;
    }

    @Transactional
    public void deleteUser(int id) {
        log.info("🗑️ Deleting user with id: {}", id);
        
        User user = userRepository.findByIdWithExams(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (user.getEmail() != null && FixedAdminCredentials.EMAIL.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Fixed admin user cannot be deleted");
        }

        int deletedSubmissions = testSubmissionRepository.deleteByUserId(id);
        if (deletedSubmissions > 0) {
            log.info("Deleted {} test submission(s) for user {}", deletedSubmissions, user.getEmail());
        }

        int deletedProctoringRecords = proctoringRepository.deleteByUserId(id);
        if (deletedProctoringRecords > 0) {
            log.info("Deleted {} proctoring record(s) for user {}", deletedProctoringRecords, user.getEmail());
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            int deletedResults = resultRepository.deleteByEmailIgnoreCase(user.getEmail());
            if (deletedResults > 0) {
                log.info("Deleted {} result record(s) for user {}", deletedResults, user.getEmail());
            }
        }

        int deletedAssignedExamMappings = userRepository.deleteAssignedExamMappings(id);
        if (deletedAssignedExamMappings > 0) {
            log.info("Deleted {} assigned exam mapping row(s) for user {}", deletedAssignedExamMappings, user.getEmail());
        }

        int deletedRoleMappings = userRepository.deleteRoleMappings(id);
        if (deletedRoleMappings > 0) {
            log.info("Deleted {} role mapping row(s) for user {}", deletedRoleMappings, user.getEmail());
        }
        
        userRepository.deleteById(id);
        log.info("✅ Deleted user: {}", user.getEmail());
    }

    private Role getOrCreateCandidateRole() {
        return roleRepository.findByName("CANDIDATE")
            .orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName("CANDIDATE");
                return roleRepository.save(newRole);
            });
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (!header.isEmpty()) {
                headerMap.put(header, cell.getColumnIndex());
            }
        }
        return headerMap;
    }

    private Integer findColumnIndex(Map<String, Integer> headerMap, String... headerNames) {
        for (String headerName : headerNames) {
            Integer index = headerMap.get(normalizeHeader(headerName));
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase(Locale.ROOT).replace("_", " ").replace("-", " ");
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        if (row.getFirstCellNum() < 0 || row.getLastCellNum() < 0) {
            return true;
        }

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            String value = getCellValue(row, i, formatter);
            if (!value.isBlank()) {
                return false;
            }
        }

        return true;
    }

    private String getCellValue(Row row, Integer columnIndex, DataFormatter formatter) {
        if (row == null || columnIndex == null || columnIndex < 0) {
            return "";
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private StoredUploadFile storeUploadFileForSlot(MultipartFile file, Slot slot) {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
        String safeName = sanitizeFilename(originalName);
        String timestamp = LocalDateTime.now().format(UPLOAD_TS_FORMAT);
        String finalFileName = timestamp + "_" + safeName;
        String slotFolder = "slot-" + (slot.getSlotNumber() != null ? slot.getSlotNumber() : slot.getId());

        Path slotDir = Paths.get(userUploadDir, slotFolder).normalize();
        Path targetPath = slotDir.resolve(finalFileName).normalize();

        if (!targetPath.startsWith(slotDir)) {
            throw new IllegalArgumentException("Invalid upload file path");
        }

        try {
            Files.createDirectories(slotDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Stored user upload file for slot {} at {}", slot.getId(), targetPath);
            return new StoredUploadFile(finalFileName, targetPath.toString());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store uploaded file", ex);
        }
    }

    private String sanitizeFilename(String filename) {
        String baseName = Paths.get(filename).getFileName().toString();
        String cleaned = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "upload.xlsx" : cleaned;
    }

    private String normalizeUploadedEmail(String rawEmail) {
        if (rawEmail == null) {
            return "";
        }

        String normalized = rawEmail.replace('\u00A0', ' ').trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.toLowerCase(Locale.ROOT).startsWith("mailto:")) {
            normalized = normalized.substring("mailto:".length()).trim();
        }

        Matcher matcher = EMAIL_EXTRACT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1).trim().toLowerCase(Locale.ROOT);
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private static class StoredUploadFile {
        private final String fileName;
        private final String filePath;

        private StoredUploadFile(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }
    }
}
