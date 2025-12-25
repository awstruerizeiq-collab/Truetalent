package com.truerize.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Proctoring;

@Repository
public interface ProctoringRepository extends JpaRepository<Proctoring, Long> {
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Proctoring p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}