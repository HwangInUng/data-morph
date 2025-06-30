package com.datamorph.core;

import com.datamorph.error.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * DataMorph 클래스의 테스트
 * TDD 방식으로 기능을 하나씩 추가해나갑니다.
 */
class DataMorphTest {
    
    @Nested
    @DisplayName("from(String filePath) 메서드는")
    class FromFilePathTest {
        
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 비어있는 파일 경로를 받으면 IllegalArgumentException을 던진다")
        void throwsExceptionWhenFilePathIsNullAndEmpty(String filePath) {
            // when & then
            assertThatThrownBy(() -> DataMorph.from(filePath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File path cannot be null or empty");
        }
        
        @Test
        @DisplayName("공백만 있는 파일 경로를 받으면 IllegalArgumentException을 던진다")
        void throwsExceptionWhenFilePathIsBlank() {
            // given
            String filePath = "   ";

            // when & then
            assertThatThrownBy(() -> DataMorph.from(filePath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File path cannot be null or empty");
        }
        
        @Test
        @DisplayName("존재하지 않는 파일 경로를 받으면 IllegalArgumentException을 던진다")
        void throwsExceptionWhenFileDoesNotExist() {
            // given
            String filePath = "/non/existent/file.csv";
            
            // when & then
            assertThatThrownBy(() -> DataMorph.from(filePath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File does not exist");
        }
        
        @Test
        @DisplayName("디렉토리 경로를 받으면 IllegalArgumentException을 던진다")
        void throwsExceptionWhenPathIsDirectory(@TempDir Path tempDir) {
            // given
            String dirPath = tempDir.toString();
            
            // when & then
            assertThatThrownBy(() -> DataMorph.from(dirPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path is not a file");
        }
        
        @Test
        @DisplayName("유효한 CSV 파일 경로를 받으면 DataSource를 반환한다")
        void returnsDataSourceForValidCsvFile(@TempDir Path tempDir) throws Exception {
            // given
            Path csvFile = tempDir.resolve("test.csv");
            Files.writeString(csvFile, "name,age\nJohn,30\nJane,25");
            
            // when
            DataSource result = DataMorph.from(csvFile.toString());
            
            // then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(DataSource.class);
            
            List<DataRow> rows = result.toList();
            assertThat(rows).hasSize(2);
            
            DataRow firstRow = rows.get(0);
            assertThat(firstRow.getString("name")).isEqualTo("John");
            assertThat(firstRow.getString("age")).isEqualTo("30");
            
            DataRow secondRow = rows.get(1);
            assertThat(secondRow.getString("name")).isEqualTo("Jane");
            assertThat(secondRow.getString("age")).isEqualTo("25");
        }
        
        @Test
        @DisplayName("유효한 JSON 파일 경로를 받으면 DataSource를 반환한다")
        void returnsDataSourceForValidJsonFile(@TempDir Path tempDir) throws Exception {
            // given
            Path jsonFile = tempDir.resolve("test.json");
            Files.writeString(jsonFile, "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]");
            
            // when
            DataSource result = DataMorph.from(jsonFile.toString());
            
            // then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(DataSource.class);
            
            List<DataRow> rows = result.toList();
            assertThat(rows).hasSize(2);
            
            DataRow firstRow = rows.get(0);
            assertThat(firstRow.getString("name")).isEqualTo("John");
            assertThat(firstRow.getInt("age")).isEqualTo(30);
        }
        
        @Test
        @DisplayName("지원하지 않는 파일 확장자를 받으면 RuntimeException을 던진다")
        void throwsExceptionForUnsupportedFileExtension(@TempDir Path tempDir) throws Exception {
            // given
            Path txtFile = tempDir.resolve("test.txt");
            Files.writeString(txtFile, "some content");
            
            // when & then
            assertThatThrownBy(() -> DataMorph.from(txtFile.toString()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse file")
                .hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Nested
    @DisplayName("fromString(String content, Format format) 메서드는")
    class FromStringTest {
        
        @Test
        @DisplayName("null content를 받으면 IllegalArgumentException을 던진다")
        void throwsExceptionWhenContentIsNull() {
            // given
            String content = null;
            Format format = Format.CSV;
            
            // when & then
            assertThatThrownBy(() -> DataMorph.fromString(content, format))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content cannot be null");
        }
        
        @Test
        @DisplayName("null format을 받으면 IllegalArgumentException을 던진다")
        void throwsExceptionWhenFormatIsNull() {
            // given
            String content = "name,age\nJohn,30";
            Format format = null;
            
            // when & then
            assertThatThrownBy(() -> DataMorph.fromString(content, format))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Format cannot be null");
        }
        
        @Test
        @DisplayName("유효한 CSV content와 format을 받으면 DataSource를 반환한다")
        void returnsDataSourceForValidCsvContent() {
            // given
            String content = "name,age\nJohn,30\nJane,25";
            Format format = Format.CSV;
            
            // when
            DataSource result = DataMorph.fromString(content, format);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(DataSource.class);
            
            List<DataRow> rows = result.toList();
            assertThat(rows).hasSize(2);
            
            DataRow firstRow = rows.get(0);
            assertThat(firstRow.getString("name")).isEqualTo("John");
            assertThat(firstRow.getString("age")).isEqualTo("30");
        }
        
        @Test
        @DisplayName("유효한 JSON content와 format을 받으면 DataSource를 반환한다")
        void returnsDataSourceForValidJsonContent() {
            // given
            String content = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
            Format format = Format.JSON;
            
            // when
            DataSource result = DataMorph.fromString(content, format);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(DataSource.class);
            
            List<DataRow> rows = result.toList();
            assertThat(rows).hasSize(2);
            
            DataRow firstRow = rows.get(0);
            assertThat(firstRow.getString("name")).isEqualTo("John");
            assertThat(firstRow.getInt("age")).isEqualTo(30);
        }
        
        @Test
        @DisplayName("빈 문자열 content를 받으면 빈 DataSource를 반환한다")
        void returnsEmptyDataSourceForEmptyContent() {
            // given
            String content = "";
            Format format = Format.CSV;
            
            // when
            DataSource result = DataMorph.fromString(content, format);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.toList()).isEmpty();
        }
        
        @Test
        @DisplayName("잘못된 형식의 content를 받으면 ParseException을 던진다")
        void throwsExceptionForInvalidContent() {
            // given
            String content = "invalid json content";
            Format format = Format.JSON;
            
            // when & then
            assertThatThrownBy(() -> DataMorph.fromString(content, format))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid JSON format");
        }
    }
    
    @Nested
    @DisplayName("데이터 변환 통합 테스트")
    class IntegrationTest {
        
        @Test
        @DisplayName("CSV 파일을 읽어서 데이터 변환 작업을 수행할 수 있다")
        void canPerformDataTransformationOnCsvFile(@TempDir Path tempDir) throws Exception {
            // given
            Path csvFile = tempDir.resolve("employees.csv");
            Files.writeString(csvFile, "name,age,salary\nJohn,30,50000\nJane,25,45000\nBob,35,60000");
            
            // when
            DataSource dataSource = DataMorph.from(csvFile.toString());
            
            // 나이가 30 이상인 직원들만 필터링
            DataSource filteredData = dataSource.filter(row -> {
                Integer age = row.getInt("age");
                return age != null && age >= 30;
            });
            
            // 급여에 10% 보너스 추가
            DataSource transformedData = filteredData.transform(row -> {
                Integer salary = row.getInt("salary");
                if (salary != null) {
                    int bonusSalary = (int) (salary * 1.1);
                    row.set("salary", bonusSalary);
                }
            });
            
            // then
            List<DataRow> result = transformedData.toList();
            assertThat(result).hasSize(2);
            
            // John: 30세, 급여 55000
            DataRow john = result.stream()
                .filter(row -> "John".equals(row.getString("name")))
                .findFirst()
                .orElseThrow();
            assertThat(john.getInt("age")).isEqualTo(30);
            assertThat(john.getInt("salary")).isEqualTo(55000);
            
            // Bob: 35세, 급여 66000
            DataRow bob = result.stream()
                .filter(row -> "Bob".equals(row.getString("name")))
                .findFirst()
                .orElseThrow();
            assertThat(bob.getInt("age")).isEqualTo(35);
            assertThat(bob.getInt("salary")).isEqualTo(66000);
        }
    }
}
