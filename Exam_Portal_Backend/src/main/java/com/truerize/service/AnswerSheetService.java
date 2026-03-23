package com.truerize.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truerize.dto.AnswerSheetQuestionDTO;
import com.truerize.dto.AnswerSheetResponseDTO;
import com.truerize.entity.Question;
import com.truerize.entity.Result;
import com.truerize.entity.Slot;
import com.truerize.entity.TestSubmission;
import com.truerize.entity.User;
import com.truerize.exception.ResourceNotFoundException;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;
import com.truerize.repository.TestSubmissionRepository;
import com.truerize.repository.UserRepository;

@Service
public class AnswerSheetService {

    private static final int DEFAULT_PASS_PERCENTAGE = 80;
    private static final String NOT_ANSWERED = "Not Answered";
    private static final String CORRECT = "Correct";
    private static final String INCORRECT = "Incorrect";

    private final TestSubmissionRepository testSubmissionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ObjectMapper objectMapper;

    public AnswerSheetService(
            TestSubmissionRepository testSubmissionRepository,
            QuestionRepository questionRepository,
            UserRepository userRepository,
            ExamRepository examRepository) {
        this.testSubmissionRepository = testSubmissionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.objectMapper = new ObjectMapper();
    }

    public AnswerSheetResponseDTO getAnswerSheet(Integer candidateId, Integer examId, Integer slotId) {
        TestSubmission submission = getSubmission(candidateId, examId, slotId);
        return buildAnswerSheet(submission);
    }

    public byte[] generateAnswerSheetExcel(Integer candidateId, Integer examId, Integer slotId) {
        AnswerSheetResponseDTO sheetData = getAnswerSheet(candidateId, examId, slotId);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Answer Sheet");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Candidate Answer Sheet");
            titleCell.setCellStyle(titleStyle);

            rowIndex++;

            rowIndex = addKeyValueRow(sheet, rowIndex, "Candidate Name", sheetData.getCandidateName());
            rowIndex = addKeyValueRow(sheet, rowIndex, "Email", sheetData.getEmail());
            rowIndex = addKeyValueRow(sheet, rowIndex, "College", sheetData.getCollegeName());
            rowIndex = addKeyValueRow(sheet, rowIndex, "Exam", sheetData.getExam());
            rowIndex = addKeyValueRow(sheet, rowIndex, "Slot", safeValue(sheetData.getSlotNumber()));
            rowIndex = addKeyValueRow(sheet, rowIndex, "Score", safeValue(sheetData.getScore()));
            rowIndex = addKeyValueRow(sheet, rowIndex, "Pass Percentage", safeValue(sheetData.getPassPercentage()));
            rowIndex = addKeyValueRow(sheet, rowIndex, "Status", safeValue(sheetData.getStatus()));

            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = { "Q.No", "Question", "Selected Answer", "Correct Answer", "Marks", "Result" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (AnswerSheetQuestionDTO question : sheetData.getQuestions()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safeValue(question.getQNo()));
                row.createCell(1).setCellValue(safeValue(question.getQuestion()));
                row.createCell(2).setCellValue(safeValue(question.getSelectedAnswer()));
                row.createCell(3).setCellValue(safeValue(question.getCorrectAnswer()));
                row.createCell(4).setCellValue(safeValue(question.getMarks()));
                row.createCell(5).setCellValue(safeValue(question.getResult()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate answer sheet file", e);
        }
    }

    public void enrichResultsWithIdentifiers(List<Result> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        Map<String, Integer> userCache = new HashMap<>();
        Map<String, Integer> examCache = new HashMap<>();

        for (Result result : results) {
            if (result == null) {
                continue;
            }

            String emailKey = result.getEmail() == null ? "" : result.getEmail().trim().toLowerCase();
            String examTitleKey = result.getExam() == null ? "" : result.getExam().trim().toLowerCase();

            if (emailKey.isEmpty() || examTitleKey.isEmpty()) {
                continue;
            }

            Optional<TestSubmission> submissionOpt = testSubmissionRepository
                    .findTopByUser_EmailIgnoreCaseAndExam_TitleIgnoreCaseOrderBySubmittedAtDesc(
                            result.getEmail(), result.getExam());

            if (submissionOpt.isPresent()) {
                TestSubmission submission = submissionOpt.get();
                result.setCandidateId(submission.getUser().getId());
                result.setExamId(submission.getExam().getId());
                userCache.put(emailKey, submission.getUser().getId());
                examCache.put(examTitleKey, submission.getExam().getId());
                continue;
            }

            Integer candidateId = userCache.computeIfAbsent(emailKey, k -> userRepository.findByEmailIgnoreCase(
                    result.getEmail()).map(User::getId).orElse(null));
            Integer examId = examCache.computeIfAbsent(examTitleKey, k -> examRepository.findByTitle(
                    result.getExam()).map(exam -> exam.getId()).orElse(null));

            if (candidateId != null) {
                result.setCandidateId(candidateId);
            }
            if (examId != null) {
                result.setExamId(examId);
            }
        }
    }

    private AnswerSheetResponseDTO buildAnswerSheet(TestSubmission submission) {
        AnswerSheetResponseDTO response = new AnswerSheetResponseDTO();
        response.setCandidateId(submission.getUser().getId());
        response.setCandidateName(submission.getUser().getName());
        response.setCollegeName(submission.getUser().getCollegeName());
        response.setEmail(submission.getUser().getEmail());
        response.setExamId(submission.getExam().getId());
        response.setExam(submission.getExam().getTitle());
        response.setScore(submission.getScore());
        response.setSubmittedAt(submission.getSubmittedAt());

        Slot slot = submission.getUser().getSlot();
        if (slot != null) {
            response.setSlotId(slot.getId());
            response.setSlotNumber(slot.getSlotNumber());
            response.setPassPercentage(slot.getPassPercentage() == null ? DEFAULT_PASS_PERCENTAGE : slot.getPassPercentage());
        } else {
            response.setPassPercentage(DEFAULT_PASS_PERCENTAGE);
        }

        Map<String, String> submittedAnswers = parseAnswers(submission.getAnswersJson());
        List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(submission.getExam().getId());
        List<AnswerSheetQuestionDTO> answerRows = new ArrayList<>();

        int attempted = 0;
        int correct = 0;
        int totalMarks = 0;

        for (Question question : questions) {
            AnswerSheetQuestionDTO questionRow = new AnswerSheetQuestionDTO();
            questionRow.setQuestionId(question.getId());
            questionRow.setQNo(question.getqNo());
            questionRow.setQuestion(question.getQuestionText());
            questionRow.setMarks(question.getMarks());
            questionRow.setCorrectAnswer(formatAnswer(question.getAnswer()));

            String selected = submittedAnswers.get(String.valueOf(question.getId()));
            String selectedValue = formatAnswer(selected);
            questionRow.setSelectedAnswer(selectedValue);

            boolean hasSelectedAnswer = !NOT_ANSWERED.equals(selectedValue);
            if (hasSelectedAnswer) {
                attempted++;
            }

            boolean isCorrect = hasSelectedAnswer && areAnswersEqual(selected, question.getAnswer());
            if (isCorrect) {
                correct++;
                questionRow.setResult(CORRECT);
            } else if (hasSelectedAnswer) {
                questionRow.setResult(INCORRECT);
            } else {
                questionRow.setResult(NOT_ANSWERED);
            }

            totalMarks += question.getMarks() == null ? 0 : question.getMarks();
            answerRows.add(questionRow);
        }

        response.setQuestions(answerRows);
        response.setTotalQuestions(answerRows.size());
        response.setAttemptedQuestions(attempted);
        response.setCorrectAnswers(correct);
        response.setTotalMarks(totalMarks);

        int threshold = response.getPassPercentage() == null ? DEFAULT_PASS_PERCENTAGE : response.getPassPercentage();
        Double scorePercentage = totalMarks > 0
                ? (submission.getScore() * 100.0) / totalMarks
                : null;
        response.setStatus(scorePercentage != null && scorePercentage >= threshold ? "Pass" : "Fail");
        return response;
    }

    private TestSubmission getSubmission(Integer candidateId, Integer examId, Integer slotId) {
        Optional<TestSubmission> submissionOpt;
        if (slotId == null) {
            submissionOpt = testSubmissionRepository.findTopByUser_IdAndExam_IdOrderBySubmittedAtDesc(
                    candidateId, examId);
        } else {
            submissionOpt = testSubmissionRepository
                    .findTopByUser_IdAndExam_IdAndUser_Slot_IdOrderBySubmittedAtDesc(candidateId, examId, slotId);
        }

        return submissionOpt.orElseThrow(() -> new ResourceNotFoundException(
                "No answer sheet found for candidateId=" + candidateId + ", examId=" + examId));
    }

    private Map<String, String> parseAnswers(String answersJson) {
        if (answersJson == null || answersJson.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }

        try {
            Map<String, Object> rawMap = objectMapper.readValue(
                    answersJson,
                    new TypeReference<Map<String, Object>>() {
                    });

            Map<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                Object value = entry.getValue();
                normalized.put(entry.getKey(), value == null ? null : String.valueOf(value));
            }
            return normalized;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private boolean areAnswersEqual(String selectedAnswer, String correctAnswer) {
        if (selectedAnswer == null || correctAnswer == null) {
            return false;
        }
        return selectedAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }

    private int addKeyValueRow(XSSFSheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
        return rowIndex + 1;
    }

    private String formatAnswer(String value) {
        if (value == null) {
            return NOT_ANSWERED;
        }
        String cleaned = value.replace("\u0000", "").trim();
        if (cleaned.isEmpty()) {
            return NOT_ANSWERED;
        }
        return new String(cleaned.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private String safeValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
