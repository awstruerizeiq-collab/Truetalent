package com.truerize.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.truerize.entity.Exam;
import com.truerize.entity.Question;
import com.truerize.repository.ExamRepository;
import com.truerize.repository.QuestionRepository;

@Service
public class QuestionService {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    private static final Pattern CSV_SPLIT_PATTERN = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamRepository examRepository;
   
    public List<Question> getQuestionsByExamId(int examId) {
        log.info("📝 Fetching questions for exam id: {}", examId);
        List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
        log.info("✅ Found {} questions", questions.size());
        return questions;
    }
    
   
    public List<Question> getQuestionsByExamIdForStudent(int examId) {
        log.info("👨‍🎓 Fetching questions for student - exam id: {}", examId);
        
        List<Question> questions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
        
       
        questions.forEach(q -> {
            String type = q.getType().toLowerCase();
            
            if (type.equals("mcq")) {
                
                if (q.getAnswer() != null && q.getAnswer().contains(",")) {
                    q.setType("multiple");
                } else {
                    q.setType("single");
                }
            } 
            else if (type.equals("verbal")) {
                q.setType("read-speak");
            } 
            else if (type.equals("coding")) {
                q.setType("coding");
            }
        });
        
        log.info("✅ Processed {} questions for student view", questions.size());
        return questions;
    }

    @Transactional
    public Map<String, Object> importQuestionsFromFile(int examId, MultipartFile file, String defaultSection) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty file");
        }

        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        List<ImportRow> rows;
        if (fileName.endsWith(".csv")) {
            rows = parseCsvRows(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            rows = parseExcelRows(file);
        } else if (fileName.endsWith(".txt")) {
            rows = parseTextRows(extractTextFromTxt(file));
        } else if (fileName.endsWith(".pdf")) {
            rows = parseTextRows(extractTextFromPdf(file));
        } else if (fileName.endsWith(".docx")) {
            rows = parseTextRows(extractTextFromDocx(file));
        } else if (fileName.endsWith(".doc")) {
            rows = parseTextRows(extractTextFromDoc(file));
        } else {
            throw new IllegalArgumentException("Unsupported file type. Allowed: .xlsx, .xls, .csv, .txt, .pdf, .docx, .doc");
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No question rows found in uploaded file");
        }

        Map<String, Integer> sectionQNoMap = new HashMap<>();
        List<Question> existingQuestions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
        for (Question existing : existingQuestions) {
            if (existing.getSection() != null && existing.getqNo() != null) {
                sectionQNoMap.compute(existing.getSection().toUpperCase(Locale.ROOT),
                    (k, v) -> v == null ? existing.getqNo() : Math.max(v, existing.getqNo()));
            }
        }
        List<String> failedRows = new ArrayList<>();
        int createdCount = 0;

        for (ImportRow row : rows) {
            try {
                String normalizedSection = normalizeSection(row.section, defaultSection);
                String normalizedType = normalizeType(row.type);
                List<String> options = normalizeOptions(row);
                String normalizedAnswer = normalizeAnswer(row.answer, options);
                Integer marks = normalizeMarks(row.marks, normalizedSection);

                Integer qNo = row.qNo;
                if (qNo == null || qNo <= 0) {
                    int lastQNo = sectionQNoMap.getOrDefault(normalizedSection, 0);
                    qNo = lastQNo + 1;
                }
                sectionQNoMap.put(normalizedSection,
                    Math.max(sectionQNoMap.getOrDefault(normalizedSection, 0), qNo));

                Question question = new Question();
                question.setExam(exam);
                question.setSection(normalizedSection);
                question.setType(normalizedType);
                question.setQuestionText(row.questionText != null ? row.questionText.trim() : null);
                question.setOptions(options);
                question.setAnswer(normalizedAnswer);
                question.setMarks(marks);
                question.setqNo(qNo);

                validateQuestion(question);
                questionRepository.save(question);
                createdCount++;
            } catch (Exception ex) {
                failedRows.add("Row " + row.rowNumber + ": " + ex.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", createdCount > 0);
        response.put("examId", examId);
        response.put("examTitle", exam.getTitle());
        response.put("processedRows", rows.size());
        response.put("createdCount", createdCount);
        response.put("failedCount", failedRows.size());
        response.put("failedRows", failedRows);
        response.put("message", failedRows.isEmpty()
            ? "Questions uploaded successfully"
            : "Questions uploaded with partial success");

        return response;
    }
    
    
    @Transactional
    public Question addQuestion(int examId, Question question) {
        log.info("➕ Adding question to exam id: {}", examId);
        
        try {
           
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId + " in 'exam' table"));
            
            
            question.setExam(exam);
            
             validateQuestion(question);
            
          
            if (question.getqNo() == null || question.getqNo() <= 0) {
                
                List<Question> existingQuestions = questionRepository.findByExam_IdOrderByQNoAsc(examId);
                int maxQNo = existingQuestions.stream()
                    .mapToInt(Question::getqNo)
                    .max()
                    .orElse(0);
                question.setqNo(maxQNo + 1);
            }
            
            
            if (question.getMarks() == null || question.getMarks() <= 0) {
                question.setMarks(1);
            }
            
            Question savedQuestion = questionRepository.save(question);
            log.info("✅ Added question with id: {} to exam: {}", savedQuestion.getId(), exam.getTitle());
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("❌ Error adding question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add question: " + e.getMessage(), e);
        }
    }
    
    
    @Transactional
    public Question updateQuestion(int questionId, Question questionDetails) {
        log.info("✏️ Updating question with id: {}", questionId);
        
        try {
            Question existingQuestion = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));
            
            
            if (questionDetails.getQuestionText() != null) {
                existingQuestion.setQuestionText(questionDetails.getQuestionText());
            }
            
            if (questionDetails.getMarks() != null) {
                existingQuestion.setMarks(questionDetails.getMarks());
            }
            
            if (questionDetails.getAnswer() != null) {
                existingQuestion.setAnswer(questionDetails.getAnswer());
            }
            
            if (questionDetails.getSection() != null) {
                existingQuestion.setSection(questionDetails.getSection());
            }
            
            if (questionDetails.getType() != null) {
                existingQuestion.setType(questionDetails.getType());
            }
            
            if (questionDetails.getOptions() != null) {
                existingQuestion.setOptions(questionDetails.getOptions());
            }
            
            if (questionDetails.getqNo() != null) {
                existingQuestion.setqNo(questionDetails.getqNo());
            }
            
          
            validateQuestion(existingQuestion);
            
            Question savedQuestion = questionRepository.save(existingQuestion);
            log.info("✅ Updated question id: {}", savedQuestion.getId());
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("❌ Error updating question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update question: " + e.getMessage(), e);
        }
    }
   
    @Transactional
    public void deleteQuestion(int questionId) {
        log.info("🗑️ Deleting question with id: {}", questionId);
        
        try {
            if (!questionRepository.existsById(questionId)) {
                throw new RuntimeException("Question not found with id: " + questionId);
            }
            
            questionRepository.deleteById(questionId);
            log.info("✅ Deleted question id: {}", questionId);
            
        } catch (Exception e) {
            log.error("❌ Error deleting question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete question: " + e.getMessage(), e);
        }
    }
    
  
    private void validateQuestion(Question question) {
        if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
            throw new IllegalArgumentException("Question text is required");
        }
        
        if (question.getSection() == null || question.getSection().trim().isEmpty()) {
            throw new IllegalArgumentException("Section is required");
        }
        
        if (question.getType() == null || question.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
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

    private List<ImportRow> parseExcelRows(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }

            Map<String, Integer> headers = mapHeaders(headerRow, formatter);
            List<ImportRow> rows = new ArrayList<>();

            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String questionText = getCellValue(row, headers.get("question_text"), formatter);
                if (questionText.isBlank()) continue;

                ImportRow importRow = new ImportRow();
                importRow.rowNumber = i + 1;
                importRow.section = getCellValue(row, headers.get("section"), formatter);
                importRow.type = getCellValue(row, headers.get("type"), formatter);
                importRow.questionText = questionText;
                importRow.optionA = getCellValue(row, headers.get("option_a"), formatter);
                importRow.optionB = getCellValue(row, headers.get("option_b"), formatter);
                importRow.optionC = getCellValue(row, headers.get("option_c"), formatter);
                importRow.optionD = getCellValue(row, headers.get("option_d"), formatter);
                importRow.options = getCellValue(row, headers.get("options"), formatter);
                importRow.answer = getCellValue(row, headers.get("answer"), formatter);
                importRow.marks = parseInteger(getCellValue(row, headers.get("marks"), formatter));
                importRow.qNo = parseInteger(getCellValue(row, headers.get("q_no"), formatter));

                rows.add(importRow);
            }

            return rows;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read Excel file", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid Excel file format", ex);
        }
    }

    private List<ImportRow> parseCsvRows(MultipartFile file) {
        List<ImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;

            Map<String, Integer> headers = mapHeaders(splitCsvLine(headerLine));
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;
                List<String> cols = splitCsvLine(line);
                String questionText = getColumnValue(cols, headers.get("question_text"));
                if (questionText.isBlank()) continue;

                ImportRow importRow = new ImportRow();
                importRow.rowNumber = rowNumber;
                importRow.section = getColumnValue(cols, headers.get("section"));
                importRow.type = getColumnValue(cols, headers.get("type"));
                importRow.questionText = questionText;
                importRow.optionA = getColumnValue(cols, headers.get("option_a"));
                importRow.optionB = getColumnValue(cols, headers.get("option_b"));
                importRow.optionC = getColumnValue(cols, headers.get("option_c"));
                importRow.optionD = getColumnValue(cols, headers.get("option_d"));
                importRow.options = getColumnValue(cols, headers.get("options"));
                importRow.answer = getColumnValue(cols, headers.get("answer"));
                importRow.marks = parseInteger(getColumnValue(cols, headers.get("marks")));
                importRow.qNo = parseInteger(getColumnValue(cols, headers.get("q_no")));

                rows.add(importRow);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read CSV file", ex);
        }
        return rows;
    }

    private String extractTextFromTxt(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read TXT file", ex);
        }
    }

    private String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read PDF file", ex);
        }
    }

    private String extractTextFromDocx(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : document.getParagraphs()) {
                if (p != null && p.getText() != null) {
                    sb.append(p.getText()).append("\n");
                }
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read DOCX file", ex);
        }
    }

    private String extractTextFromDoc(MultipartFile file) {
        try (HWPFDocument document = new HWPFDocument(file.getInputStream());
             WordExtractor extractor = new WordExtractor(document)) {
            return String.join("\n", extractor.getParagraphText());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read DOC file", ex);
        }
    }

    private List<ImportRow> parseTextRows(String text) {
        List<ImportRow> rows = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return rows;
        }

        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        ImportRow current = null;
        int rowCounter = 1;

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();

            if (line.isEmpty()) {
                if (isRowReady(current)) {
                    rows.add(current);
                    current = null;
                    rowCounter++;
                }
                continue;
            }

            String normalized = line.toLowerCase(Locale.ROOT);

            if (isQuestionStarter(line)) {
                if (isRowReady(current)) {
                    rows.add(current);
                    rowCounter++;
                }
                current = new ImportRow();
                current.rowNumber = rowCounter;
                current.questionText = stripQuestionPrefix(line);
                continue;
            }

            if (current == null) {
                current = new ImportRow();
                current.rowNumber = rowCounter;
            }

            if (normalized.startsWith("section")) {
                current.section = splitAfterColon(line);
            } else if (normalized.startsWith("type")) {
                current.type = splitAfterColon(line);
            } else if (normalized.startsWith("marks")) {
                current.marks = parseInteger(splitAfterColon(line));
            } else if (normalized.startsWith("answer") || normalized.startsWith("ans")) {
                current.answer = splitAfterColon(line);
            } else if (isOptionLine(line, 'A')) {
                current.optionA = splitOptionLine(line);
            } else if (isOptionLine(line, 'B')) {
                current.optionB = splitOptionLine(line);
            } else if (isOptionLine(line, 'C')) {
                current.optionC = splitOptionLine(line);
            } else if (isOptionLine(line, 'D')) {
                current.optionD = splitOptionLine(line);
            } else if (normalized.startsWith("question")) {
                current.questionText = splitAfterColon(line);
            } else {
                if (current.questionText == null || current.questionText.isBlank()) {
                    current.questionText = line;
                } else {
                    current.questionText = current.questionText + " " + line;
                }
            }
        }

        if (isRowReady(current)) {
            rows.add(current);
        }

        return rows;
    }

    private Map<String, Integer> mapHeaders(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> mapped = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(formatter.formatCellValue(cell));
            if (!normalized.isBlank()) {
                mapped.put(normalized, cell.getColumnIndex());
            }
        }
        return mapped;
    }

    private Map<String, Integer> mapHeaders(List<String> headerCols) {
        Map<String, Integer> mapped = new HashMap<>();
        for (int i = 0; i < headerCols.size(); i++) {
            String normalized = normalizeHeader(headerCols.get(i));
            if (!normalized.isBlank()) {
                mapped.put(normalized, i);
            }
        }
        return mapped;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        String h = header.trim().toLowerCase(Locale.ROOT)
            .replace("-", "_")
            .replace(" ", "_");
        if (h.equals("question") || h.equals("questiontext")) return "question_text";
        if (h.equals("optiona")) return "option_a";
        if (h.equals("optionb")) return "option_b";
        if (h.equals("optionc")) return "option_c";
        if (h.equals("optiond")) return "option_d";
        if (h.equals("qno") || h.equals("question_no")) return "q_no";
        return h;
    }

    private String normalizeSection(String sectionRaw, String defaultSection) {
        String section = (sectionRaw == null || sectionRaw.isBlank()) ? defaultSection : sectionRaw;
        if (section == null || section.isBlank()) {
            throw new IllegalArgumentException("Section is required (A/B/C/D or section name)");
        }

        String val = section.trim().toUpperCase(Locale.ROOT);
        if (val.startsWith("SECTION")) {
            val = val.replace("SECTION", "").replace(":", "").trim();
        }
        if (val.startsWith("A")) return "A";
        if (val.startsWith("B")) return "B";
        if (val.startsWith("C")) return "C";
        if (val.startsWith("D")) return "D";
        if (val.contains("QUANT")) return "A";
        if (val.contains("LOGICAL")) return "B";
        if (val.contains("VERBAL")) return "C";
        if (val.contains("TECH")) return "D";

        throw new IllegalArgumentException("Invalid section value: " + sectionRaw);
    }

    private String normalizeType(String typeRaw) {
        if (typeRaw == null || typeRaw.isBlank()) return "MCQ";
        String val = typeRaw.trim().toUpperCase(Locale.ROOT);
        if (val.equals("MCQ") || val.equals("SINGLE") || val.equals("MULTIPLE")) return "MCQ";
        if (val.equals("VERBAL")) return "VERBAL";
        if (val.equals("CODING")) return "CODING";
        return "MCQ";
    }

    private List<String> normalizeOptions(ImportRow row) {
        List<String> options = new ArrayList<>();
        if (row.options != null && !row.options.isBlank()) {
            for (String opt : row.options.split("\\|")) {
                options.add(opt.trim());
            }
        } else {
            options.add(row.optionA != null ? row.optionA.trim() : "");
            options.add(row.optionB != null ? row.optionB.trim() : "");
            options.add(row.optionC != null ? row.optionC.trim() : "");
            options.add(row.optionD != null ? row.optionD.trim() : "");
        }

        while (options.size() < 4) {
            options.add("");
        }

        if (options.stream().allMatch(String::isBlank)) {
            throw new IllegalArgumentException("Options are required for MCQ");
        }

        return options;
    }

    private String normalizeAnswer(String answerRaw, List<String> options) {
        if (answerRaw == null || answerRaw.isBlank()) {
            throw new IllegalArgumentException("Answer is required");
        }

        String answer = answerRaw.trim();
        String upper = answer.toUpperCase(Locale.ROOT);
        if (upper.matches("^[A-D]$")) {
            return upper;
        }
        if (upper.matches("^[A-D]\\).*")) {
            return String.valueOf(upper.charAt(0));
        }

        for (int i = 0; i < Math.min(options.size(), 4); i++) {
            if (options.get(i) != null && options.get(i).trim().equalsIgnoreCase(answer)) {
                return String.valueOf((char) ('A' + i));
            }
        }

        throw new IllegalArgumentException("Answer must be A/B/C/D or option text");
    }

    private Integer normalizeMarks(Integer marks, String section) {
        if (marks != null && marks > 0) return marks;
        return "D".equalsIgnoreCase(section) ? 2 : 1;
    }

    private String getCellValue(Row row, Integer columnIndex, DataFormatter formatter) {
        if (row == null || columnIndex == null || columnIndex < 0) return "";
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        String[] raw = CSV_SPLIT_PATTERN.split(line, -1);
        for (String col : raw) {
            values.add(unquote(col));
        }
        return values;
    }

    private String getColumnValue(List<String> cols, Integer index) {
        if (index == null || index < 0 || index >= cols.size()) return "";
        return cols.get(index).trim();
    }

    private String unquote(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1).replace("\"\"", "\"");
        }
        return v;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isRowReady(ImportRow row) {
        return row != null && row.questionText != null && !row.questionText.trim().isEmpty();
    }

    private boolean isQuestionStarter(String line) {
        String normalized = line.trim();
        return normalized.matches("^(Q\\s*\\d+|Question\\s*\\d*|\\d+)\\s*[\\.:\\)-].*");
    }

    private String stripQuestionPrefix(String line) {
        return line.replaceFirst("^(Q\\s*\\d+|Question\\s*\\d*|\\d+)\\s*[\\.:\\)-]\\s*", "").trim();
    }

    private boolean isOptionLine(String line, char option) {
        return line.trim().matches("^" + option + "[\\)\\.:\\-].*");
    }

    private String splitOptionLine(String line) {
        return line.replaceFirst("^[A-D][\\)\\.:\\-]\\s*", "").trim();
    }

    private String splitAfterColon(String line) {
        int idx = line.indexOf(':');
        if (idx < 0) idx = line.indexOf('-');
        if (idx < 0) idx = line.indexOf('.');
        if (idx < 0) {
            String trimmed = line.trim();
            int firstSpace = trimmed.indexOf(' ');
            return firstSpace > 0 ? trimmed.substring(firstSpace + 1).trim() : trimmed;
        }
        return line.substring(idx + 1).trim();
    }

    private static class ImportRow {
        int rowNumber;
        String section;
        String type;
        String questionText;
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        String options;
        String answer;
        Integer marks;
        Integer qNo;
    }
}
