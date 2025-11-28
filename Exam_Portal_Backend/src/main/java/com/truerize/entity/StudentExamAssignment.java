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
    
    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
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
}
