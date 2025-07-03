package io.datamorph.integration;

import io.datamorph.core.DataMorph;
import io.datamorph.core.DataRow;
import io.datamorph.core.DataSource;
import io.datamorph.core.Format;
import io.datamorph.core.StreamDataSource;
import io.datamorph.parser.CsvParser;
import io.datamorph.parser.JsonLinesParser;
import io.datamorph.streaming.MemoryMonitor;
import io.datamorph.transform.Transform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 스트리밍 기능 통합 테스트
 * DataMorph의 스트리밍 처리, 메모리 효율성, JSON Lines 지원 등을 종합적으로 테스트
 */
class StreamingIntegrationTest {

    @TempDir
    Path tempDir;

    private String csvData;
    private MemoryMonitor memoryMonitor;

    @BeforeEach
    void setUp() {
        csvData = """
                emp_name,age,salary,department,active
                John Doe,30,50000,Engineering,true
                Jane Smith,25,45000,Marketing,true
                Bob Johnson,35,60000,Engineering,false
                Alice Brown,28,52000,HR,true
                """;
        memoryMonitor = new MemoryMonitor();
    }

    @Nested
    @DisplayName("DataMorph API 통합 테스트")
    class DataMorphApiIntegrationTest {

        @Test
        @DisplayName("CSV 스트림 변환 체이닝 통합 테스트")
        void testCsvStreamTransformChaining() {
            // Given
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

            // When
            List<DataRow> result = DataMorph.fromStream(inputStream, Format.CSV)
                    .transform(Transform.builder()
                            .rename("emp_name", "name")
                            .add("bonus", 1000)
                            .remove("active")
                            .build())
                    .filter(row -> row.getString("department").equals("Engineering"))
                    .transform(row -> {
                        int currentSalary = row.getInt("salary");
                        row.set("total_compensation", currentSalary + row.getInt("bonus"));
                    })
                    .toList();

            // Then
            assertEquals(2, result.size());

            DataRow john = result.stream()
                    .filter(row -> "John Doe".equals(row.getString("name")))
                    .findFirst()
                    .orElseThrow();

            assertEquals("John Doe", john.getString("name"));
            assertEquals(30, john.getInt("age"));
            assertEquals(50000, john.getInt("salary"));
            assertEquals(1000, john.getInt("bonus"));
            assertEquals(51000, john.getInt("total_compensation"));
            assertEquals("Engineering", john.getString("department"));
            assertFalse(john.has("emp_name"));
            assertFalse(john.has("active"));
        }

        @Test
        @DisplayName("지연 평가 동작 검증 테스트")
        void testLazyEvaluationBehavior() {
            // Given
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

            // When
            long startTime = System.currentTimeMillis();

            DataSource dataSource = DataMorph.fromStream(inputStream, Format.CSV)
                    .transform(row -> row.set("processed", true))
                    .filter(row -> row.getInt("age") > 25);

            long middleTime = System.currentTimeMillis();

            // Then
            assertTrue(middleTime - startTime < 100);
            assertTrue(dataSource instanceof StreamDataSource);

            // When
            List<DataRow> result = dataSource.toList();

            // Then
            assertEquals(3, result.size());
            result.forEach(row -> assertTrue(row.getBoolean("processed")));
        }

        @Test
        @DisplayName("파일 기반 스트리밍 DataSource 테스트")
        void testFileBasedStreamingDataSource() throws IOException {
            // Given
            Path csvFile = tempDir.resolve("employees.csv");
            Files.writeString(csvFile, csvData);

            // When
            List<DataRow> result = DataMorph.fromStreamFile(csvFile.toString())
                    .transform(row -> row.set("processed_date", "2025-07-01"))
                    .filter(row -> row.getInt("age") >= 28)
                    .toList();

            // Then
            assertEquals(3, result.size());
            result.forEach(row -> {
                assertEquals("2025-07-01", row.getString("processed_date"));
                assertTrue(row.getInt("age") >= 28);
            });
        }

        @Test
        @DisplayName("변환 단계 추적 테스트")
        void testTransformStepTracking() {
            // Given
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

            // When
            StreamDataSource dataSource = (StreamDataSource) DataMorph.fromStream(inputStream, Format.CSV)
                    .transform(row -> row.set("step1", "done"))
                    .filter(row -> row.getInt("age") > 25)
                    .transform(Transform.builder().add("step2", "done").build());

            // Then
            assertEquals(3, dataSource.getTransformStepCount());

            List<String> descriptions = dataSource.getTransformStepDescriptions();
            assertEquals(3, descriptions.size());

            assertTrue(descriptions.get(0).contains("Step 1"));
            assertTrue(descriptions.get(1).contains("Step 2"));
            assertTrue(descriptions.get(2).contains("Step 3"));
        }

        @Test
        @DisplayName("스트림 자원 관리 테스트")
        void testStreamResourceManagement() throws IOException {
            // Given
            Path testFile = tempDir.resolve("test.csv");
            Files.writeString(testFile, csvData);

            // When
            DataSource dataSource = DataMorph.fromStreamFile(testFile.toString());

            // Then
            assertTrue(dataSource instanceof StreamDataSource);

            StreamDataSource streamSource = (StreamDataSource) dataSource;
            assertTrue(streamSource.isInputStreamAvailable());

            List<DataRow> result = streamSource.toList();
            assertEquals(4, result.size());
        }
    }

    @Nested
    @DisplayName("메모리 모니터링")
    class MemoryMonitoringTest {

        @Test
        @DisplayName("메모리 사용량을 정상적으로 추적한다")
        void tracksMemoryUsageCorrectly() {
            // when
            double usage = memoryMonitor.getMemoryUsageRatio();
            String info = memoryMonitor.getMemoryInfo();

            // then
            assertThat(usage).isBetween(0.0, 1.0);
            assertThat(info).contains("Memory usage:");
            assertThat(info).contains("MB");
        }

        @Test
        @DisplayName("사용 가능한 메모리를 반환한다")
        void returnsAvailableMemory() {
            // when
            long available = memoryMonitor.getAvailableMemory();

            // then
            assertThat(available).isPositive();
        }

        @Test
        @DisplayName("메모리 압박 상태를 확인한다")
        void checksMemoryPressure() {
            // when
            boolean pressure = memoryMonitor.isMemoryPressureHigh();

            // then
            assertThat(pressure).isInstanceOf(Boolean.class);
        }
    }

    @Nested
    @DisplayName("JSON Lines 스트리밍 처리")
    class JsonLinesStreamingTest {

        @Test
        @DisplayName("JSON Lines 형태의 데이터를 스트리밍으로 처리한다")
        void processesJsonLinesData() {
            // given
            String jsonLines = """
                    {"name": "John", "age": 30, "department": "Engineering"}
                    {"name": "Jane", "age": 25, "department": "Marketing"}
                    {"name": "Bob", "age": 35, "department": "Engineering"}
                    """;

            InputStream input = new ByteArrayInputStream(jsonLines.getBytes(StandardCharsets.UTF_8));
            JsonLinesParser parser = new JsonLinesParser();

            // when
            try (Stream<DataRow> stream = parser.parseAsStream(input)) {
                List<DataRow> result = stream
                        .peek(row -> memoryMonitor.checkMemoryUsage())
                        .filter(row -> "Engineering".equals(row.getString("department")))
                        .toList();

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getString("name")).isEqualTo("John");
                assertThat(result.get(1).getString("name")).isEqualTo("Bob");
            }
        }

        @Test
        @DisplayName("빈 라인을 건너뛰고 처리한다")
        void skipsEmptyLines() {
            // given
            String jsonLinesWithEmpty = """
                    {"name": "John", "age": 30}
                    
                    {"name": "Jane", "age": 25}
                    
                    
                    {"name": "Bob", "age": 35}
                    """;

            InputStream input = new ByteArrayInputStream(jsonLinesWithEmpty.getBytes(StandardCharsets.UTF_8));
            JsonLinesParser parser = new JsonLinesParser();

            // when
            try (Stream<DataRow> stream = parser.parseAsStream(input)) {
                List<DataRow> result = stream.toList();

                // then
                assertThat(result).hasSize(3);
                assertThat(result.get(0).getString("name")).isEqualTo("John");
                assertThat(result.get(1).getString("name")).isEqualTo("Jane");
                assertThat(result.get(2).getString("name")).isEqualTo("Bob");
            }
        }
    }

    @Nested
    @DisplayName("스트리밍 성능 및 메모리 효율성")
    class StreamingPerformanceTest {

        @Test
        @DisplayName("대용량 CSV 데이터를 메모리 모니터링과 함께 처리한다")
        void processesLargeCsvWithMemoryMonitoring() {
            // given
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("id,name,value\n");
            for (int i = 1; i <= 1000; i++) {
                csvBuilder.append(i).append(",Employee").append(i).append(",").append(i * 100).append("\n");
            }

            InputStream input = new ByteArrayInputStream(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
            CsvParser parser = new CsvParser();

            // when
            long startMemory = memoryMonitor.getAvailableMemory();

            try (Stream<DataRow> stream = parser.parseAsStream(input)) {
                List<DataRow> processed = stream
                        .peek(row -> memoryMonitor.checkMemoryUsage())
                        .filter(row -> row.getInt("id") % 100 == 0)
                        .toList();

                // then
                assertThat(processed).hasSize(10);

                long endMemory = memoryMonitor.getAvailableMemory();
                long memoryDiff = Math.abs(startMemory - endMemory);
                assertThat(memoryDiff).isLessThan(100 * 1024 * 1024); // 100MB 미만 차이
            }
        }

        @Test
        @DisplayName("DataMorph API를 통한 메모리 효율성 검증")
        void testDataMorphMemoryEfficiency() {
            // Given
            StringBuilder largeDataBuilder = new StringBuilder();
            largeDataBuilder.append("id,name,value\n");

            for (int i = 1; i <= 1000; i++) {
                largeDataBuilder.append(i)
                        .append(",Employee")
                        .append(i)
                        .append(",")
                        .append(i * 100)
                        .append("\n");
            }

            InputStream largeInputStream = new ByteArrayInputStream(
                    largeDataBuilder.toString().getBytes(StandardCharsets.UTF_8));

            long startTime = System.currentTimeMillis();

            List<DataRow> result = DataMorph.fromStream(largeInputStream, Format.CSV)
                    .filter(row -> row.getInt("id") % 10 == 0) // 10의 배수만 필터링
                    .transform(row -> row.set("filtered", true))
                    .toList();

            long endTime = System.currentTimeMillis();

            // Then
            assertEquals(100, result.size());
            assertTrue(endTime - startTime < 5000);

            result.forEach(row -> {
                assertEquals(0, row.getInt("id") % 10);
                assertTrue(row.getBoolean("filtered"));
            });
        }

        @Test
        @DisplayName("대용량 데이터셋(1만 row)으로 메모리 효율성을 검증한다")
        void testMemoryEfficiencyWithLargeDataset() {
            // given
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("id,data,timestamp\n");
            for (int i = 1; i <= 10000; i++) {
                csvBuilder.append(i)
                        .append(",data_").append(i)
                        .append(",2025-01-01T").append(String.format("%02d", i % 24))
                        .append(":00:00\n");
            }

            InputStream input = new ByteArrayInputStream(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
            CsvParser parser = new CsvParser();

            // when
            long initialMemory = memoryMonitor.getAvailableMemory();
            int totalSum = 0;
            final int[] processedCounter = {0};

            try (Stream<DataRow> stream = parser.parseAsStream(input)) {
                totalSum = stream
                        .peek(row -> {
                            processedCounter[0]++;
                            if (processedCounter[0] % 1000 == 0) {
                                memoryMonitor.checkMemoryUsage();
                            }
                        })
                        .mapToInt(row -> row.getInt("id"))
                        .sum();
            }

            long finalMemory = memoryMonitor.getAvailableMemory();

            // then
            assertThat(totalSum).isEqualTo(50005000);

            long memoryUsed = initialMemory - finalMemory;
            assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // 100MB 미만
        }
    }
}
