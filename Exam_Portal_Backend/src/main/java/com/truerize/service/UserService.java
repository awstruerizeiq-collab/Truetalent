package com.truerize.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Exam;
import com.truerize.entity.Role;
import com.truerize.entity.Slot;
import com.truerize.entity.User;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.RoleRepository;
import com.truerize.repository.SlotRepository;
import com.truerize.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private MailService mailService;

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
    public User createUser(User user) {
        log.info("➕ Creating new user: {}", user.getEmail());
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("Active");
        }
        
        
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role candidateRole = roleRepository.findByName("CANDIDATE")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("CANDIDATE");
                    return roleRepository.save(newRole);
                });
            user.setRoles(Set.of(candidateRole));
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

    public Optional<User> authenticate(String email, String password) {
        return userRepository.findByEmailAndPassword(email, password);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
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
            String newEmail = userDetails.getEmail().trim();
           
            if (!newEmail.equalsIgnoreCase(originalEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
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
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        userRepository.delete(user);
        log.info("✅ Deleted user: {}", user.getEmail());
    }
}