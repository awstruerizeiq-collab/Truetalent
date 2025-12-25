package com.truerize.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truerize.entity.Exam;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Integer> {
    
    
    boolean existsById(Integer id);
    
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.questions WHERE e.id = :id")
    Optional<Exam> findByIdWithQuestions(@Param("id") Integer id);
    
   
    Optional<Exam> findByTitle(String title);
    
    
    boolean existsByTitleIgnoreCase(String title);
}