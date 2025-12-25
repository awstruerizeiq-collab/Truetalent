package com.truerize.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
public class Result {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private String name;
    
    @Column(name="college_name")
    private String collegeName;
    
    private String email;
    
    private String exam;
    
    private Integer score;
    
    private String status;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id")
    @JsonIgnoreProperties({"users", "results"})
    private Slot slot;
    
    public Result() {}
  
    public Result(int id, String name, String collegeName, String email, String exam, Integer score, String status) {
        this.id = id;
        this.name = name;
        this.collegeName = collegeName;
        this.email = email;
        this.exam = exam;
        this.score = score;
        this.status = status;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getExam() { return exam; }
    public void setExam(String exam) { this.exam = exam; }
    
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Slot getSlot() { return slot; }
    public void setSlot(Slot slot) { this.slot = slot; }
}
