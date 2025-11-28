package com.truerize.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.Exam;
@Repository
public interface ExamRepository extends JpaRepository<Exam,Integer> {

	Optional<Exam> findById(Long examId);
	

}
