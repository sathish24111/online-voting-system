package com.college.voting.utils;

import com.college.voting.entity.Student;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StudentImportHelper {

    public static class ImportResult {
        private final List<Student> students = new ArrayList<>();
        private final List<String> logs = new ArrayList<>();
        private int totalRows = 0;
        private int successCount = 0;
        private int duplicateCount = 0;
        private int errorCount = 0;

        public List<Student> getStudents() { return students; }
        public List<String> getLogs() { return logs; }
        public int getTotalRows() { return totalRows; }
        public int getSuccessCount() { return successCount; }
        public int getDuplicateCount() { return duplicateCount; }
        public int getErrorCount() { return errorCount; }
    }

    public static ImportResult importStudents(MultipartFile file, PasswordEncoder passwordEncoder) {
        ImportResult result = new ImportResult();
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        try (InputStream is = file.getInputStream()) {
            if (filename != null && (filename.endsWith(".xlsx") || (contentType != null && contentType.contains("spreadsheet")))) {
                parseExcel(is, passwordEncoder, result);
            } else if (filename != null && (filename.endsWith(".csv") || (contentType != null && contentType.contains("csv")))) {
                parseCsv(is, passwordEncoder, result);
            } else {
                result.logs.add("Unsupported file type. Please upload a .xlsx Excel sheet or a .csv file.");
                result.errorCount++;
            }
        } catch (Exception e) {
            result.logs.add("Error reading file: " + e.getMessage());
            result.errorCount++;
        }

        return result;
    }

    private static void parseExcel(InputStream is, PasswordEncoder encoder, ImportResult result) throws Exception {
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        int rowNumber = 0;

        for (Row row : sheet) {
            rowNumber++;
            // Skip Header row
            if (rowNumber == 1) {
                continue;
            }

            result.totalRows++;

            // Columns: Register Number, Name, Department, Year, Email, DOB, Password
            String regNo = getCellValueAsString(row.getCell(0));
            String name = getCellValueAsString(row.getCell(1));
            String dept = getCellValueAsString(row.getCell(2));
            String year = getCellValueAsString(row.getCell(3));
            String email = getCellValueAsString(row.getCell(4));
            String dob = getCellValueAsString(row.getCell(5));
            String rawPassword = getCellValueAsString(row.getCell(6));

            if (isEmpty(regNo) || isEmpty(name) || isEmpty(dept) || isEmpty(year) || isEmpty(email) || isEmpty(dob)) {
                result.logs.add("Row " + rowNumber + ": Ignored due to empty column value(s).");
                result.errorCount++;
                continue;
            }

            if (!email.contains("@") || !email.contains(".")) {
                result.logs.add("Row " + rowNumber + ": Ignored due to invalid email address (" + email + ").");
                result.errorCount++;
                continue;
            }

            // Create student instance
            Student student = new Student();
            student.setRegisterNo(regNo.trim().toUpperCase());
            student.setName(name.trim());
            student.setDepartment(dept.trim());
            student.setYear(year.trim());
            student.setEmail(email.trim().toLowerCase());
            student.setDob(dob.trim());
            
            // Set password using Custom value or fallback to default
            String passwordToUse = (rawPassword != null && !rawPassword.trim().isEmpty()) ? rawPassword.trim() : "Password123!";
            student.setPassword(encoder.encode(passwordToUse));
            student.setStatus("ENABLED");

            result.students.add(student);
            result.successCount++;
        }
        workbook.close();
    }

    private static void parseCsv(InputStream is, PasswordEncoder encoder, ImportResult result) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int rowNumber = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                // Skip header row
                if (rowNumber == 1) {
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                result.totalRows++;
                
                // Parse CSV line (simple split, accounting for potential quotes)
                String[] tokens = parseCsvLine(line);

                if (tokens.length < 6) {
                    result.logs.add("Row " + rowNumber + ": Missing columns. Required format: RegisterNo, Name, Department, Year, Email, DOB [, Password].");
                    result.errorCount++;
                    continue;
                }

                String regNo = tokens[0];
                String name = tokens[1];
                String dept = tokens[2];
                String year = tokens[3];
                String email = tokens[4];
                String dob = tokens[5];
                String rawPassword = tokens.length > 6 ? tokens[6] : "";

                if (isEmpty(regNo) || isEmpty(name) || isEmpty(dept) || isEmpty(year) || isEmpty(email) || isEmpty(dob)) {
                    result.logs.add("Row " + rowNumber + ": Ignored due to empty column value(s).");
                    result.errorCount++;
                    continue;
                }

                if (!email.contains("@") || !email.contains(".")) {
                    result.logs.add("Row " + rowNumber + ": Ignored due to invalid email (" + email + ").");
                    result.errorCount++;
                    continue;
                }

                Student student = new Student();
                student.setRegisterNo(regNo.trim().toUpperCase());
                student.setName(name.trim());
                student.setDepartment(dept.trim());
                student.setYear(year.trim());
                student.setEmail(email.trim().toLowerCase());
                student.setDob(dob.trim());
                
                String passwordToUse = (rawPassword != null && !rawPassword.trim().isEmpty()) ? rawPassword.trim() : "Password123!";
                student.setPassword(encoder.encode(passwordToUse));
                student.setStatus("ENABLED");

                result.students.add(student);
                result.successCount++;
            }
        }
    }

    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Handle numbers as strings without scientific notation
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.format("%d", (long) val);
                } else {
                    return String.format("%s", val);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
