package com.datamorph.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

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
        }
    }
}
