package com.truerize.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    private static final String FROM_EMAIL = "talent@truerize.com";
    private static final int BATCH_SIZE = 20;
    private static final int DELAY_BETWEEN_EMAILS_MS = 500;
    private static final int DELAY_BETWEEN_BATCHES_MS = 3000;
    private static final int SLOT_WINDOW_MINUTES = 30; 

  
    @Async
    public void sendExamAssignedEmail(String to, String password, String examLink, 
                                     LocalDate slotDate, LocalTime slotTime, Integer slotNumber) {
        if (to == null || to.isBlank()) {
            log.warn("⚠️ Skipping exam assignment email: invalid email");
            return;
        }
        
        try {
            log.info("📧 Attempting to send exam assignment email to: {}", to);
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
            
            String formattedDate = slotDate.format(dateFormatter);
            String formattedTime = slotTime.format(timeFormatter);
            LocalTime endTime = slotTime.plusMinutes(SLOT_WINDOW_MINUTES);
            String formattedEndTime = endTime.format(timeFormatter);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject("Truerize Assessment Invitation - Slot " + slotNumber);
            message.setText(
                "Dear Candidate,\n\n" +
                "You have been invited to participate in the Truerize online assessment.\n\n" +
                "📋 LOGIN DETAILS:\n" +
                "Assessment Link: " + examLink + "\n" +
                "Username (Email): " + to + "\n" +
                "Password: " + password + "\n\n" +
                "🕐 SLOT DETAILS:\n" +
                "Slot Number: " + slotNumber + "\n" +
                "Date: " + formattedDate + "\n" +
                "Login Window: " + formattedTime + " to " + formattedEndTime + " (30 minutes)\n" +
                "Exam Duration: 1 hour (after you start)\n\n" +
                "⚠️ CRITICAL INSTRUCTIONS:\n" +
                "• The exam link will ONLY work during your assigned 30-minute login window\n" +
                "• You must login between " + formattedTime + " and " + formattedEndTime + " on " + formattedDate + "\n" +
                "• Once you login and start the exam, you will have 1 hour to complete it\n" +
                "• The link will be disabled before and after the 30-minute login window\n" +
                "• Ensure stable internet connection before your slot\n" +
                "• Keep camera and microphone ready for proctoring\n" +
                "• You will receive a unique shuffled question set based on your slot\n" +
                "• Once you start, the exam CANNOT be paused or retaken\n\n" +
                "📝 EXAM FORMAT:\n" +
                "• Total Duration: 1 hour\n" +
                "• You must login within the 30-minute window mentioned above\n" +
                "• After login, timer starts and you have 60 minutes to complete\n\n" +
                "❌ DO NOT attempt to login outside your slot time - the system will reject your access.\n\n" +
                "For any queries, contact: hr@truerize.com\n\n" +
                "Best regards,\n" +
                "Truerize Recruitment Team\n" +
                "HSR Layout, Bengaluru\n" +
                "https://www.truerize.com/"
            );
            
            mailSender.send(message);
            log.info("✅ Exam assignment email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("❌ Failed to send exam assignment email to {}: {}", to, e.getMessage(), e);
        }
    }

   
    @Async
    public void sendResultReleasedEmail(String to) {
        if (to == null || to.isBlank()) {
            log.warn("⚠️ Skipping congratulatory email: invalid email");
            return;
        }

        try {
            log.info("🎉 Sending congratulatory email to: {}", to);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject("Congratulations! You Passed the Assessment");
            message.setText(
                "Dear Candidate,\n\n" +
                "Congratulations! We are delighted to inform you that you have successfully cleared the online assessment. " +
                "Your performance demonstrated a strong understanding of the required concepts and skills.\n\n" +
                "We are pleased to invite you to the next stage of our selection process – the Technical Interview, " +
                "scheduled on 25th November 2025 at 10:00 AM IST.\n\n" +
                "This interview will allow our technical panel to better understand your problem-solving approach, " +
                "domain expertise, and overall suitability for the role.\n\n" +
                "Kindly confirm your availability by replying to this email at your earliest convenience. " +
                "Once we receive your confirmation, we will share further details and instructions to help you prepare for the interview.\n\n" +
                "We appreciate the effort and enthusiasm you have shown throughout the assessment process " +
                "and look forward to interacting with you in the next round.\n\n" +
                "Best Regards,\n" +
                "HR Manager\n" +
                "Truerize\n" +
                "hr@truerize.com / +91-9876543210"
            );
            
            mailSender.send(message);
            log.info("✅ Congratulatory email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("❌ Failed to send congratulatory email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendRegretEmail(String to) {
        if (to == null || to.isBlank()) {
            log.warn("⚠️ Skipping regret email: invalid email");
            return;
        }

        try {
            log.info("📧 Sending regret email to: {}", to);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject("Your Assessment Result");
            message.setText(
                "Dear Candidate,\n\n" +
                "Thank you for taking the time to participate in the online assessment for Truerize.\n\n" +
                "After careful evaluation, we regret to inform you that your performance did not meet the criteria required to proceed to the next stage of our selection process.\n\n" +
                "We truly appreciate the effort, time, and interest you have shown in exploring opportunities with us. " +
                "Please don't be discouraged—new opportunities arise frequently at Truerize, and we encourage you to reapply in the future as your skills and experience continue to grow.\n\n" +
                "We wish you continued success in your career journey and thank you once again for considering Truerize.\n\n" +
                "Best Regards,\n" +
                "HR Manager\n" +
                "Truerize\n" +
                "hr@truerize.com / +91-9876543210"
            );
            
            mailSender.send(message);
            log.info("✅ Regret email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("❌ Failed to send regret email to {}: {}", to, e.getMessage(), e);
        }
    }

   
    @Async
    public CompletableFuture<BulkEmailResult> sendBulkExamAssignedEmails(
            List<CandidateEmailData> candidates, String examLink) {
        
        log.info("🚀 Starting bulk email send for {} candidates", candidates.size());
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> failedEmails = new ArrayList<>();
        
        try {
            List<List<CandidateEmailData>> batches = createBatches(candidates, BATCH_SIZE);
            
            for (int i = 0; i < batches.size(); i++) {
                List<CandidateEmailData> batch = batches.get(i);
                log.info("📧 Processing batch {}/{} ({} emails)", 
                    i + 1, batches.size(), batch.size());
                
                for (CandidateEmailData candidate : batch) {
                    try {
                        sendSingleExamAssignedEmailSync(
                            candidate.getEmail(), 
                            candidate.getPassword(), 
                            examLink,
                            candidate.getSlotDate(),
                            candidate.getSlotTime(),
                            candidate.getSlotNumber()
                        );
                        successCount.incrementAndGet();
                        log.info("✅ Sent to: {}", candidate.getEmail());
                        Thread.sleep(DELAY_BETWEEN_EMAILS_MS);
                        
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        failedEmails.add(candidate.getEmail());
                        log.error("❌ Failed to send to {}: {}", 
                            candidate.getEmail(), e.getMessage());
                    }
                }
                
                if (i < batches.size() - 1) {
                    log.info("⏸️  Waiting {} seconds before next batch...", 
                        DELAY_BETWEEN_BATCHES_MS / 1000);
                    Thread.sleep(DELAY_BETWEEN_BATCHES_MS);
                }
            }
            
            log.info("✅ Bulk email send completed: {} success, {} failed", 
                successCount.get(), failureCount.get());
            
            if (!failedEmails.isEmpty()) {
                log.warn("⚠️  Failed emails: {}", failedEmails);
            }
            
            return CompletableFuture.completedFuture(
                new BulkEmailResult(successCount.get(), failureCount.get(), failedEmails)
            );
            
        } catch (Exception e) {
            log.error("❌ Bulk email send failed: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                new BulkEmailResult(successCount.get(), failureCount.get(), failedEmails)
            );
        }
    }
    
    
    
    private void sendSingleExamAssignedEmailSync(String to, String password, String examLink,
                                                 LocalDate slotDate, LocalTime slotTime, Integer slotNumber) 
            throws Exception {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        
        String formattedDate = slotDate.format(dateFormatter);
        String formattedTime = slotTime.format(timeFormatter);
        LocalTime endTime = slotTime.plusMinutes(SLOT_WINDOW_MINUTES);
        String formattedEndTime = endTime.format(timeFormatter);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_EMAIL);
        message.setTo(to);
        message.setSubject("Truerize Assessment Invitation - Slot " + slotNumber);
        message.setText(
            "Dear Candidate,\n\n" +
            "You have been invited to participate in the Truerize online assessment.\n\n" +
            "📋 LOGIN DETAILS:\n" +
            "Assessment Link: " + examLink + "\n" +
            "Username (Email): " + to + "\n" +
            "Password: " + password + "\n\n" +
            "🕐 SLOT DETAILS:\n" +
            "Slot Number: " + slotNumber + "\n" +
            "Date: " + formattedDate + "\n" +
            "Login Window: " + formattedTime + " to " + formattedEndTime + " (30 minutes)\n" +
            "Exam Duration: 1 hour (after you start)\n\n" +
            "⚠️ CRITICAL INSTRUCTIONS:\n" +
            "• The exam link will ONLY work during your assigned 30-minute login window\n" +
            "• You must login between " + formattedTime + " and " + formattedEndTime + " on " + formattedDate + "\n" +
            "• Once you login and start the exam, you will have 1 hour to complete it\n" +
            "• The link will be disabled before and after the 30-minute login window\n" +
            "• Ensure stable internet connection before your slot\n" +
            "• Keep camera and microphone ready for proctoring\n" +
            "• You will receive a unique shuffled question set based on your slot\n" +
            "• Once you start, the exam CANNOT be paused or retaken\n\n" +
            "📝 EXAM FORMAT:\n" +
            "• Total Duration: 1 hour\n" +
            "• You must login within the 30-minute window mentioned above\n" +
            "• After login, timer starts and you have 60 minutes to complete\n\n" +
            "❌ DO NOT attempt to login outside your slot time - the system will reject your access.\n\n" +
            "For any queries, contact: hr@truerize.com\n\n" +
            "Best regards,\n" +
            "Truerize Recruitment Team\n" +
            "HSR Layout, Bengaluru\n" +
            "https://www.truerize.com/"
        );
        
        mailSender.send(message);
    }
    
    private <T> List<List<T>> createBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    
    public boolean testEmailConfiguration() {
        try {
            log.info("🧪 Testing email configuration...");
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(FROM_EMAIL);
            message.setSubject("Test Email - Truerize Exam Portal");
            message.setText("This is a test email from Truerize Exam Portal. Configuration is working!");
            
            mailSender.send(message);
            log.info("✅ Test email sent successfully");
            return true;
            
        } catch (Exception e) {
            log.error("❌ Email configuration test failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
   
    public static class CandidateEmailData {
        private String email;
        private String password;
        private LocalDate slotDate;
        private LocalTime slotTime;
        private Integer slotNumber;
        
        public CandidateEmailData() {}
        
        public CandidateEmailData(String email, String password, LocalDate slotDate, 
                                 LocalTime slotTime, Integer slotNumber) {
            this.email = email;
            this.password = password;
            this.slotDate = slotDate;
            this.slotTime = slotTime;
            this.slotNumber = slotNumber;
        }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public LocalDate getSlotDate() { return slotDate; }
        public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
        public LocalTime getSlotTime() { return slotTime; }
        public void setSlotTime(LocalTime slotTime) { this.slotTime = slotTime; }
        public Integer getSlotNumber() { return slotNumber; }
        public void setSlotNumber(Integer slotNumber) { this.slotNumber = slotNumber; }
    }
    
   
    public static class BulkEmailResult {
        private int successCount;
        private int failureCount;
        private List<String> failedEmails;
        
        public BulkEmailResult(int successCount, int failureCount, List<String> failedEmails) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.failedEmails = failedEmails;
        }
        
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getFailedEmails() { return failedEmails; }
        public int getTotalCount() { return successCount + failureCount; }
        
        @Override
        public String toString() {
            return String.format("BulkEmailResult{success=%d, failed=%d, total=%d}", 
                successCount, failureCount, getTotalCount());
        }
    }
}