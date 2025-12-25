package com.truerize.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_exam_assignments")
public class StudentExamAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String studentId;
    
    @Column(nullable = false)
    private int examId;
    
    @Column(nullable = false)
    private Integer assignedSetNumber;
    
    @Column(nullable = false)
    private Integer slotNumber;
    
    @Column(nullable = false)
    private LocalDateTime assignedAt;
    
    @Column(nullable = false)
    private Boolean hasStarted = false;
    
    private LocalDateTime startedAt;
    
    @Column(nullable = false)
    private Boolean hasCompleted = false;
    
    private LocalDateTime completedAt;
    
    // NEW FIELDS FOR ONE-TIME EXAM CONTROL
    @Column(name = "session_id", length = 500)
    private String sessionId;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;
    
    @Column(name = "is_exam_locked", nullable = false)
    private Boolean isExamLocked = false;
    
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
    
    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        if (loginCount == null) loginCount = 0;
        if (isExamLocked == null) isExamLocked = false;
        if (hasStarted == null) hasStarted = false;
        if (hasCompleted == null) hasCompleted = false;
    }
    
   
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public int getExamId() {
        return examId;
    }
    
    public void setExamId(int examId) {
        this.examId = examId;
    }
    
    public Integer getAssignedSetNumber() {
        return assignedSetNumber;
    }
    
    public void setAssignedSetNumber(Integer assignedSetNumber) {
        this.assignedSetNumber = assignedSetNumber;
    }
    
    public Integer getSlotNumber() {
        return slotNumber;
    }
    
    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public Boolean getHasStarted() {
        return hasStarted;
    }
    
    public void setHasStarted(Boolean hasStarted) {
        this.hasStarted = hasStarted;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public Boolean getHasCompleted() {
        return hasCompleted;
    }
    
    public void setHasCompleted(Boolean hasCompleted) {
        this.hasCompleted = hasCompleted;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    // NEW GETTERS AND SETTERS
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public Integer getLoginCount() {
        return loginCount;
    }
    
    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }
    
    public Boolean getIsExamLocked() {
        return isExamLocked;
    }
    
    public void setIsExamLocked(Boolean isExamLocked) {
        this.isExamLocked = isExamLocked;
    }
    
    public LocalDateTime getLockedAt() {
        return lockedAt;
    }
    
    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }
}