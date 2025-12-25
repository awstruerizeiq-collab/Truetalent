package com.truerize.entity;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(name="college_name")
    private String collegeName;

    private String email;

    private String password;

    private String status;

   
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id")
    @JsonIgnoreProperties({"users"})
    private Slot slot;

    @Transient
    @JsonProperty("slotNumber")
    private Integer slotNumberInput;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnoreProperties({"users", "hibernateLazyInitializer", "handler"})
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_assigned_exams",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "exam_id")
    )
    @JsonIgnoreProperties({"users", "questions", "proctoringRecords", "hibernateLazyInitializer", "handler"})
    private Set<Exam> assignedExams = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"user", "exam", "hibernateLazyInitializer", "handler"})
    private Set<Proctoring> proctoringRecords = new HashSet<>();

    // Constructors
    public User() {}

    public User(String name, String email, String password, String collegeName, String status) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.collegeName = collegeName;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }

    public String getCollegeName() { 
        return collegeName; 
    }
    
    public void setCollegeName(String collegeName) { 
        this.collegeName = collegeName; 
    }

    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }

    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }

    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    /**
     * Returns the slot number for JSON serialization
     * Gets it from the related Slot entity
     */
    @JsonProperty("slotNumber")
    public Integer getSlotNumber() { 
        return slot != null ? slot.getSlotNumber() : null;
    }
    
    /**
     * Receives slot number from frontend during deserialization
     * Stored temporarily in slotNumberInput for processing in service layer
     */
    @JsonProperty("slotNumber")
    public void setSlotNumber(Integer slotNumber) { 
        this.slotNumberInput = slotNumber;
    }

    /**
     * Get the transient slot number input from frontend
     */
    public Integer getSlotNumberInput() {
        return slotNumberInput;
    }

    public Set<Role> getRoles() { 
        return roles; 
    }
    
    public void setRoles(Set<Role> roles) { 
        this.roles = roles; 
    }

    public Set<Exam> getAssignedExams() { 
        return assignedExams; 
    }
    
    public void setAssignedExams(Set<Exam> assignedExams) { 
        this.assignedExams = assignedExams; 
    }

    public Set<Proctoring> getProctoringRecords() { 
        return proctoringRecords; 
    }
    
    public void setProctoringRecords(Set<Proctoring> proctoringRecords) { 
        this.proctoringRecords = proctoringRecords; 
    }
}