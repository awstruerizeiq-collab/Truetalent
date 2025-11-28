package com.truerize.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.Role;
import com.truerize.entity.User;
import com.truerize.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {

        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Email is required"));
            }
            
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Password is required"));
            }
            
            Optional<User> userOptional = userService.authenticate(
                request.getEmail().trim(), 
                request.getPassword()
            );
            
            if (userOptional.isEmpty()) {
                log.warn("❌ Invalid credentials for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid email or password"));
            }
            
            User user = userOptional.get();
            
            if ("Blocked".equalsIgnoreCase(user.getStatus())) {
                log.warn("❌ Blocked user attempted login: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "Your account has been blocked."));
            }
            
            String role = "CANDIDATE";
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                role = user.getRoles().stream()
                    .findFirst()
                    .map(Role::getName)
                    .orElse("CANDIDATE");
            }
            
            session.setAttribute("userId", String.valueOf(user.getId()));
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", role);
            session.setAttribute("userName", user.getName());
            session.setAttribute("loginTime", System.currentTimeMillis());
            
//            log.info("✅ LOGIN SUCCESSFUL - User: {}, Role: {}", user.getId(), role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("role", role);
            response.put("status", user.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        try {
            Object userIdObj = session.getAttribute("userId");
            
            if (userIdObj == null) {
                log.warn("❌ No active session found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No active session found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userIdObj);
            response.put("email", session.getAttribute("email"));
            response.put("role", session.getAttribute("role"));
            response.put("name", session.getAttribute("userName"));
            response.put("sessionId", session.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get current user"));
        }
    }

   
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpSession session, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        try {
            
            Object userId = session.getAttribute("userId");
            Object userName = session.getAttribute("userName");
            Object email = session.getAttribute("email");
            Object role = session.getAttribute("role");
            Object loginTime = session.getAttribute("loginTime");
            
            if (userId != null) {
                log.info("📋 Logging out user:");
                log.info("   - User ID: {}", userId);
                log.info("   - Name: {}", userName);
                log.info("   - Email: {}", email);
                log.info("   - Role: {}", role);
                log.info("   - Session ID: {}", session.getId());
                
                if (loginTime != null) {
                    long sessionDuration = (System.currentTimeMillis() - (Long)loginTime) / 1000;
                    log.info("   - Session Duration: {} seconds ({} minutes)", 
                        sessionDuration, sessionDuration / 60);
                }
            }
            
            
            session.invalidate();
            log.info("✅ Session invalidated");
            
            
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                log.info("🍪 Clearing {} cookies", cookies.length);
                for (Cookie cookie : cookies) {
                    Cookie clearCookie = new Cookie(cookie.getName(), "");
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    clearCookie.setHttpOnly(true);
                    response.addCookie(clearCookie);
                    log.info("   - Cleared: {}", cookie.getName());
                }
            }
            
            
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
       
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Logged out successfully - all data cleared"
            ));
            
        } catch (IllegalStateException e) {
            log.warn("⚠️ Session already invalidated");
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Session already invalidated"
            ));
            
        } catch (Exception e) {
            log.error("❌ Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Logout failed"));
        }
    }

    @PostMapping("/addCandidate")
    public ResponseEntity<?> addCandidate(@RequestBody User user) {
        try {
            log.info("📋 Adding candidate: {}", user.getEmail());
            
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Email is required"));
            }
            
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Password is required"));
            }
            
            User newUser = userService.createUser(user);
            
            log.info("✅ Candidate added: {}", newUser.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Candidate added successfully",
                "user", newUser
            ));
        } catch (Exception e) {
            log.error("❌ Error adding candidate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to add candidate"));
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            boolean exists = userService.existsByEmail(email);
            return ResponseEntity.ok(Map.of("exists", exists, "email", email));
        } catch (Exception e) {
            log.error("❌ Error checking email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check email"));
        }
    }
    
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}