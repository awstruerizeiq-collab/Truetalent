package com.truerize.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.StoredFile;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
