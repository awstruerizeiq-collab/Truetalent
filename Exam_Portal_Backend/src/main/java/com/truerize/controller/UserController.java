package com.truerize.controller;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.entity.User;
import com.truerize.service.MailService;
import com.truerize.service.UserService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.findAll();
            log.info("Retrieved {} users", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable int id) {
        try {
            Optional<User> user = userService.findById(id);
            
            if (user.isPresent()) {
                log.info("Retrieved user with id: {}", id);
                return ResponseEntity.ok(user.get());
            } else {
                log.warn("User not found with id: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            log.error("Error fetching user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User newUser = userService.createUser(user);
            log.info("Created new user with id: {}", newUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create user"));
        }
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<?> uploadUsersFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("slotId") Integer slotId) {
        try {
            Map<String, Object> result = userService.importUsersFromExcel(file, slotId);
            boolean hasFailures = ((Integer) result.getOrDefault("failedCount", 0)) > 0;
            return ResponseEntity.status(hasFailures ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error importing users from Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to import users from Excel"));
        }
    }

   
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            log.info("Updated user with id: {}", id);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error updating user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        } catch (Exception e) {
            log.error("Unexpected error updating user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update user"));
        }
    }

   
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        try {
            userService.deleteUser(id);
            log.info("Deleted user with id: {}", id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deleted successfully",
                "userId", id
            ));
        } catch (IllegalArgumentException e) {
            log.error("Validation error deleting user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error deleting user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        } catch (Exception e) {
            log.error("Unexpected error deleting user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete user"));
        }
    }

    
    @PostMapping("/assign-exam")
    public ResponseEntity<?> assignExamToUsers(@RequestBody Map<String, Object> request) {
        log.info("📝 Received exam assignment request: {}", request);
        
        try {
            // Validate and extract userIds
            Object userIdsObj = request.get("userIds");
            if (userIdsObj == null) {
                log.error("❌ Missing userIds in request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "userIds is required"));
            }
            
            List<Integer> userIds = new ArrayList<>();
            if (userIdsObj instanceof List) {
                for (Object id : (List<?>) userIdsObj) {
                    if (id instanceof Integer) {
                        userIds.add((Integer) id);
                    } else if (id instanceof String) {
                        try {
                            userIds.add(Integer.parseInt((String) id));
                        } catch (NumberFormatException e) {
                            log.error("❌ Invalid user ID format: {}", id);
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, 
                                    "error", "Invalid user ID format: " + id));
                        }
                    }
                }
            } else {
                log.error("❌ userIds must be a list");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "userIds must be a list"));
            }
            
            if (userIds.isEmpty()) {
                log.error("❌ No valid user IDs provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "At least one user ID is required"));
            }
            
            
            Object examIdObj = request.get("examId");
            if (examIdObj == null) {
                log.error("❌ Missing examId in request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "examId is required"));
            }
            
            Integer examId;
            try {
                if (examIdObj instanceof Integer) {
                    examId = (Integer) examIdObj;
                } else if (examIdObj instanceof String) {
                    examId = Integer.parseInt((String) examIdObj);
                } else {
                    log.error("❌ Invalid examId type: {}", examIdObj.getClass());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", "Invalid examId format"));
                }
            } catch (NumberFormatException e) {
                log.error("❌ Invalid examId format: {}", examIdObj);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Invalid examId format"));
            }
            
            log.info("✅ Validated request - userIds: {}, examId: {}", userIds, examId);
            
         
            Map<String, Object> result = userService.assignExamToUsersAndSendEmails(userIds, examId);
            
           
            if (Boolean.TRUE.equals(result.get("success"))) {
                if ((Integer) result.get("failedCount") > 0) {
                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
                } else {
                    return ResponseEntity.ok(result);
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("❌ Error assigning exam to users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to assign exam",
                    "message", e.getMessage()
                ));
        }
    }

   
    @GetMapping("/exam/{examId}")
    public ResponseEntity<?> getUsersByExam(@PathVariable Integer examId) {
        try {
            List<User> users = userService.findAll();
            List<User> assignedUsers = users.stream()
                .filter(user -> user.getAssignedExams().stream()
                    .anyMatch(exam -> exam.getId() == examId))
                .toList();
            
            log.info("Found {} users assigned to exam {}", assignedUsers.size(), examId);
            return ResponseEntity.ok(assignedUsers);
        } catch (Exception e) {
            log.error("Error fetching users for exam {}", examId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch users"));
        }
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "email is required"));
            }

            mailService.sendDirectTestEmail(email);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test email sent successfully",
                "email", email
            ));
        } catch (Exception e) {
            log.error("Error sending test email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to send test email",
                    "message", e.getMessage()
                ));
        }
    }
}
