package com.truerize.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Exam;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ExamSetService examSetService;
    
    @Autowired
    private MailService mailService;

    public Optional<User> authenticate(String email, String password) {
        log.info("Authenticating user: {}", email);
        
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                log.warn("❌ User not found: {}", email);
                return Optional.empty();
            }
            
            User user = userOpt.get();
            
            if (user.getPassword().equals(password)) {
                log.info("Authentication successful for user: {}", email);
                return Optional.of(user);
            } else {
                log.warn("❌ Invalid password for user: {}", email);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("❌ Error during authentication", e);
            return Optional.empty();
        }
    }

    public Optional<User> findByEmailAndPassword(String email, String password) {
        return authenticate(email, password);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }
   
    public Optional<User> findUser(int id) {
        return userRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
   
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User createUser(User user) {
        log.info(" Creating user: {}", user.getEmail());
        
        if (existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("Active");
        }
        
        User savedUser = userRepository.save(user);
        log.info("✅ User created with ID: {}", savedUser.getId());
        
        return savedUser;
    }

    @Transactional
    public User updateUser(int id, User userDetails) {
        log.info(" Updating user: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
        if (userDetails.getName() != null) {
            user.setName(userDetails.getName());
        }
        if (userDetails.getEmail() != null) {
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }
        if (userDetails.getCollegeName() != null) {
            user.setCollegeName(userDetails.getCollegeName());
        }
        if (userDetails.getStatus() != null) {
            user.setStatus(userDetails.getStatus());
        }
        if (userDetails.getRoles() != null) {
            user.setRoles(userDetails.getRoles());
        }
        if (userDetails.getSlotNumber() != null) {
            user.setSlotNumber(userDetails.getSlotNumber());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("✅ User updated: {}", updatedUser.getId());
        
        return updatedUser;
    }

    @Transactional
    public void deleteUser(int id) {
        log.info(" Deleting user: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        
        userRepository.deleteById(id);
        log.info("✅ User deleted: {}", id);
    }

   
    @Transactional
    public User blockUser(int id) {
        log.info(" Blocking user: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
        user.setStatus("Blocked");
        User blockedUser = userRepository.save(user);
        
        log.info("✅ User blocked: {}", blockedUser.getId());
        return blockedUser;
    }

    @Transactional
    public User unblockUser(int id) {
        log.info("✅ Unblocking user: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
        user.setStatus("Active");
        User unblockedUser = userRepository.save(user);
        
        log.info("✅ User unblocked: {}", unblockedUser.getId());
        return unblockedUser;
    }

   
    public List<User> getUsersByRole(String roleName) {
        return userRepository.findByRoles_Name(roleName);
    }

    public List<User> getUsersByStatus(String status) {
        return userRepository.findByStatus(status);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public long countUsersByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    @Transactional
    public void assignExamToUsers(Integer examId, List<Integer> userIds) {
       
        log.info("Exam ID: {}", examId);
        log.info("User IDs: {}", userIds);

        try {
          
            Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));

            log.info("✅ Exam found: {}", exam.getTitle());

           
            if (!examSetService.hasQuestionSets(examId.longValue())) {
                log.info("⚠️ Question sets not found, generating...");
                examSetService.generateQuestionSets(examId.longValue());
                log.info("✅ Question sets generated");
            } else {
                log.info("✅ Question sets already exist");
            }

            int setCounter = 1;
            for (Integer userId : userIds) {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

                user.getAssignedExams().add(exam);
                user.setStatus("Assigned Exam");
                userRepository.save(user);

                
                int setNumber = ((setCounter - 1) % 5) + 1;
                examSetService.assignStudentToSet(String.valueOf(userId), examId.longValue(), setNumber, setCounter);
                
                log.info("✅ Assigned user {} to exam {} with set {}", userId, examId, setNumber);
                
              
                try {
                    String examLink = "http://localhost:3000/exam/" + examId;
                    mailService.sendExamAssignedEmail(
                        user.getEmail(), 
                        user.getPassword(), 
                        examLink
                    );
                    log.info(" Email sent to: {}", user.getEmail());
                } catch (Exception emailError) {
                    log.error("❌ Failed to send email to {}: {}", user.getEmail(), emailError.getMessage());
                   
                }
                
                setCounter++;
            }

            log.info("========================================");
            log.info("✅ ASSIGNMENT COMPLETE - {} users assigned", userIds.size());
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Error assigning exam to users", e);
            throw new RuntimeException("Failed to assign exam: " + e.getMessage());
        }
    }
}