package com.truerize.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;

@Entity
@Table(name = "exam")
public class Exam {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column(nullable = false, unique = true)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private int duration; // in minutes
    
    @Column(name = "total_marks")
    private Integer totalMarks;
    
    @Column(name = "passing_marks")
    private Integer passingMarks;
    
    @Column(length = 20)
    private String status = "Active"; 
    
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<Question> questions = new ArrayList<>();
    
   
    @ManyToMany(mappedBy = "assignedExams", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<User> users = new HashSet<>();
    
    
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Proctoring> proctoringRecords = new HashSet<>();
    
   
    public Exam() {}
    
    public Exam(String title, String description, int duration) {
        this.title = title;
        this.description = description;
        this.duration = duration;
    }
    
  
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public Integer getTotalMarks() {
        return totalMarks;
    }
    
    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }
    
    public Integer getPassingMarks() {
        return passingMarks;
    }
    
    public void setPassingMarks(Integer passingMarks) {
        this.passingMarks = passingMarks;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<Question> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<Question> questions) {
       
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.clear();
        if (questions != null) {
            this.questions.addAll(questions);
           
            questions.forEach(q -> q.setExam(this));
        }
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }
    
    public Set<Proctoring> getProctoringRecords() {
        return proctoringRecords;
    }
    
    public void setProctoringRecords(Set<Proctoring> proctoringRecords) {
        this.proctoringRecords = proctoringRecords;
    }
    
  
    public void addUser(User user) {
        this.users.add(user);
        user.getAssignedExams().add(this);
    }
    
    public void removeUser(User user) {
        this.users.remove(user);
        user.getAssignedExams().remove(this);
    }
    
    public void addQuestion(Question question) {
        this.questions.add(question);
        question.setExam(this);
    }
    
    public void removeQuestion(Question question) {
        this.questions.remove(question);
        question.setExam(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam)) return false;
        Exam exam = (Exam) o;
        return id == exam.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}