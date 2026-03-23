package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.TestSubmission;

@Repository
public interface TestSubmissionRepository extends JpaRepository<TestSubmission, Integer> {
	List<TestSubmission> findAll();
    List<TestSubmission> findByUserId(int userId);
    List<TestSubmission> findByExamId(int examId);
    Optional<TestSubmission> findTopByUser_IdAndExam_IdOrderBySubmittedAtDesc(Integer userId, Integer examId);
    Optional<TestSubmission> findTopByUser_IdAndExam_IdAndUser_Slot_IdOrderBySubmittedAtDesc(Integer userId, Integer examId, Integer slotId);
    Optional<TestSubmission> findTopByUser_EmailIgnoreCaseAndExam_TitleIgnoreCaseOrderBySubmittedAtDesc(String email, String examTitle);
	TestSubmission save(TestSubmission submission);
}
