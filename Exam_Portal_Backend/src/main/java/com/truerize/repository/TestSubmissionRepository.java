package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TestSubmission ts WHERE ts.user.id = :userId")
    int deleteByUserId(@Param("userId") Integer userId);
	TestSubmission save(TestSubmission submission);
}
