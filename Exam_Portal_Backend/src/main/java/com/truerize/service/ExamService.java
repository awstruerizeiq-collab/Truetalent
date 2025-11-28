package com.truerize.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.truerize.entity.Exam;
import com.truerize.repository.ExamRepository;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepo;
 
   public Exam createExam(Exam exam){
	 return  examRepo.save(exam);
   }
	

	public void deleteExam(int id) {
		examRepo.deleteById(id);
		
	}

	public List<Exam> findAllExams() {
		return  examRepo.findAll();
	
	}
	
	 public Optional<Exam> findExam(int id) {
	        return examRepo.findById(id);
	    }

	public Exam updateExam(int id, Exam examDetails) {
		Exam existingExam = examRepo.findById(id).orElseThrow(()-> new RuntimeException("the exam is not found"));
		existingExam.setDuration(examDetails.getDuration());
		existingExam.setTitle(examDetails.getTitle());
		
		return examRepo.save(existingExam);
	}

}
