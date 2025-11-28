package com.truerize.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.TestSubmission;

@Repository
public interface TestSubmissionRepository extends JpaRepository<TestSubmission, Integer> {
	List<TestSubmission> findAll();
    List<TestSubmission> findByUserId(int userId);
    List<TestSubmission> findByExamId(int examId);
	TestSubmission save(TestSubmission submission);
}
