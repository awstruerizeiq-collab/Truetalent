package com.truerize.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import java.util.stream.Collectors;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.ExamQuestionSet;
import com.truerize.entity.Question;
import com.truerize.entity.StudentExamAssignment;
import com.truerize.repository.ExamQuestionSetRepo;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.StudentExamAssignmentRepo;

@Service
public class ExamSetService {

    private static final Logger log = LoggerFactory.getLogger(ExamSetService.class);
    private static final int NUMBER_OF_SETS = 5;

    @Autowired
    private ExamQuestionSetRepo examQuestionSetRepo;

    @Autowired
    private StudentExamAssignmentRepo studentExamAssignmentRepo;

    @Autowired
    private QuestionRepository questionRepository;

    @Transactional
    public Map<String, Object> generateQuestionSets(Long examId) {
        log.info("========================================");
        log.info("🎯 STARTING QUESTION SET GENERATION");
        log.info("   Exam ID: {}", examId);
        log.info("========================================");

        try {

            if (examId == null || examId <= 0) {
                throw new IllegalArgumentException("Invalid exam ID: " + examId);
            }

            int examIdInt = examId.intValue();

            log.info("📚 Step 1: Fetching questions for exam {}...", examId);
            List<Question> questions = questionRepository.findByExamId(examIdInt);

            if (questions == null || questions.isEmpty()) {
                log.error("❌ No questions found for exam: {}", examId);
                throw new IllegalStateException(
                        "No questions found for exam " + examId + ". Please add questions first.");
            }

            log.info("✅ Found {} questions", questions.size());

            try {
                log.info("🗑️ Step 2: Deleting existing student assignments...");
                int deletedAssignments = studentExamAssignmentRepo.deleteByExamId(examIdInt);
                log.info("✅ Deleted {} existing assignments", deletedAssignments);
            } catch (Exception e) {
                log.warn("⚠️ Could not delete assignments: {}", e.getMessage());
            }

            log.info("🗑️ Step 3: Deleting existing question sets...");
            List<ExamQuestionSet> existingSets = examQuestionSetRepo.findByExamId(examIdInt);

            if (!existingSets.isEmpty()) {
                log.info("   Found {} existing sets to delete", existingSets.size());

                for (ExamQuestionSet set : existingSets) {
                    if (set.getQuestionIds() != null && !set.getQuestionIds().isEmpty()) {
                        set.getQuestionIds().clear();
                        examQuestionSetRepo.save(set);
                    }
                }

                examQuestionSetRepo.flush();

                examQuestionSetRepo.deleteByExamId(examIdInt);
                examQuestionSetRepo.flush();

                log.info("✅ Deleted existing question sets");
            } else {
                log.info("✅ No existing sets to delete");
            }

            log.info("📂 Step 4: Grouping questions by section...");
            Map<String, List<Question>> questionsBySection = questions.stream()
                    .collect(Collectors.groupingBy(Question::getSection));

            log.info("✅ Grouped into {} sections:", questionsBySection.size());
            for (Map.Entry<String, List<Question>> entry : questionsBySection.entrySet()) {
                log.info("   Section '{}': {} questions", entry.getKey(), entry.getValue().size());
            }

            log.info("🎲 Step 5: Generating {} question sets...", NUMBER_OF_SETS);
            List<ExamQuestionSet> generatedSets = new ArrayList<>();

            for (int setNum = 1; setNum <= NUMBER_OF_SETS; setNum++) {
                log.info("   Generating Set {}...", setNum);

                ExamQuestionSet questionSet = new ExamQuestionSet();
                questionSet.setExamId(examIdInt);
                questionSet.setSetNumber(setNum);
                questionSet.setCreatedAt(LocalDateTime.now());
                questionSet.setIsActive(true);

                List<String> shuffledQuestionIds = new ArrayList<>();

                List<String> sortedSections = new ArrayList<>(questionsBySection.keySet());
                Collections.sort(sortedSections);

                for (String section : sortedSections) {
                    List<Question> sectionQuestions = new ArrayList<>(questionsBySection.get(section));

                    Collections.shuffle(sectionQuestions, new SecureRandom());

                    shuffledQuestionIds.addAll(
                            sectionQuestions.stream()
                                    .map(q -> String.valueOf(q.getId()))
                                    .collect(Collectors.toList()));
                }

                questionSet.setQuestionIds(shuffledQuestionIds);

                ExamQuestionSet saved = examQuestionSetRepo.save(questionSet);
                generatedSets.add(saved);

                log.info("   ✅ Set {} created with {} questions", setNum, shuffledQuestionIds.size());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("examId", examId);
            result.put("totalQuestions", questions.size());
            result.put("numberOfSets", NUMBER_OF_SETS);
            result.put("sets", generatedSets.stream().map(set -> {
                Map<String, Object> setInfo = new HashMap<>();
                setInfo.put("id", set.getId());
                setInfo.put("setNumber", set.getSetNumber());
                setInfo.put("questionCount", set.getQuestionIds().size());
                setInfo.put("isActive", set.getIsActive());
                return setInfo;
            }).collect(Collectors.toList()));
            result.put("message", "Successfully generated " + NUMBER_OF_SETS + " question sets with " + questions.size()
                    + " questions each");

            log.info("========================================");
            log.info("✅ GENERATION COMPLETE");
            log.info("   Total Sets: {}", NUMBER_OF_SETS);
            log.info("   Questions per Set: {}", questions.size());
            log.info("========================================");

            return result;

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("❌ Business logic error: {}", e.getMessage());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("examId", examId);
            return errorResult;

        } catch (Exception e) {
            log.error("❌ Unexpected error generating question sets", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Failed to generate question sets");
            errorResult.put("examId", examId);
            return errorResult;
        }
    }

    @Transactional
    public Map<String, Object> autoAssignStudentToSet(String studentId, Long examId) {
        log.info("🎯 Auto-assigning student {} to exam {}", studentId, examId);

        try {
            int examIdInt = examId.intValue();

            Optional<StudentExamAssignment> existingAssignment = studentExamAssignmentRepo
                    .findByStudentIdAndExamId(studentId, examIdInt);

            if (existingAssignment.isPresent()) {
                log.info("✅ Student already assigned to Set {}",
                        existingAssignment.get().getAssignedSetNumber());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Student already assigned");
                result.put("studentId", studentId);
                result.put("examId", examId);
                result.put("assignedSetNumber", existingAssignment.get().getAssignedSetNumber());
                result.put("slotNumber", existingAssignment.get().getSlotNumber());
                result.put("algorithm", "round-robin");
                return result;
            }

            List<ExamQuestionSet> existingSets = examQuestionSetRepo.findByExamIdAndIsActiveTrue(examIdInt);

            if (existingSets.isEmpty()) {
                log.info("⚠️ No question sets found, generating...");
                Map<String, Object> generateResult = generateQuestionSets(examId);

                if (!Boolean.TRUE.equals(generateResult.get("success"))) {
                    throw new IllegalStateException("Failed to generate question sets");
                }

                existingSets = examQuestionSetRepo.findByExamIdAndIsActiveTrue(examIdInt);

                if (existingSets.isEmpty()) {
                    throw new IllegalStateException("Failed to create question sets");
                }
            }

            log.info("✅ Found {} active sets", existingSets.size());

            Map<Integer, Long> setDistribution = getSetDistribution(examIdInt);

            log.info("Current distribution:");
            for (int i = 1; i <= NUMBER_OF_SETS; i++) {
                long count = setDistribution.getOrDefault(i, 0L);
                log.info("   Set {}: {} students", i, count);
            }

            int assignedSetNumber = 1;
            long minStudents = Long.MAX_VALUE;

            for (int setNum = 1; setNum <= NUMBER_OF_SETS; setNum++) {
                long count = setDistribution.getOrDefault(setNum, 0L);
                if (count < minStudents) {
                    minStudents = count;
                    assignedSetNumber = setNum;
                }
            }

            log.info("✅ Assigning to Set {} (currently has {} students)",
                    assignedSetNumber, minStudents);

            int slotNumber = (int) (minStudents + 1);

            StudentExamAssignment assignment = new StudentExamAssignment();
            assignment.setStudentId(studentId);
            assignment.setExamId(examIdInt);
            assignment.setAssignedSetNumber(assignedSetNumber);
            assignment.setSlotNumber(slotNumber);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setHasStarted(false);
            assignment.setHasCompleted(false);

            studentExamAssignmentRepo.save(assignment);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Student assigned successfully using round-robin");
            result.put("studentId", studentId);
            result.put("examId", examId);
            result.put("assignedSetNumber", assignedSetNumber);
            result.put("slotNumber", slotNumber);
            result.put("algorithm", "round-robin");

            return result;

        } catch (DataIntegrityViolationException e) {
            log.warn("⚠️ Race condition detected, fetching existing assignment");

            Optional<StudentExamAssignment> existingAssignment = studentExamAssignmentRepo
                    .findByStudentIdAndExamId(studentId, examId.intValue());

            if (existingAssignment.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Student already assigned (race condition handled)");
                result.put("studentId", studentId);
                result.put("examId", examId);
                result.put("assignedSetNumber", existingAssignment.get().getAssignedSetNumber());
                result.put("slotNumber", existingAssignment.get().getSlotNumber());
                result.put("algorithm", "round-robin");
                return result;
            }

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Assignment failed: " + e.getMessage());
            return errorResult;

        } catch (Exception e) {
            log.error("❌ Error in auto-assignment", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Auto-assignment failed: " + e.getMessage());
            return errorResult;
        }
    }

    private Map<Integer, Long> getSetDistribution(int examId) {
        List<StudentExamAssignment> assignments = studentExamAssignmentRepo.findByExamId(examId);

        return assignments.stream()
                .collect(Collectors.groupingBy(
                        StudentExamAssignment::getAssignedSetNumber,
                        Collectors.counting()));
    }

    @Transactional
    public Map<String, Object> assignStudentToSet(String studentId, Long examId, Integer setNumber,
            Integer slotNumber) {
        log.info("📋 Manual assignment: Student {}, Exam {}, Set {}, Slot {}", studentId, examId, setNumber,
                slotNumber);

        try {
            int examIdInt = examId.intValue();

            ExamQuestionSet questionSet = examQuestionSetRepo
                    .findByExamIdAndSetNumberAndIsActiveTrue(examIdInt, setNumber)
                    .orElseThrow(() -> new IllegalStateException(
                            "Question set " + setNumber + " not found for exam " + examId));

            Optional<StudentExamAssignment> existingAssignment = studentExamAssignmentRepo
                    .findByStudentIdAndExamId(studentId, examIdInt);

            if (existingAssignment.isPresent()) {
                StudentExamAssignment existing = existingAssignment.get();

                if (existing.getAssignedSetNumber().equals(setNumber)) {
                    log.info("✅ Student already assigned to Set {}", setNumber);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Student already assigned to this set");
                    result.put("studentId", studentId);
                    result.put("examId", examId);
                    result.put("assignedSetNumber", setNumber);
                    result.put("slotNumber", existing.getSlotNumber());
                    result.put("algorithm", "manual");
                    return result;
                }

                log.info("🔄 Removing existing assignment (Set {})", existing.getAssignedSetNumber());
                studentExamAssignmentRepo.delete(existing);
                studentExamAssignmentRepo.flush();
            }

            StudentExamAssignment assignment = new StudentExamAssignment();
            assignment.setStudentId(studentId);
            assignment.setExamId(examIdInt);
            assignment.setAssignedSetNumber(setNumber);
            assignment.setSlotNumber(slotNumber);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setHasStarted(false);
            assignment.setHasCompleted(false);

            studentExamAssignmentRepo.save(assignment);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Student manually assigned successfully");
            result.put("studentId", studentId);
            result.put("examId", examId);
            result.put("assignedSetNumber", setNumber);
            result.put("slotNumber", slotNumber);
            result.put("algorithm", "manual");

            return result;

        } catch (Exception e) {
            log.error("❌ Error assigning student", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    public List<Question> getShuffledQuestionsForStudent(String studentId, Long examId) {
        log.info("📚 Getting shuffled questions: Student {}, Exam {}", studentId, examId);

        int examIdInt = examId.intValue();

        StudentExamAssignment assignment = studentExamAssignmentRepo
                .findByStudentIdAndExamId(studentId, examIdInt)
                .orElseThrow(() -> new IllegalStateException(
                        "Student " + studentId + " is not assigned to exam " + examId));

        log.info("✅ Assignment found - Set: {}", assignment.getAssignedSetNumber());

        ExamQuestionSet questionSet = examQuestionSetRepo
                .findByExamIdAndSetNumberAndIsActiveTrue(examIdInt, assignment.getAssignedSetNumber())
                .orElseThrow(() -> new IllegalStateException(
                        "Question set " + assignment.getAssignedSetNumber() + " not found"));

        log.info("✅ Question set found with {} questions", questionSet.getQuestionIds().size());

        List<Integer> questionIds = questionSet.getQuestionIds().stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<Question> questions = questionRepository.findAllById(questionIds);

        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Question> orderedQuestions = questionIds.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("✅ Returning {} questions (Set {})",
                orderedQuestions.size(), assignment.getAssignedSetNumber());

        return orderedQuestions;
    }

    public Optional<StudentExamAssignment> getStudentAssignment(String studentId, Long examId) {
        return studentExamAssignmentRepo.findByStudentIdAndExamId(studentId, examId.intValue());
    }

    @Transactional
    public void markExamStarted(String studentId, Long examId) {
        StudentExamAssignment assignment = studentExamAssignmentRepo
                .findByStudentIdAndExamId(studentId, examId.intValue())
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));

        if (!assignment.getHasStarted()) {
            assignment.setHasStarted(true);
            assignment.setStartedAt(LocalDateTime.now());
            studentExamAssignmentRepo.save(assignment);
            log.info("✅ Marked exam as started");
        }
    }

    @Transactional
    public void markExamCompleted(String studentId, Long examId) {
        StudentExamAssignment assignment = studentExamAssignmentRepo
                .findByStudentIdAndExamId(studentId, examId.intValue())
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));

        if (!assignment.getHasCompleted()) {
            assignment.setHasCompleted(true);
            assignment.setCompletedAt(LocalDateTime.now());
            studentExamAssignmentRepo.save(assignment);
            log.info("✅ Marked exam as completed");
        }
    }

    public List<Map<String, Object>> getQuestionSetsForExam(Long examId) {
        List<ExamQuestionSet> sets = examQuestionSetRepo.findByExamId(examId.intValue());

        return sets.stream().map(set -> {
            Map<String, Object> setData = new HashMap<>();
            setData.put("id", set.getId());
            setData.put("setNumber", set.getSetNumber());
            setData.put("examId", set.getExamId());
            setData.put("isActive", set.getIsActive());
            setData.put("createdAt", set.getCreatedAt());
            setData.put("questionIds", set.getQuestionIds());
            setData.put("questionCount", set.getQuestionIds().size());
            return setData;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getExamStatistics(Long examId) {
        int examIdInt = examId.intValue();

        List<StudentExamAssignment> assignments = studentExamAssignmentRepo.findByExamId(examIdInt);
        List<ExamQuestionSet> sets = examQuestionSetRepo.findByExamId(examIdInt);

        Map<Integer, Long> distributionMap = getSetDistribution(examIdInt);

        Map<String, Object> stats = new HashMap<>();
        stats.put("examId", examId);
        stats.put("totalSets", sets.size());
        stats.put("totalStudentsAssigned", assignments.size());
        stats.put("studentsStarted", assignments.stream().filter(StudentExamAssignment::getHasStarted).count());
        stats.put("studentsCompleted", assignments.stream().filter(StudentExamAssignment::getHasCompleted).count());
        stats.put("setDistribution", distributionMap);

        return stats;
    }

    @Transactional
    public Map<String, Object> deleteQuestionSetsForExam(Long examId) {
        int examIdInt = examId.intValue();
        log.info("🗑️ Deleting question sets for exam: {}", examId);

        try {
            // Delete assignments
            long assignmentCount = studentExamAssignmentRepo.countByExamId(examIdInt);
            if (assignmentCount > 0) {
                log.info("🗑️ Deleting {} student assignments", assignmentCount);
                studentExamAssignmentRepo.deleteByExamId(examIdInt);
                studentExamAssignmentRepo.flush();
            }

            // Delete question sets
            List<ExamQuestionSet> existingSets = examQuestionSetRepo.findByExamId(examIdInt);

            if (!existingSets.isEmpty()) {
                log.info("🗑️ Found {} question sets to delete", existingSets.size());

                for (ExamQuestionSet set : existingSets) {
                    if (set.getQuestionIds() != null && !set.getQuestionIds().isEmpty()) {
                        set.getQuestionIds().clear();
                        examQuestionSetRepo.save(set);
                    }
                }

                examQuestionSetRepo.flush();

                Integer deletedCount = examQuestionSetRepo.deleteByExamId(examIdInt);
                examQuestionSetRepo.flush();

                log.info("✅ Deleted {} question sets", deletedCount != null ? deletedCount : 0);

                return Map.of(
                        "success", true,
                        "message", "Question sets deleted successfully",
                        "deletedCount", deletedCount != null ? deletedCount : 0);
            } else {
                return Map.of(
                        "success", true,
                        "message", "No question sets found to delete",
                        "deletedCount", 0);
            }

        } catch (Exception e) {
            log.error("❌ Error deleting question sets", e);

            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }

    public boolean hasQuestionSets(Long examId) {
        return examQuestionSetRepo.countByExamIdAndIsActiveTrue(examId.intValue()) > 0;
    }

    public Map<String, Object> getSetDetails(Long examId, int setNumber) {
        int examIdInt = examId.intValue();

        ExamQuestionSet questionSet = examQuestionSetRepo
                .findByExamIdAndSetNumberAndIsActiveTrue(examIdInt, setNumber)
                .orElseThrow(() -> new IllegalStateException(
                        "Question set " + setNumber + " not found for exam " + examId));

        long studentCount = studentExamAssignmentRepo
                .countByExamIdAndAssignedSetNumber(examIdInt, setNumber);

        return Map.of(
                "examId", examId,
                "setNumber", setNumber,
                "questionCount", questionSet.getQuestionIds().size(),
                "studentCount", studentCount,
                "createdAt", questionSet.getCreatedAt(),
                "isActive", questionSet.getIsActive());
    }

    public Map<String, Object> getSetDetailsWithQuestions(Long examId, Integer setNumber) {
        try {
            int examIdInt = examId.intValue();

            ExamQuestionSet questionSet = examQuestionSetRepo
                    .findByExamIdAndSetNumberAndIsActiveTrue(examIdInt, setNumber)
                    .orElseThrow(() -> new IllegalStateException(
                            "Question set " + setNumber + " not found"));

            List<Question> allQuestions = questionRepository.findByExamId(examIdInt);

            List<Integer> questionIds = questionSet.getQuestionIds().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            List<Question> setQuestions = questionRepository.findAllById(questionIds);

            return Map.of(
                    "examId", examId,
                    "setNumber", setNumber,
                    "questionIdsInSet", questionSet.getQuestionIds(),
                    "questionIdsCount", questionSet.getQuestionIds().size(),
                    "totalQuestionsInExam", allQuestions.size(),
                    "questionsFoundForSet", setQuestions.size(),
                    "setIsActive", questionSet.getIsActive(),
                    "allQuestionIdsInDatabase", allQuestions.stream().map(Question::getId).collect(Collectors.toList()),
                    "questionsInSet", setQuestions.stream().map(q -> Map.of(
                            "id", q.getId(),
                            "questionText",
                            q.getQuestionText().substring(0, Math.min(50, q.getQuestionText().length())),
                            "section", q.getSection(),
                            "type", q.getType(),
                            "marks", q.getMarks())).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("❌ Error getting set details", e);
            return Map.of(
                    "error", e.getMessage(),
                    "examId", examId,
                    "setNumber", setNumber);
        }
    }
}