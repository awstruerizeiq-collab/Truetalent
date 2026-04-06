package com.truerize.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.truerize.entity.Slot;
import com.truerize.entity.User;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final int SLOT_WINDOW_MINUTES = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final int bulkBatchSize;
    private final long delayBetweenEmailsMs;
    private final long delayBetweenBatchesMs;
    private final int maxRetries;
    private final long retryDelayMs;
    private final Executor mailTaskExecutor;

    public MailService(
            JavaMailSender mailSender,
            @Qualifier("mailTaskExecutor") Executor mailTaskExecutor,
            @Value("${app.mail.from:${spring.mail.username:talent@truerize.com}}") String fromEmail,
            @Value("${app.mail.bulk.batch-size:20}") int bulkBatchSize,
            @Value("${app.mail.bulk.delay-between-emails-ms:400}") long delayBetweenEmailsMs,
            @Value("${app.mail.bulk.delay-between-batches-ms:1500}") long delayBetweenBatchesMs,
            @Value("${app.mail.bulk.max-retries:3}") int maxRetries,
            @Value("${app.mail.bulk.retry-delay-ms:2000}") long retryDelayMs) {
        this.mailSender = mailSender;
        this.mailTaskExecutor = mailTaskExecutor;
        this.fromEmail = fromEmail;
        this.bulkBatchSize = Math.max(1, bulkBatchSize);
        this.delayBetweenEmailsMs = Math.max(0L, delayBetweenEmailsMs);
        this.delayBetweenBatchesMs = Math.max(0L, delayBetweenBatchesMs);
        this.maxRetries = Math.max(1, maxRetries);
        this.retryDelayMs = Math.max(0L, retryDelayMs);
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<EmailSendResult> sendExamAssignedEmail(
            String to,
            String password,
            String examLink,
            LocalDate slotDate,
            LocalTime slotTime,
            Integer slotNumber) {

        CandidateEmailData candidate = new CandidateEmailData(to, password, slotDate, slotTime, slotNumber);
        EmailSendResult result = sendExamAssignedEmailWithRetry(candidate, examLink, "single-exam-mail", 1, 1);
        return CompletableFuture.completedFuture(result);
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<EmailSendResult> sendResultReleasedEmail(String to) {
        String normalizedEmail = normalizeEmail(to);
        EmailSendResult result = sendSimpleEmailWithRetry(
            normalizedEmail,
            "Congratulations! You Passed the Assessment",
            buildResultReleasedEmailBody(),
            "result-release",
            "result-release",
            1,
            1
        );
        return CompletableFuture.completedFuture(result);
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<EmailSendResult> sendRegretEmail(String to) {
        String normalizedEmail = normalizeEmail(to);
        EmailSendResult result = sendSimpleEmailWithRetry(
            normalizedEmail,
            "Your Assessment Result",
            buildRegretEmailBody(),
            "result-regret",
            "result-regret",
            1,
            1
        );
        return CompletableFuture.completedFuture(result);
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<BulkEmailResult> sendBulkExamAssignedEmails(
            List<CandidateEmailData> candidates,
            String examLink) {
        return CompletableFuture.completedFuture(dispatchBulkExamAssignedEmails(candidates, examLink));
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<BulkEmailResult> sendBulkExamAssignedEmailsForUsers(
            List<User> users,
            String examLink) {
        List<CandidateEmailData> candidates = mapUsersToCandidateEmails(users);
        return CompletableFuture.completedFuture(dispatchBulkExamAssignedEmails(candidates, examLink));
    }

    public boolean testEmailConfiguration() {
        try {
            EmailSendResult result = sendSimpleEmailWithRetry(
                normalizeEmail(fromEmail),
                "Test Email - Truerize Exam Portal",
                "This is a test email from Truerize Exam Portal. Configuration is working!",
                "configuration-test",
                "configuration-test",
                1,
                1
            );
            return result.isSuccess();
        } catch (Exception ex) {
            log.error("Email configuration test failed: {}", ex.getMessage(), ex);
            return false;
        }
    }

    public void sendDirectTestEmail(String to) {
        String normalizedEmail = normalizeEmail(to);
        EmailSendResult result = sendSimpleEmailWithRetry(
            normalizedEmail,
            "Truerize Exam Portal Test Email",
            "Hello,\n\n"
                + "This is a direct test email from the Truerize Exam Portal backend.\n\n"
                + "Sent to: " + normalizedEmail + "\n"
                + "From: " + fromEmail + "\n\n"
                + "If you received this email, SMTP delivery from the application is working.\n\n"
                + "Regards,\n"
                + "Truerize Exam Portal",
            "direct-test",
            "direct-test",
            1,
            1
        );

        if (!result.isSuccess()) {
            throw new IllegalStateException("Failed to send test email to " + normalizedEmail + ": " + result.getErrorMessage());
        }
    }

    private BulkEmailResult dispatchBulkExamAssignedEmails(List<CandidateEmailData> candidates, String examLink) {
        String dispatchId = "bulk-" + UUID.randomUUID().toString().substring(0, 8);
        Instant startedAt = Instant.now();
        List<CandidateEmailData> safeCandidates = candidates != null ? candidates : Collections.emptyList();
        List<CandidateEmailData> uniqueCandidates = deduplicateCandidates(safeCandidates);

        log.info(
            "Mail dispatch {} started: requested={}, unique={}, batchSize={}, delayBetweenEmailsMs={}, delayBetweenBatchesMs={}, maxRetries={}",
            dispatchId,
            safeCandidates.size(),
            uniqueCandidates.size(),
            bulkBatchSize,
            delayBetweenEmailsMs,
            delayBetweenBatchesMs,
            maxRetries
        );

        if (uniqueCandidates.isEmpty()) {
            return BulkEmailResult.empty(dispatchId, safeCandidates.size(), startedAt, Instant.now());
        }

        int successCount = 0;
        int failureCount = 0;
        List<String> failedEmails = new ArrayList<>();
        Map<String, String> failureReasons = new LinkedHashMap<>();

        try {
            List<List<CandidateEmailData>> batches = createBatches(uniqueCandidates, bulkBatchSize);
            int processedCount = 0;

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<CandidateEmailData> batch = batches.get(batchIndex);
                log.info(
                    "Mail dispatch {} processing batch {}/{} with {} recipient(s)",
                    dispatchId,
                    batchIndex + 1,
                    batches.size(),
                    batch.size()
                );

                List<CompletableFuture<EmailSendResult>> batchFutures = new ArrayList<>();

                for (CandidateEmailData candidate : batch) {
                    processedCount++;
                    if (!batchFutures.isEmpty() && delayBetweenEmailsMs > 0) {
                        pause(delayBetweenEmailsMs, dispatchId, candidate.getEmail(), "between emails");
                    }

                    final int recipientIndex = processedCount;
                    batchFutures.add(CompletableFuture.supplyAsync(
                        () -> safeSendExamAssignedEmail(candidate, examLink, dispatchId, recipientIndex, uniqueCandidates.size()),
                        mailTaskExecutor
                    ));
                }

                CompletableFuture.allOf(batchFutures.toArray(CompletableFuture[]::new)).join();

                for (CompletableFuture<EmailSendResult> future : batchFutures) {
                    EmailSendResult sendResult = future.join();
                    if (sendResult.isSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                        failedEmails.add(sendResult.getEmail());
                        failureReasons.put(sendResult.getEmail(), sendResult.getErrorMessage());
                    }
                }

                if (batchIndex < batches.size() - 1 && delayBetweenBatchesMs > 0) {
                    pause(delayBetweenBatchesMs, dispatchId, null, "between batches");
                }
            }
        } catch (Exception ex) {
            log.error("Mail dispatch {} aborted unexpectedly: {}", dispatchId, ex.getMessage(), ex);
        }

        Instant completedAt = Instant.now();
        BulkEmailResult result = new BulkEmailResult(
            dispatchId,
            safeCandidates.size(),
            uniqueCandidates.size(),
            successCount,
            failureCount,
            failedEmails,
            failureReasons,
            Duration.between(startedAt, completedAt).toMillis(),
            startedAt,
            completedAt
        );

        if (failureCount == 0) {
            log.info("Mail dispatch {} completed successfully: {}", dispatchId, result);
        } else {
            log.warn("Mail dispatch {} completed with failures: {}", dispatchId, result);
            log.warn("Mail dispatch {} failed recipients: {}", dispatchId, failureReasons);
        }

        return result;
    }

    private EmailSendResult safeSendExamAssignedEmail(
            CandidateEmailData candidate,
            String examLink,
            String dispatchId,
            int recipientIndex,
            int totalRecipients) {

        try {
            return sendExamAssignedEmailWithRetry(candidate, examLink, dispatchId, recipientIndex, totalRecipients);
        } catch (Exception ex) {
            Throwable rootCause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
            String email = candidate != null && candidate.getEmail() != null
                ? candidate.getEmail().trim().toLowerCase(Locale.ROOT)
                : "unknown";
            String errorMessage = rootCause instanceof Exception
                ? resolveErrorMessage((Exception) rootCause)
                : rootCause.getMessage();

            log.error(
                "Mail dispatch {} rejected exam-assignment email for {} before send: {}",
                dispatchId,
                email,
                errorMessage,
                rootCause
            );
            return EmailSendResult.failure(email, "Truerize Assessment Invitation", 0, errorMessage);
        }
    }

    private EmailSendResult sendExamAssignedEmailWithRetry(
            CandidateEmailData candidate,
            String examLink,
            String dispatchId,
            int recipientIndex,
            int totalRecipients) {

        validateExamAssignmentCandidate(candidate, examLink);
        String recipient = normalizeEmail(candidate.getEmail());
        String subject = "Truerize Assessment Invitation - Slot " + candidate.getSlotNumber();
        String body = buildExamAssignedEmailBody(
            recipient,
            candidate.getPassword(),
            examLink,
            candidate.getSlotDate(),
            candidate.getSlotTime(),
            candidate.getSlotNumber()
        );

        return sendSimpleEmailWithRetry(
            recipient,
            subject,
            body,
            "exam-assignment",
            dispatchId,
            recipientIndex,
            totalRecipients
        );
    }

    private EmailSendResult sendSimpleEmailWithRetry(
            String to,
            String subject,
            String body,
            String emailType,
            String dispatchId,
            int recipientIndex,
            int totalRecipients) {

        String recipient = normalizeEmail(to);
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info(
                    "Mail dispatch {} sending {} email to {} ({}/{}, attempt {}/{})",
                    dispatchId,
                    emailType,
                    recipient,
                    recipientIndex,
                    totalRecipients,
                    attempt,
                    maxRetries
                );

                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);

                log.info(
                    "Mail dispatch {} sent {} email to {} on attempt {}",
                    dispatchId,
                    emailType,
                    recipient,
                    attempt
                );
                return EmailSendResult.success(recipient, subject, attempt);
            } catch (Exception ex) {
                lastError = ex;
                String errorMessage = resolveErrorMessage(ex);
                log.warn(
                    "Mail dispatch {} failed {} email to {} on attempt {}/{}: {}",
                    dispatchId,
                    emailType,
                    recipient,
                    attempt,
                    maxRetries,
                    errorMessage
                );

                if (attempt < maxRetries) {
                    pause(retryDelayMs, dispatchId, recipient, "before retry");
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            }
        }

        String errorMessage = resolveErrorMessage(lastError);
        log.error(
            "Mail dispatch {} exhausted retries for {} email to {}: {}",
            dispatchId,
            emailType,
            recipient,
            errorMessage,
            lastError
        );
        return EmailSendResult.failure(recipient, subject, maxRetries, errorMessage);
    }

    private List<CandidateEmailData> mapUsersToCandidateEmails(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        List<CandidateEmailData> candidates = new ArrayList<>();
        for (User user : users) {
            CandidateEmailData candidate = toCandidateEmailData(user);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private CandidateEmailData toCandidateEmailData(User user) {
        if (user == null) {
            return null;
        }

        Slot slot = user.getSlot();
        if (slot == null) {
            log.warn("Skipping bulk mail mapping for user {} because no slot is assigned", user.getEmail());
            return null;
        }

        try {
            return new CandidateEmailData(
                normalizeEmail(user.getEmail()),
                user.getPassword(),
                slot.getDate(),
                slot.getTime(),
                slot.getSlotNumber()
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping bulk mail mapping for user {}: {}", user.getEmail(), ex.getMessage());
            return null;
        }
    }

    private List<CandidateEmailData> deduplicateCandidates(List<CandidateEmailData> candidates) {
        Map<String, CandidateEmailData> uniqueCandidates = new LinkedHashMap<>();
        for (CandidateEmailData candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            try {
                String normalizedEmail = normalizeEmail(candidate.getEmail());
                candidate.setEmail(normalizedEmail);
                uniqueCandidates.put(normalizedEmail, candidate);
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping candidate from bulk email list: {}", ex.getMessage());
            }
        }
        return new ArrayList<>(uniqueCandidates.values());
    }

    private void validateExamAssignmentCandidate(CandidateEmailData candidate, String examLink) {
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate email data is required");
        }
        normalizeEmail(candidate.getEmail());
        if (candidate.getPassword() == null || candidate.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required for exam assignment email");
        }
        if (candidate.getSlotDate() == null) {
            throw new IllegalArgumentException("Slot date is required for exam assignment email");
        }
        if (candidate.getSlotTime() == null) {
            throw new IllegalArgumentException("Slot time is required for exam assignment email");
        }
        if (candidate.getSlotNumber() == null) {
            throw new IllegalArgumentException("Slot number is required for exam assignment email");
        }
        if (examLink == null || examLink.isBlank()) {
            throw new IllegalArgumentException("Exam link is required for exam assignment email");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex == null) {
            return "Unknown mail error";
        }
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    private String buildExamAssignedEmailBody(
            String to,
            String password,
            String examLink,
            LocalDate slotDate,
            LocalTime slotTime,
            Integer slotNumber) {

        String formattedDate = slotDate.format(DATE_FORMATTER);
        String formattedTime = slotTime.format(TIME_FORMATTER);
        LocalTime endTime = slotTime.plusMinutes(SLOT_WINDOW_MINUTES);
        String formattedEndTime = endTime.format(TIME_FORMATTER);

        return "Dear Candidate,\n\n"
            + "You have been invited to participate in the Truerize online assessment.\n\n"
            + "LOGIN DETAILS:\n"
            + "Assessment Link: " + examLink + "\n"
            + "Username (Email): " + to + "\n"
            + "Password: " + password + "\n\n"
            + "SLOT DETAILS:\n"
            + "Slot Number: " + slotNumber + "\n"
            + "Date: " + formattedDate + "\n"
            + "Login Window: " + formattedTime + " to " + formattedEndTime + " (30 minutes)\n"
            + "Exam Duration: 1 hour (after you start)\n\n"
            + "CRITICAL INSTRUCTIONS:\n"
            + "- The exam link will only work during your assigned 30-minute login window\n"
            + "- You must login between " + formattedTime + " and " + formattedEndTime + " on " + formattedDate + "\n"
            + "- Once you login and start the exam, you will have 1 hour to complete it\n"
            + "- The link will be disabled before and after the 30-minute login window\n"
            + "- Ensure stable internet connection before your slot\n"
            + "- Keep camera and microphone ready for proctoring\n"
            + "- You will receive a unique shuffled question set based on your slot\n"
            + "- Once you start, the exam cannot be paused or retaken\n\n"
            + "EXAM FORMAT:\n"
            + "- Total Duration: 1 hour\n"
            + "- You must login within the 30-minute window mentioned above\n"
            + "- After login, timer starts and you have 60 minutes to complete\n\n"
            + "Do not attempt to login outside your slot time. The system will reject your access.\n\n"
            + "For any queries, contact: hr@truerize.com\n\n"
            + "Best regards,\n"
            + "Truerize Recruitment Team\n"
            + "HSR Layout, Bengaluru\n"
            + "https://www.truerize.com/";
    }

    private String buildResultReleasedEmailBody() {
        return "Dear Candidate,\n\n"
            + "Congratulations! We are delighted to inform you that you have successfully cleared the online assessment. "
            + "Your performance demonstrated a strong understanding of the required concepts and skills.\n\n"
            + "We are pleased to invite you to the next stage of our selection process - HR will contact soon.\n\n"
            + "This interview will allow our technical panel to better understand your problem-solving approach, "
            + "domain expertise, and overall suitability for the role.\n\n"
            + "Kindly confirm your availability by replying to this email at your earliest convenience. "
            + "Once we receive your confirmation, we will share further details and instructions to help you prepare for the interview.\n\n"
            + "We appreciate the effort and enthusiasm you have shown throughout the assessment process "
            + "and look forward to interacting with you in the next round.\n\n"
            + "Best Regards,\n"
            + "HR Manager\n"
            + "Truerize\n"
            + "hr@truerize.com / +91-9876543210";
    }

    private String buildRegretEmailBody() {
        return "Dear Candidate,\n\n"
            + "Thank you for taking the time to participate in the online assessment for Truerize.\n\n"
            + "After careful evaluation, we regret to inform you that your performance did not meet the criteria required "
            + "to proceed to the next stage of our selection process.\n\n"
            + "We truly appreciate the effort, time, and interest you have shown in exploring opportunities with us. "
            + "Please do not be discouraged-new opportunities arise frequently at Truerize, and we encourage you to reapply in the future "
            + "as your skills and experience continue to grow.\n\n"
            + "We wish you continued success in your career journey and thank you once again for considering Truerize.\n\n"
            + "Best Regards,\n"
            + "HR Manager\n"
            + "Truerize\n"
            + "hr@truerize.com / +91-9876543210";
    }

    private <T> List<List<T>> createBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int start = 0; start < list.size(); start += batchSize) {
            int end = Math.min(start + batchSize, list.size());
            batches.add(list.subList(start, end));
        }
        return batches;
    }

    private void pause(long delayMs, String dispatchId, String email, String reason) {
        if (delayMs <= 0) {
            return;
        }

        try {
            if (email != null) {
                log.debug("Mail dispatch {} waiting {} ms {} for {}", dispatchId, delayMs, reason, email);
            } else {
                log.debug("Mail dispatch {} waiting {} ms {}", dispatchId, delayMs, reason);
            }
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mail dispatch interrupted while waiting " + reason, ex);
        }
    }

    public static class CandidateEmailData {
        private String email;
        private String password;
        private LocalDate slotDate;
        private LocalTime slotTime;
        private Integer slotNumber;

        public CandidateEmailData() {
        }

        public CandidateEmailData(
                String email,
                String password,
                LocalDate slotDate,
                LocalTime slotTime,
                Integer slotNumber) {
            this.email = email;
            this.password = password;
            this.slotDate = slotDate;
            this.slotTime = slotTime;
            this.slotNumber = slotNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public LocalDate getSlotDate() {
            return slotDate;
        }

        public void setSlotDate(LocalDate slotDate) {
            this.slotDate = slotDate;
        }

        public LocalTime getSlotTime() {
            return slotTime;
        }

        public void setSlotTime(LocalTime slotTime) {
            this.slotTime = slotTime;
        }

        public Integer getSlotNumber() {
            return slotNumber;
        }

        public void setSlotNumber(Integer slotNumber) {
            this.slotNumber = slotNumber;
        }
    }

    public static class EmailSendResult {
        private final String email;
        private final String subject;
        private final boolean success;
        private final int attempts;
        private final String errorMessage;
        private final Instant completedAt;

        private EmailSendResult(
                String email,
                String subject,
                boolean success,
                int attempts,
                String errorMessage,
                Instant completedAt) {
            this.email = email;
            this.subject = subject;
            this.success = success;
            this.attempts = attempts;
            this.errorMessage = errorMessage;
            this.completedAt = completedAt;
        }

        public static EmailSendResult success(String email, String subject, int attempts) {
            return new EmailSendResult(email, subject, true, attempts, null, Instant.now());
        }

        public static EmailSendResult failure(String email, String subject, int attempts, String errorMessage) {
            return new EmailSendResult(email, subject, false, attempts, errorMessage, Instant.now());
        }

        public String getEmail() {
            return email;
        }

        public String getSubject() {
            return subject;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getAttempts() {
            return attempts;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }
    }

    public static class BulkEmailResult {
        private final String dispatchId;
        private final int requestedCount;
        private final int uniqueRecipientCount;
        private final int successCount;
        private final int failureCount;
        private final List<String> failedEmails;
        private final Map<String, String> failureReasons;
        private final long durationMs;
        private final Instant startedAt;
        private final Instant completedAt;

        public BulkEmailResult(
                String dispatchId,
                int requestedCount,
                int uniqueRecipientCount,
                int successCount,
                int failureCount,
                List<String> failedEmails,
                Map<String, String> failureReasons,
                long durationMs,
                Instant startedAt,
                Instant completedAt) {
            this.dispatchId = dispatchId;
            this.requestedCount = requestedCount;
            this.uniqueRecipientCount = uniqueRecipientCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.failedEmails = List.copyOf(failedEmails != null ? failedEmails : Collections.emptyList());
            this.failureReasons = Collections.unmodifiableMap(
                new LinkedHashMap<>(failureReasons != null ? failureReasons : Collections.emptyMap())
            );
            this.durationMs = durationMs;
            this.startedAt = startedAt;
            this.completedAt = completedAt;
        }

        public static BulkEmailResult empty(String dispatchId, int requestedCount, Instant startedAt, Instant completedAt) {
            return new BulkEmailResult(
                dispatchId,
                requestedCount,
                0,
                0,
                0,
                Collections.emptyList(),
                Collections.emptyMap(),
                Duration.between(startedAt, completedAt).toMillis(),
                startedAt,
                completedAt
            );
        }

        public String getDispatchId() {
            return dispatchId;
        }

        public int getRequestedCount() {
            return requestedCount;
        }

        public int getUniqueRecipientCount() {
            return uniqueRecipientCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public List<String> getFailedEmails() {
            return failedEmails;
        }

        public Map<String, String> getFailureReasons() {
            return failureReasons;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }

        public int getTotalCount() {
            return successCount + failureCount;
        }

        @Override
        public String toString() {
            return "BulkEmailResult{"
                + "dispatchId='" + dispatchId + '\''
                + ", requested=" + requestedCount
                + ", unique=" + uniqueRecipientCount
                + ", success=" + successCount
                + ", failed=" + failureCount
                + ", durationMs=" + durationMs
                + '}';
        }
    }
}
