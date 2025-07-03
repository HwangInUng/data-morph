package io.datamorph.integration;

import io.datamorph.core.DataMorph;
import io.datamorph.core.DataSource;
import io.datamorph.core.Format;
import io.datamorph.exceptions.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Writer 시스템 통합 테스트
 */
@DisplayName("Writer 시스템 통합 테스트")
class WriterIntegrationTest {

    @TempDir
    Path tempDir;

    // 테스트용 샘플 객체
    static class Employee {
        private String name;
        private int age;
        private double salary;
        private boolean active;

        public Employee() {}

        public Employee(String name, int age, double salary, boolean active) {
            this.name = name;
            this.age = age;
            this.salary = salary;
            this.active = active;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public double getSalary() { return salary; }
        public void setSalary(double salary) { this.salary = salary; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    @Nested
    @DisplayName("DataSource.toFile() 테스트")
    class DataSourceToFileTest {

        @Test
        @DisplayName("CSV 파일로 저장")
        void toFile_csv() throws IOException, ParseException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true),
                new Employee("Jane Smith", 25, 45000.0, false),
                new Employee("Bob Johnson", 35, 60000.0, true)
            );

            DataSource dataSource = DataMorph.fromObjects(employees);
            Path outputFile = tempDir.resolve("employees.csv");

            // When
            dataSource.toFile(outputFile.toString());

            // Then
            assertTrue(Files.exists(outputFile));
            String content = Files.readString(outputFile);
            
            String[] lines = content.split("\n");
            assertEquals("active,age,name,salary", lines[0]); // 헤더
            assertTrue(lines[1].contains("John Doe"));
            assertTrue(lines[2].contains("Jane Smith"));
            assertTrue(lines[3].contains("Bob Johnson"));
        }

        @Test
        @DisplayName("JSON 파일로 저장")
        void toFile_json() throws IOException, ParseException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true),
                new Employee("Jane Smith", 25, 45000.0, false)
            );

            DataSource dataSource = DataMorph.fromObjects(employees);
            Path outputFile = tempDir.resolve("employees.json");

            // When
            dataSource.toFile(outputFile.toString());

            // Then
            assertTrue(Files.exists(outputFile));
            String content = Files.readString(outputFile);
            
            assertTrue(content.startsWith("["));
            assertTrue(content.endsWith("]"));
            assertTrue(content.contains("\"name\": \"John Doe\""));
            assertTrue(content.contains("\"age\": 30"));
            assertTrue(content.contains("\"salary\": 50000.0"));
            assertTrue(content.contains("\"active\": true"));
        }

        @Test
        @DisplayName("포맷 지정하여 파일로 저장")
        void toFile_withFormat() throws IOException, ParseException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true)
            );

            DataSource dataSource = DataMorph.fromObjects(employees);
            Path outputFile = tempDir.resolve("employees.txt");

            // When - CSV 포맷으로 저장
            dataSource.toFile(outputFile.toString(), Format.CSV);

            // Then
            assertTrue(Files.exists(outputFile));
            String content = Files.readString(outputFile);
            assertTrue(content.contains("active,age,name,salary"));
        }

        @Test
        @DisplayName("toString() 메서드 테스트")
        void toString_format() throws ParseException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true)
            );

            DataSource dataSource = DataMorph.fromObjects(employees);

            // When
            String csvString = dataSource.toString(Format.CSV);
            String jsonString = dataSource.toString(Format.JSON);

            // Then
            assertTrue(csvString.contains("active,age,name,salary"));
            assertTrue(csvString.contains("true,30,John Doe,50000.0"));

            assertTrue(jsonString.startsWith("["));
            assertTrue(jsonString.contains("\"name\": \"John Doe\""));
        }
    }

    @Nested
    @DisplayName("DataMorph 유틸리티 메서드 테스트")
    class DataMorphUtilityTest {

        @Test
        @DisplayName("객체 리스트를 파일로 저장")
        void saveObjectsToFile() throws IOException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true),
                new Employee("Jane Smith", 25, 45000.0, false)
            );
            Path outputFile = tempDir.resolve("employees.csv");

            // When
            DataMorph.saveObjectsToFile(employees, outputFile.toString());

            // Then
            assertTrue(Files.exists(outputFile));
            String content = Files.readString(outputFile);
            assertTrue(content.contains("John Doe"));
            assertTrue(content.contains("Jane Smith"));
        }

        @Test
        @DisplayName("객체 리스트를 지정 포맷으로 파일에 저장")
        void saveObjectsToFile_withFormat() throws IOException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true)
            );
            Path outputFile = tempDir.resolve("employees.json");

            // When
            DataMorph.saveObjectsToFile(employees, outputFile.toString(), Format.JSON);

            // Then
            assertTrue(Files.exists(outputFile));
            String content = Files.readString(outputFile);
            assertTrue(content.startsWith("["));
            assertTrue(content.contains("\"name\": \"John Doe\""));
        }

        @Test
        @DisplayName("파일 포맷 변환")
        void convertFile() throws IOException, ParseException {
            // Given - CSV 파일 생성
            String csvContent = "name,age,salary\nJohn Doe,30,50000\nJane Smith,25,45000";
            Path inputFile = tempDir.resolve("input.csv");
            Path outputFile = tempDir.resolve("output.json");
            Files.writeString(inputFile, csvContent);

            // When
            DataMorph.convertFile(inputFile.toString(), outputFile.toString());

            // Then
            assertTrue(Files.exists(outputFile));
            String jsonContent = Files.readString(outputFile);
            assertTrue(jsonContent.startsWith("["));
            assertTrue(jsonContent.contains("\"name\": \"John Doe\""));
            assertTrue(jsonContent.contains("\"age\": 30"));
        }

        @Test
        @DisplayName("포맷 지정하여 파일 변환")
        void convertFile_withFormats() throws IOException {
            // Given - 텍스트 파일에 CSV 데이터 저장
            String csvContent = "name,age,salary\nJohn Doe,30,50000";
            Path inputFile = tempDir.resolve("input.txt");
            Path outputFile = tempDir.resolve("output.txt");
            Files.writeString(inputFile, csvContent);

            // When
            DataMorph.convertFile(
                inputFile.toString(), 
                outputFile.toString(), 
                Format.CSV, 
                Format.JSON
            );

            // Then
            assertTrue(Files.exists(outputFile));
            String jsonContent = Files.readString(outputFile);
            assertTrue(jsonContent.startsWith("["));
            assertTrue(jsonContent.contains("\"name\": \"John Doe\""));
        }

        @Test
        @DisplayName("문자열 데이터를 파일로 저장")
        void saveStringToFile() throws IOException {
            // Given
            String csvContent = "name,age\nJohn,30\nJane,25";
            Path outputFile = tempDir.resolve("output.json");

            // When
            DataMorph.saveStringToFile(csvContent, Format.CSV, outputFile.toString());

            // Then
            assertTrue(Files.exists(outputFile));
            String jsonContent = Files.readString(outputFile);
            assertTrue(jsonContent.startsWith("["));
            assertTrue(jsonContent.contains("\"name\": \"John\""));
        }
    }

    @Nested
    @DisplayName("파일 변환 연계 테스트")
    class FileConversionChainTest {

        @Test
        @DisplayName("CSV → JSON → CSV 변환 체인")
        void conversionChain_csvToJsonToCsv() throws IOException, ParseException {
            // Given - 원본 CSV 파일
            String originalCsv = "name,age,city\nJohn Doe,30,New York\nJane Smith,25,Boston";
            Path csvFile1 = tempDir.resolve("original.csv");
            Path jsonFile = tempDir.resolve("converted.json");
            Path csvFile2 = tempDir.resolve("final.csv");
            Files.writeString(csvFile1, originalCsv);

            // When - CSV → JSON 변환
            DataMorph.convertFile(csvFile1.toString(), jsonFile.toString());
            
            // Then - JSON 파일 검증
            assertTrue(Files.exists(jsonFile));
            String jsonContent = Files.readString(jsonFile);
            assertTrue(jsonContent.contains("\"name\": \"John Doe\""));

            // When - JSON → CSV 변환
            DataMorph.convertFile(jsonFile.toString(), csvFile2.toString());

            // Then - 최종 CSV 파일 검증
            assertTrue(Files.exists(csvFile2));
            String finalCsv = Files.readString(csvFile2);
            assertTrue(finalCsv.contains("age,city,name")); // 정렬된 헤더
            assertTrue(finalCsv.contains("John Doe"));
            assertTrue(finalCsv.contains("Jane Smith"));
        }

        @Test
        @DisplayName("변환 후 필터링 및 변환")
        void conversionWithTransformation() throws IOException, ParseException {
            // Given
            List<Employee> employees = Arrays.asList(
                new Employee("John Doe", 30, 50000.0, true),
                new Employee("Jane Smith", 25, 45000.0, false),
                new Employee("Bob Johnson", 35, 60000.0, true)
            );

            DataSource dataSource = DataMorph.fromObjects(employees);
            Path outputFile = tempDir.resolve("filtered_employees.csv");

            // When - 30세 이상만 필터링하여 저장
            dataSource
                .filter(row -> row.getInt("age") >= 30)
                .toFile(outputFile.toString());

            // Then
            assertTrue(Files.exists(outputFile));
            String content = Files.readString(outputFile);
            
            assertTrue(content.contains("John Doe"));
            assertTrue(content.contains("Bob Johnson"));
            assertFalse(content.contains("Jane Smith")); // 25세이므로 제외
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("잘못된 파일 경로로 저장 시 예외")
        void toFile_invalidPath() throws ParseException {
            List<Employee> employees = Arrays.asList(new Employee("John", 30, 50000.0, true));
            DataSource dataSource = DataMorph.fromObjects(employees);

            // 존재하지 않는 디렉토리에 저장 시도
            assertThrows(RuntimeException.class, () ->
                dataSource.toFile("/nonexistent/directory/file.csv"));
        }

        @Test
        @DisplayName("null 파일 경로로 저장 시 예외")
        void toFile_nullPath() throws ParseException {
            List<Employee> employees = Arrays.asList(new Employee("John", 30, 50000.0, true));
            DataSource dataSource = DataMorph.fromObjects(employees);

            assertThrows(IllegalArgumentException.class, () ->
                dataSource.toFile(null));
        }

        @Test
        @DisplayName("지원하지 않는 확장자로 저장 시 예외")
        void toFile_unsupportedExtension() throws ParseException {
            List<Employee> employees = Arrays.asList(new Employee("John", 30, 50000.0, true));
            DataSource dataSource = DataMorph.fromObjects(employees);

            assertThrows(RuntimeException.class, () ->
                dataSource.toFile("output.xml"));
        }

        @Test
        @DisplayName("null 포맷으로 문자열 변환 시 예외")
        void toString_nullFormat() throws ParseException {
            List<Employee> employees = Arrays.asList(new Employee("John", 30, 50000.0, true));
            DataSource dataSource = DataMorph.fromObjects(employees);

            assertThrows(IllegalArgumentException.class, () ->
                dataSource.toString(null));
        }
    }
}
