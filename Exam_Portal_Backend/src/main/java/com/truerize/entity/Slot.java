package com.truerize.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "slots")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "slot_number", nullable = false, unique = true)
    private Integer slotNumber;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "slot_password")
    private String slotPassword;

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"slot", "roles", "assignedExams", "proctoringRecords"})
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"slot", "details"})
    private List<Result> results = new ArrayList<>();

    public Slot() {}

    public Slot(Integer slotNumber, LocalDate date, LocalTime time) {
        this.slotNumber = slotNumber;
        this.date = date;
        this.time = time;
    }

    public Slot(Integer slotNumber, String collegeName, LocalDate date, LocalTime time) {
        this.slotNumber = slotNumber;
        this.collegeName = collegeName;
        this.date = date;
        this.time = time;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getSlotNumber() { return slotNumber; }
    public void setSlotNumber(Integer slotNumber) { this.slotNumber = slotNumber; }
    
    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getSlotPassword() { return slotPassword; }
    public void setSlotPassword(String slotPassword) { this.slotPassword = slotPassword; }
    
    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }
    
    public List<Result> getResults() { return results; }
    public void setResults(List<Result> results) { this.results = results; }

    @Override
    public String toString() {
        return "Slot{id=" + id + ", slotNumber=" + slotNumber + 
               ", collegeName='" + collegeName + "', date=" + date + ", time=" + time +
               ", hasPassword=" + (slotPassword != null && !slotPassword.isBlank()) + '}';
    }
}
