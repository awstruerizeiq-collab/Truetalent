package com.truerize.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.User;
import com.truerize.service.UserService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching all users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable int id) {
        try {
            Optional<User> user = userService.findUser(id);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            log.error("Error fetching user with id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch user"));
        }
    }

   
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            log.info("Creating user: {}", user.getName());
            User newUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody User userDetails) {
        try {
            log.info("Updating user: {}", id);
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error updating user: " + id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update user"));
        }
    }

   
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        try {
            log.info("Deleting user: {}", id);
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
        }
    }

 
    @PostMapping("/assign-exam")
    public ResponseEntity<?> assignExamToUsers(@RequestBody AssignExamRequest request) {
        log.info("========================================");
        log.info("📋 REQUEST: Assign Exam to Users");
        log.info("========================================");
        
        try {
            if (request.getExamId() == null || request.getExamId() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Valid examId is required"));
            }
            
            if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "userIds list cannot be empty"));
            }
            
            log.info("📋 Exam ID: {}", request.getExamId());
            log.info("👥 User IDs: {}", request.getUserIds());
            
            userService.assignExamToUsers(request.getExamId(), request.getUserIds());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Exam assigned successfully to " + request.getUserIds().size() + " user(s)");
            response.put("examId", request.getExamId());
            response.put("assignedUsers", request.getUserIds().size());
            
            log.info("✅ SUCCESS: Exam assigned to {} users", request.getUserIds().size());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Runtime Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected Error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to assign exam: " + e.getMessage()));
        }
    }

    
    @PostMapping("/addCandidate")
    public ResponseEntity<?> addCandidate(@RequestBody User user) {
        try {
            log.info("Adding candidate: {}", user.getName());
            User newUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (Exception e) {
            log.error("Error adding candidate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to add candidate: " + e.getMessage()));
        }
    }

    
    public static class AssignExamRequest {
        private Integer examId;
        private List<Integer> userIds;

        public Integer getExamId() {
            return examId;
        }

        public void setExamId(Integer examId) {
            this.examId = examId;
        }

        public List<Integer> getUserIds() {
            return userIds;
        }

        public void setUserIds(List<Integer> userIds) {
            this.userIds = userIds;
        }

        @Override
        public String toString() {
            return "AssignExamRequest{examId=" + examId + ", userIds=" + userIds + "}";
        }
    }
}