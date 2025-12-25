package com.truerize.dto;

public class LoginRequest {

    private String email;
    private String password;
    private Integer examId;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getExamId() { return examId; }
    public void setExamId(Integer examId) { this.examId = examId; }
}
