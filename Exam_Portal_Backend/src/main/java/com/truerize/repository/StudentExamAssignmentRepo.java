package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.StudentExamAssignment;

@Repository
public interface StudentExamAssignmentRepo extends JpaRepository<StudentExamAssignment, Long> {
  
    Optional<StudentExamAssignment> findByStudentIdAndExamId(String studentId, int examId);
   
    List<StudentExamAssignment> findByExamId(int examId);
  
    long countByExamId(int examId);
   
    long countByExamIdAndAssignedSetNumber(int examId, int setNumber);
  
    @Transactional
    @Modifying
    @Query("DELETE FROM StudentExamAssignment s WHERE s.examId = :examId")
    int deleteByExamId(@Param("examId") int examId);
   
    List<StudentExamAssignment> findByStudentIdAndHasCompletedTrue(String studentId);
   
    List<StudentExamAssignment> findByStudentIdAndHasStartedTrueAndHasCompletedFalse(String studentId);
    
    Optional<StudentExamAssignment> findByStudentIdAndExamIdAndHasCompletedTrue(String studentId, int examId);
   
    Optional<StudentExamAssignment> findByStudentIdAndExamIdAndHasStartedTrueAndHasCompletedFalse(String studentId, int examId);
   
    boolean existsByStudentIdAndExamId(String studentId, int examId);

    Optional<StudentExamAssignment> findTopByStudentIdAndHasStartedTrueAndHasCompletedFalseOrderByAssignedAtDesc(String studentId);

    Optional<StudentExamAssignment> findTopByStudentIdAndHasCompletedFalseOrderByAssignedAtDesc(String studentId);

    List<StudentExamAssignment> findByStudentIdOrderByAssignedAtDesc(String studentId);
}
