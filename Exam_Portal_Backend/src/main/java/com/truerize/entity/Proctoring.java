package com.truerize.entity;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "proctoring")
public class Proctoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relationship with Exam
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(length = 255)
    private String photoUrl;

    @Column(length = 255)
    private String idProofUrl;

    @Column(columnDefinition = "boolean default true")
    private boolean cameraEnabled = true;
    
    @Column(columnDefinition = "boolean default true")
    private boolean microphoneEnabled = true;
    
    @Column(columnDefinition = "boolean default true")
    private boolean screenSharingEnabled = true;

    private LocalDateTime createdAt;

    public Proctoring() {
        this.createdAt = LocalDateTime.now();
        this.cameraEnabled = true;
        this.microphoneEnabled = true;
        this.screenSharingEnabled = true;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Exam getExam() {
		return exam;
	}

	public void setExam(Exam exam) {
		this.exam = exam;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public String getIdProofUrl() {
		return idProofUrl;
	}

	public void setIdProofUrl(String idProofUrl) {
		this.idProofUrl = idProofUrl;
	}

	public boolean isCameraEnabled() {
		return cameraEnabled;
	}

	public void setCameraEnabled(boolean cameraEnabled) {
		this.cameraEnabled = cameraEnabled;
	}

	public boolean isMicrophoneEnabled() {
		return microphoneEnabled;
	}

	public void setMicrophoneEnabled(boolean microphoneEnabled) {
		this.microphoneEnabled = microphoneEnabled;
	}

	public boolean isScreenSharingEnabled() {
		return screenSharingEnabled;
	}

	public void setScreenSharingEnabled(boolean screenSharingEnabled) {
		this.screenSharingEnabled = screenSharingEnabled;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    // Getters and Setters
    // ...
    
}