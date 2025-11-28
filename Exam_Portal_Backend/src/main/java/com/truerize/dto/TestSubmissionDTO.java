package com.truerize.dto;

public class TestSubmissionDTO {
    
    private UserIdDTO user;
    private ExamIdDTO exam;
    private String answersJson;
    private String videoUrl;

    public static class UserIdDTO {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public static class ExamIdDTO {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public UserIdDTO getUser() {
        return user;
    }

    public void setUser(UserIdDTO user) {
        this.user = user;
    }

    public ExamIdDTO getExam() {
        return exam;
    }

    public void setExam(ExamIdDTO exam) {
        this.exam = exam;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}