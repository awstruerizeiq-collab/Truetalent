package com.truerize.service;

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
    
    private static final String FROM_EMAIL = "bindumsbindu5@gmail.com";

    @Async
    public void sendExamAssignedEmail(String to, String password, String examLink) {
        if (to == null || to.isBlank()) {
            log.warn("⚠️ Skipping exam assignment email: invalid email");
            return;
        }
        
        try {
            log.info("Attempting to send exam assignment email to: {}", to);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject("Truerize Assessment Invitation");
            message.setText(
                "Dear Candidate,\n\n" +
                "You have been invited to participate in the Truerize online assessment.\n\n" +
                "📋 Login Details:\n" +
                "Assessment Link: " + examLink + "\n" +
                "Username (Email): " + to + "\n" +
                "Password: " + password + "\n\n" +
                "⚠️ Important Instructions:\n" +
                "• Login 10 minutes before your scheduled slot time\n" +
                "• Ensure stable internet connection\n" +
                "• Keep camera and microphone ready\n" +
                "• You will receive a unique shuffled question set based on your slot\n\n" +
                "Please complete the assessment by the specified deadline.\n\n" +
                "For any queries, contact: hr@truerize.com\n\n" +
                "Best regards,\n" +
                "Truerize Recruitment Team\n" +
                "HSR Layout, Bengaluru\n" +
                "https://www.truerize.com/"
            );
            
            mailSender.send(message);
            log.info("Exam assignment email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("❌ Failed to send exam assignment email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendResultReleasedEmail(String to) {
        if (to == null || to.isBlank()) {
            log.warn(" Skipping congratulatory email: invalid email");
            return;
        }

        try {
            log.info(" Sending congratulatory email to: {}", to);
            
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
            "Please don’t be discouraged—new opportunities arise frequently at Truerize, and we encourage you to reapply in the future as your skills and experience continue to grow.\n\n" +
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

    public boolean testEmailConfiguration() {
        try {
            log.info("Testing email configuration...");
            
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
}