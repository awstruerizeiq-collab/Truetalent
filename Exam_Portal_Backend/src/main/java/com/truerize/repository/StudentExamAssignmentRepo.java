package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.StudentExamAssignment;

@Repository
public interface StudentExamAssignmentRepo extends JpaRepository<StudentExamAssignment, Long> {
    
    Optional<StudentExamAssignment> findByStudentIdAndExamId(String studentId, int examId);
    
    List<StudentExamAssignment> findByExamId(int examId);
    
    long countByExamId(int examId);
    
    long countByExamIdAndAssignedSetNumber(int examId, int setNumber);
    
    int deleteByExamId(int examId);
}