package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.ExamQuestionSet;

@Repository
public interface ExamQuestionSetRepo extends JpaRepository<ExamQuestionSet, Long> {
    
    List<ExamQuestionSet> findByExamId(Integer examId);
    
    List<ExamQuestionSet> findByExamIdAndIsActiveTrue(Integer examId);
    
    Optional<ExamQuestionSet> findByExamIdAndSetNumberAndIsActiveTrue(Integer examId, Integer setNumber);
    
    long countByExamIdAndIsActiveTrue(Integer examId);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM ExamQuestionSet eqs WHERE eqs.examId = :examId")
    Integer deleteByExamId(@Param("examId") Integer examId);
}