package com.truerize.dto;

public class DashboardStatistics {

    private long totalUsers;
    private long totalExams;
    private long totalQuestions;
    private long examsCompleted;

    
    public DashboardStatistics() {
    }

    public DashboardStatistics(long totalUsers, long totalExams, long totalQuestions, long examsCompleted) {
        this.totalUsers = totalUsers;
        this.totalExams = totalExams;
        this.totalQuestions = totalQuestions;
        this.examsCompleted = examsCompleted;
    }

   
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalExams() {
        return totalExams;
    }

    public void setTotalExams(long totalExams) {
        this.totalExams = totalExams;
    }

    public long getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public long getExamsCompleted() {
        return examsCompleted;
    }

    public void setExamsCompleted(long examsCompleted) {
        this.examsCompleted = examsCompleted;
    }
}
