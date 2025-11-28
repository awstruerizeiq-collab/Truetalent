package com.truerize.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.truerize.entity.Proctoring;
import com.truerize.repository.ProctoringRepository;

@Service
public class ProctoringService {
    
    @Autowired
    private ProctoringRepository proctoringRepository;
    
    public Proctoring saveProctoring(Proctoring proctoring) {
        return proctoringRepository.save(proctoring);
    }
}