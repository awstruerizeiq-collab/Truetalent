package com.truerize.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truerize.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    
    List<Question> findByExam_IdOrderByQNoAsc(int examId);
    
    List<Question> findByExamIdOrderByQNoAsc(int examId);
    
    List<Question> findByExamId(int examId);
    
    long countByExamId(int examId);
    
    List<Question> findByExamIdAndSectionOrderByQNoAsc(int examId, String section);
    
    List<Question> findByExamIdAndTypeOrderByQNoAsc(int examId, String type);
    
    @Query("SELECT MAX(q.qNo) FROM Question q WHERE q.exam.id = :examId")
    Integer findMaxQNoByExamId(@Param("examId") int examId);
    
    void deleteByExamId(int examId);
}