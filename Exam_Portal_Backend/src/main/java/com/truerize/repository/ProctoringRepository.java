package com.truerize.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.Proctoring;

@Repository
public interface ProctoringRepository extends JpaRepository<Proctoring, Long> {
    // JpaRepository provides save() method for Proctoring entity
}