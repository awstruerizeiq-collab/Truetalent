//package com.truerize.service;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
////import java.util.Random;
//import java.util.stream.Collectors;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.truerize.entity.ExamQuestionSet;
//import com.truerize.entity.Question;
//import com.truerize.entity.StudentExamAssignment;
//import com.truerize.repository.ExamQuestionSetRepo;
//import com.truerize.repository.QuestionRepository;
//import com.truerize.repository.StudentExamAssignmentRepo;
//
//import jakarta.transaction.Transactional;
//
//public List<Question> getShuffledQuestionsForStudent(String studentId, String examIdStr) {
//    int examId = Integer.parseInt(examIdStr);
//    log.info("Fetching shuffled questions for student {} in exam {}", studentId, examId);
//
//    // Fetch the student's assignment
//    StudentExamAssignment assignment = studentExamAssignmentRepo
//            .findByStudentIdAndExamId(studentId, examId)
//            .orElseThrow(() -> new IllegalStateException(
//                    "Student " + studentId + " is not assigned to exam " + examId));
//
//    // Fetch the question set assigned to this student
//    ExamQuestionSet questionSet = examQuestionSetRepo
//            .findByExamIdAndSetNumberAndIsActiveTrue(examId, assignment.getAssignedSetNumber())
//            .orElseThrow(() -> new IllegalStateException(
//                    "Question set " + assignment.getAssignedSetNumber() + " not found for exam " + examId));
//
//    // Convert List<String> IDs to List<Integer> for repository
//    List<Integer> questionIds = questionSet.getQuestionIds().stream()
//            .map(Integer::parseInt)
//            .collect(Collectors.toList());
//
//    // Fetch questions from repository
//    List<Question> questions = questionRepository.findAllById(questionIds);
//
//    // Map questions by ID for ordering
//    Map<Integer, Question> questionMap = questions.stream()
//            .collect(Collectors.toMap(Question::getId, q -> q));
//
//    // Preserve the original shuffled order
//    List<Question> orderedQuestions = questionIds.stream()
//            .map(questionMap::get)
//            .filter(Objects::nonNull)
//            .collect(Collectors.toList());
//
//    log.info("Returning {} shuffled questions for student {} (Set {})",
//            orderedQuestions.size(), studentId, assignment.getAssignedSetNumber());
//
//    return orderedQuestions;
//}
//
