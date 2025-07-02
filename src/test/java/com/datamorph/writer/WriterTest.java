package com.datamorph.writer;

import com.datamorph.core.DataRow;
import com.datamorph.core.Format;
import com.datamorph.exceptions.WriteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Writer 시스템 테스트
 */
@DisplayName("Writer 시스템 테스트")
class WriterTest {

    @Nested
    @DisplayName("WriterFactory 테스트")
    class WriterFactoryTest {

        @Test
        @DisplayName("포맷으로 Writer 생성 - CSV")
        void createWriter_CSV() {
            Writer writer = WriterFactory.createWriter(Format.CSV);

            assertInstanceOf(CsvWriter.class, writer);
            assertEquals("csv", writer.getSupportedExtension());
            assertEquals("text/csv", writer.getMimeType());
        }

        @Test
        @DisplayName("포맷으로 Writer 생성 - JSON")
        void createWriter_JSON() {
            Writer writer = WriterFactory.createWriter(Format.JSON);
            assertInstanceOf(JsonWriter.class, writer);
            assertEquals("json", writer.getSupportedExtension());
            assertEquals("application/json", writer.getMimeType());
        }

        @Test
        @DisplayName("파일 경로로 Writer 생성")
        void createWriterFromPath() {
            Writer csvWriter = WriterFactory.createWriterFromPath("test.csv");
            assertInstanceOf(CsvWriter.class, csvWriter);

            Writer jsonWriter = WriterFactory.createWriterFromPath("data.json");
            assertInstanceOf(JsonWriter.class, jsonWriter);
        }

        @Test
        @DisplayName("확장자로 Writer 생성")
        void createWriterFromExtension() {
            Writer csvWriter = WriterFactory.createWriterFromExtension(".csv");
            assertInstanceOf(CsvWriter.class, csvWriter);

            Writer jsonWriter = WriterFactory.createWriterFromExtension(".json");
            assertInstanceOf(JsonWriter.class, jsonWriter);
        }

        @Test
        @DisplayName("null 포맷으로 Writer 생성 시 예외")
        void createWriter_nullFormat() {
            assertThrows(IllegalArgumentException.class, () -> 
                WriterFactory.createWriter(null));
        }

        @Test
        @DisplayName("지원하지 않는 확장자로 Writer 생성 시 예외")
        void createWriter_unsupportedExtension() {
            assertThrows(IllegalArgumentException.class, () -> 
                WriterFactory.createWriterFromPath("test.xml"));
            
            assertThrows(IllegalArgumentException.class, () -> 
                WriterFactory.createWriterFromExtension("xml"));
        }
    }

    @Nested
    @DisplayName("CsvWriter 테스트")
    class CsvWriterTest {

        private final CsvWriter writer = new CsvWriter();

        @Test
        @DisplayName("기본 CSV 쓰기")
        void write_basic() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow("name", "John", "age", 30, "city", "New York"),
                createDataRow("name", "Jane", "age", 25, "city", "Boston")
            );

            String result = writer.writeToString(data);
            String[] lines = result.split("\n");

            assertEquals("age,city,name", lines[0]); // 헤더는 정렬됨
            assertEquals("30,New York,John", lines[1]);
            assertEquals("25,Boston,Jane", lines[2]);
        }

        @Test
        @DisplayName("특수문자가 포함된 필드 CSV 쓰기")
        void write_specialCharacters() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow("name", "John \"Johnny\" Doe", "description", "He said, \"Hello, World!\""),
                createDataRow("name", "Jane\nSmith", "description", "Line1\nLine2")
            );

            String result = writer.writeToString(data);
            String[] lines = result.split("\n");

            assertTrue(lines[1].contains("\"John \"\"Johnny\"\" Doe\""));
            assertTrue(lines[1].contains("\"He said, \"\"Hello, World!\"\"\""));
        }

        @Test
        @DisplayName("빈 데이터 CSV 쓰기")
        void write_emptyData() throws WriteException {
            List<DataRow> data = Collections.emptyList();
            String result = writer.writeToString(data);
            assertEquals("", result);
        }

        @Test
        @DisplayName("null 값이 포함된 데이터 CSV 쓰기")
        void write_nullValues() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow("name", "John", "age", null, "city", "New York")
            );

            String result = writer.writeToString(data);
            String[] lines = result.split("\n");

            assertEquals("age,city,name", lines[0]);
            assertEquals(",New York,John", lines[1]);
        }

        @Test
        @DisplayName("null 데이터로 쓰기 시 예외")
        void write_nullData() {
            assertThrows(IllegalArgumentException.class, () ->
                writer.write(null, new ByteArrayOutputStream()));
        }

        @Test
        @DisplayName("null OutputStream으로 쓰기 시 예외")
        void write_nullOutputStream() {
            List<DataRow> data = Arrays.asList(createDataRow("name", "John"));
            assertThrows(IllegalArgumentException.class, () ->
                writer.write(data, null));
        }
    }

    @Nested
    @DisplayName("JsonWriter 테스트")
    class JsonWriterTest {

        private final JsonWriter writer = new JsonWriter();

        @Test
        @DisplayName("기본 JSON 쓰기")
        void write_basic() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow("name", "John", "age", 30, "active", true),
                createDataRow("name", "Jane", "age", 25, "active", false)
            );

            String result = writer.writeToString(data);
            
            assertTrue(result.startsWith("["));
            assertTrue(result.endsWith("]"));
            assertTrue(result.contains("\"name\": \"John\""));
            assertTrue(result.contains("\"age\": 30"));
            assertTrue(result.contains("\"active\": true"));
            assertTrue(result.contains("\"active\": false"));
        }

        @Test
        @DisplayName("특수문자가 포함된 JSON 쓰기")
        void write_specialCharacters() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow("message", "Hello \"World\"", "newline", "Line1\nLine2", "tab", "Before\tAfter")
            );

            String result = writer.writeToString(data);
            
            assertTrue(result.contains("\"Hello \\\"World\\\"\""));
            assertTrue(result.contains("\"Line1\\nLine2\""));
            assertTrue(result.contains("\"Before\\tAfter\""));
        }

        @Test
        @DisplayName("빈 데이터 JSON 쓰기")
        void write_emptyData() throws WriteException {
            List<DataRow> data = Collections.emptyList();
            String result = writer.writeToString(data);
            assertEquals("[\n]", result);
        }

        @Test
        @DisplayName("null 값이 포함된 데이터 JSON 쓰기")
        void write_nullValues() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow("name", "John", "age", null, "city", "New York")
            );

            String result = writer.writeToString(data);
            assertTrue(result.contains("\"age\": null"));
        }

        @Test
        @DisplayName("다양한 데이터 타입 JSON 쓰기")
        void write_variousDataTypes() throws WriteException {
            List<DataRow> data = Arrays.asList(
                createDataRow(
                    "string", "text",
                    "integer", 42,
                    "double", 3.14,
                    "boolean", true,
                    "null_value", null
                )
            );

            String result = writer.writeToString(data);
            
            assertTrue(result.contains("\"string\": \"text\""));
            assertTrue(result.contains("\"integer\": 42"));
            assertTrue(result.contains("\"double\": 3.14"));
            assertTrue(result.contains("\"boolean\": true"));
            assertTrue(result.contains("\"null_value\": null"));
        }
    }

    /**
     * 테스트용 DataRow 생성 헬퍼 메서드
     */
    private DataRow createDataRow(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even");
        }

        DataRow row = new DataRow();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            row.set(key, value);
        }
        return row;
    }
}
