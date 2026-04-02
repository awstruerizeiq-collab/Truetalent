package com.truerize.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.entity.StudentExamAssignment;
import com.truerize.repository.ExamQuestionSetRepo;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.ProctoringRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.StudentExamAssignmentRepo;
import com.truerize.repository.TestSubmissionRepository;

@Service
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    @Autowired
    private StudentExamAssignmentRepo studentExamAssignmentRepo;
    
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private TestSubmissionRepository testSubmissionRepository;

    @Autowired
    private ProctoringRepository proctoringRepository;

    @Autowired
    private ExamQuestionSetRepo examQuestionSetRepo;

    @Autowired
    private QuestionRepository questionRepository;

  
    @Transactional
    public void startExam(String studentId, Integer examId) {
        log.info("🚀 Starting exam {} for student {}", examId, studentId);
        
        Optional<StudentExamAssignment> assignmentOpt = 
            studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, examId);
        
        if (assignmentOpt.isEmpty()) {
            log.error("❌ No exam assignment found for student: {}, exam: {}", studentId, examId);
            throw new IllegalStateException("No exam assignment found");
        }
        
        StudentExamAssignment assignment = assignmentOpt.get();
        
        if (!assignment.getHasStarted()) {
            assignment.setHasStarted(true);
            assignment.setStartedAt(LocalDateTime.now());
            studentExamAssignmentRepo.save(assignment);
            log.info("✅ Exam started successfully");
        }
    }

    @Transactional
    public void completeExam(String studentId, Integer examId) {
        log.info("🏁 Completing exam {} for student {}", examId, studentId);
        
        Optional<StudentExamAssignment> assignmentOpt = 
            studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, examId);
        
        if (assignmentOpt.isEmpty()) {
            log.error("❌ No exam assignment found");
            throw new IllegalStateException("No exam assignment found");
        }
        
        StudentExamAssignment assignment = assignmentOpt.get();
        
        if (!assignment.getHasCompleted()) {
            assignment.setHasCompleted(true);
            assignment.setCompletedAt(LocalDateTime.now());
            studentExamAssignmentRepo.save(assignment);
            log.info("✅ Exam completed successfully");
        }
    }

    public Optional<StudentExamAssignment> getStudentExamAssignment(String studentId, Integer examId) {
        return studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, examId);
    }

    public List<Exam> findAllExams() {
        log.info("📚 Fetching all exams from 'exam' table");
        List<Exam> exams = examRepository.findAll();
        log.info("✅ Found {} exams", exams.size());
        return exams;
    }

    public Optional<Exam> findExam(int id) {
        log.info("🔍 Fetching exam with id: {} from 'exam' table", id);
        return examRepository.findById(id);
    }

    @Transactional
    public Exam createExam(Exam exam) {
        log.info("➕ Creating new exam: {}", exam.getTitle());
        
        try {
           
            if (exam.getTitle() == null || exam.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Exam title is required");
            }
            
            if (exam.getDuration() <= 0) {
                throw new IllegalArgumentException("Exam duration must be greater than 0");
            }
            
           
            if (examRepository.existsByTitleIgnoreCase(exam.getTitle())) {
                throw new IllegalArgumentException("Exam with title '" + exam.getTitle() + "' already exists");
            }
            
           
            if (exam.getStatus() == null || exam.getStatus().trim().isEmpty()) {
                exam.setStatus("Active");
            }
            
           
            if (exam.getQuestions() == null) {
                exam.setQuestions(new java.util.ArrayList<>());
            }
            
           
            if (!exam.getQuestions().isEmpty()) {
                log.info("   Processing {} questions", exam.getQuestions().size());
                
                int questionNumber = 1;
                for (Question question : exam.getQuestions()) {
                   
                    question.setExam(exam);
                    
                  
                    if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Question text is required for all questions");
                    }
                    
                    if (question.getSection() == null || question.getSection().trim().isEmpty()) {
                        throw new IllegalArgumentException("Section is required for all questions");
                    }
                    
                   
                    if (question.getType() == null || question.getType().trim().isEmpty()) {
                        throw new IllegalArgumentException("Type is required for all questions");
                    }
                    
                  
                    if (question.getqNo() == null || question.getqNo() <= 0) {
                        question.setqNo(questionNumber++);
                    }
                    
                  
                    if (question.getMarks() == null || question.getMarks() <= 0) {
                        question.setMarks(1);
                    }
                    
                   
                    if (question.getType().equalsIgnoreCase("MCQ")) {
                        if (question.getOptions() == null || question.getOptions().isEmpty()) {
                            throw new IllegalArgumentException("MCQ questions must have options");
                        }
                        if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
                            throw new IllegalArgumentException("MCQ questions must have an answer");
                        }
                    }
                }
            }
            
          
            if (exam.getTotalMarks() == null && !exam.getQuestions().isEmpty()) {
                int totalMarks = exam.getQuestions().stream()
                    .mapToInt(q -> q.getMarks() != null ? q.getMarks() : 0)
                    .sum();
                exam.setTotalMarks(totalMarks);
                log.info("   Calculated total marks: {}", totalMarks);
            }
            
        
            Exam savedExam = examRepository.save(exam);
            
            log.info("✅ Created exam successfully in 'exam' table!");
            log.info("   ID: {}", savedExam.getId());
            log.info("   Title: {}", savedExam.getTitle());
            log.info("   Duration: {} minutes", savedExam.getDuration());
            log.info("   Questions: {}", savedExam.getQuestions().size());
            log.info("   Total Marks: {}", savedExam.getTotalMarks());
            log.info("   Status: {}", savedExam.getStatus());
            
            return savedExam;
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error creating exam: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create exam: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Exam updateExam(int id, Exam examDetails) {
        log.info("✏️ Updating exam with id: {} in 'exam' table", id);
        
        try {
            Exam exam = examRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));

            if (examDetails.getTitle() != null && !examDetails.getTitle().trim().isEmpty()) {
              
                Optional<Exam> existingExam = examRepository.findByTitle(examDetails.getTitle());
                if (existingExam.isPresent() && existingExam.get().getId() != id) {
                    throw new IllegalArgumentException("Exam with title '" + examDetails.getTitle() + "' already exists");
                }
                exam.setTitle(examDetails.getTitle());
            }
            
            if (examDetails.getDescription() != null) {
                exam.setDescription(examDetails.getDescription());
            }
            
            if (examDetails.getDuration() > 0) {
                exam.setDuration(examDetails.getDuration());
            }
            
            if (examDetails.getTotalMarks() != null) {
                exam.setTotalMarks(examDetails.getTotalMarks());
            }
            
            if (examDetails.getPassingMarks() != null) {
                exam.setPassingMarks(examDetails.getPassingMarks());
            }
            
            if (examDetails.getStatus() != null) {
                exam.setStatus(examDetails.getStatus());
            }
           
            if (examDetails.getQuestions() != null) {
                log.info("   Updating questions: {} new questions", examDetails.getQuestions().size());
               
                exam.getQuestions().clear();
                
                
                int questionNumber = 1;
                for (Question question : examDetails.getQuestions()) {
                    question.setExam(exam);
                    
                   
                    if (question.getqNo() == null || question.getqNo() <= 0) {
                        question.setqNo(questionNumber++);
                    }
                    
                    exam.getQuestions().add(question);
                }
            }

            Exam savedExam = examRepository.save(exam);
            log.info("✅ Updated exam: {}", savedExam.getTitle());
            
            return savedExam;
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error updating exam: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update exam: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteExam(int id) {
        log.info("🗑️ Deleting exam with id: {} from 'exam' table", id);
        
        try {
            Exam exam = examRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));

           
            int deletedAssignments = studentExamAssignmentRepo.deleteByExamId(id);
            log.info("   Deleted {} student assignments", deletedAssignments);

            int deletedSubmissions = testSubmissionRepository.deleteByExamId(id);
            log.info("   Deleted {} test submissions", deletedSubmissions);

            int deletedProctoring = proctoringRepository.deleteByExamId(id);
            log.info("   Deleted {} proctoring records", deletedProctoring);

            Integer deletedQuestionSets = examQuestionSetRepo.deleteByExamId(id);
            log.info("   Deleted {} exam question sets", deletedQuestionSets != null ? deletedQuestionSets : 0);

            int deletedAssignedUserMappings = examRepository.deleteAssignedUserMappings(id);
            log.info("   Deleted {} user-exam mappings", deletedAssignedUserMappings);

            questionRepository.deleteByExamId(id);
            log.info("   Deleted question rows for exam {}", id);

            examRepository.delete(exam);
            
            log.info("✅ Deleted exam: {}", exam.getTitle());
            
        } catch (Exception e) {
            log.error("❌ Error deleting exam: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete exam: " + e.getMessage(), e);
        }
    }
    
  
    public boolean examExists(Integer examId) {
        boolean exists = examRepository.existsById(examId);
        log.info("   Exam {} existence check in 'exam' table: {}", examId, exists);
        return exists;
    }
}
