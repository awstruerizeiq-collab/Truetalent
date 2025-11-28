package com.truerize.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.truerize.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByExam_IdOrderByQNoAsc(int examId);

	List<Question> findByExamId(int id);
}
