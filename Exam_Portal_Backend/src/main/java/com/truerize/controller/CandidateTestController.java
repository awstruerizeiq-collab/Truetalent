package com.truerize.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.TestSubmission;
import com.truerize.service.TestSubmissionService;

@RestController
@RequestMapping("/api/candidate")
@CrossOrigin(origins = "http://localhost:3000")
public class CandidateTestController {

    @Autowired
    private TestSubmissionService testSubmissionService;

    @PostMapping("/submit-exam")
    public TestSubmission submitExam(@RequestBody TestSubmission submission) {
        return testSubmissionService.submitTest(submission);
    }
}