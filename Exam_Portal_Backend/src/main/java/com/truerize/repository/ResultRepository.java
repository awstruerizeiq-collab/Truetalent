package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.Result;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    
    Optional<Result> findById(Long id);
    
    void deleteById(Long id);
    
    List<Result> findByStatus(String status);
    
    Optional<Result> findByEmail(String email);
    
    long countByStatus(String status);
    
    boolean existsById(Long id);
    
    List<Result> findAllByOrderByScoreDesc();
    
    List<Result> findByScoreGreaterThanEqual(int score);
    
    List<Result> findByScoreLessThan(int score);
}