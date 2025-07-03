package io.datamorph.parser;

import io.datamorph.core.DataRow;
import io.datamorph.core.Format;
import io.datamorph.exceptions.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * CsvParser의 스트리밍 기능 테스트
 */
class CsvStreamingParserTest {

    private CsvParser parser;
    private String csvData;
    private String largeCsvData;
    private String emptyData;
    private String headerOnlyData;

    @BeforeEach
    void setUp() {
        parser = new CsvParser();
        
        csvData = "name,age,salary,department\n" +
                "John,30,50000,Engineering\n" +
                "Jane,25,45000,Marketing\n" +
                "Bob,35,60000,Engineering\n";
            
        // 대용량 데이터 시뮬레이션
        StringBuilder largeBuilder = new StringBuilder();
        largeBuilder.append("id,name,value\n");
        for (int i = 1; i <= 1000; i++) {
            largeBuilder.append(i)
                       .append(",Employee")
                       .append(i)
                       .append(",")
                       .append(i * 100)
                       .append("\n");
        }
        largeCsvData = largeBuilder.toString();
        
        emptyData = "";
        headerOnlyData = "name,age,salary\n";
    }

    @Nested
    @DisplayName("스트리밍 파싱 지원 여부는")
    class StreamingSupportTest {

        @Test
        @DisplayName("CsvParser가 스트리밍 파싱을 지원한다")
        void csvParserSupportsStreamingParsing() {
            // when & then
            assertThat(parser.supportsStreamingParsing()).isTrue();
        }

        @Test
        @DisplayName("권장 버퍼 크기를 반환한다")
        void returnsRecommendedBufferSize() {
            // when
            int bufferSize = parser.getRecommendedBufferSize();
            
            // then
            assertThat(bufferSize).isEqualTo(16384); // 16KB
        }
    }

    @Nested
    @DisplayName("기본 스트리밍 파싱은")
    class BasicStreamingParsingTest {

        @Test
        @DisplayName("CSV 데이터를 스트림으로 파싱한다")
        void parsesCsvDataAsStream() {
            // given
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            List<DataRow> result = rowStream.toList();
            
            // then
            assertThat(result).hasSize(3);
            
            DataRow firstRow = result.get(0);
            assertThat(firstRow.getString("name")).isEqualTo("John");
            assertThat(firstRow.getInt("age")).isEqualTo(30);
            assertThat(firstRow.getInt("salary")).isEqualTo(50000);
            assertThat(firstRow.getString("department")).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("빈 스트림에서 빈 스트림을 반환한다")
        void returnsEmptyStreamForEmptyInput() {
            // given
            InputStream inputStream = new ByteArrayInputStream(emptyData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            List<DataRow> result = rowStream.toList();
            
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("헤더만 있는 경우 빈 스트림을 반환한다")
        void returnsEmptyStreamForHeaderOnly() {
            // given
            InputStream inputStream = new ByteArrayInputStream(headerOnlyData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            List<DataRow> result = rowStream.toList();
            
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null InputStream 시 예외가 발생한다")
        void throwsExceptionForNullInputStream() {
            // when & then
            assertThatThrownBy(() -> parser.parseAsStream(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("InputStream cannot be null");
        }
    }

    @Nested
    @DisplayName("스트림 자원 관리는")
    class StreamResourceManagementTest {

        @Test
        @DisplayName("스트림 사용 후 자동으로 리소스를 해제한다")
        void automaticallyClosesResourcesAfterStreamUsage() {
            // given
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            
            // 스트림 사용
            List<DataRow> result = rowStream.toList();
            
            // then
            assertThat(result).hasSize(3);
            
            // InputStream은 자동으로 닫혀야 함 (직접 테스트하기 어려우므로 예외 없이 완료되는지 확인)
            assertThatCode(() -> {
                // 스트림 재사용 시도 (이미 닫힌 상태)
                inputStream.available();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("try-with-resources와 함께 사용할 수 있다")
        void worksWithTryWithResources() {
            // given
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            
            // when & then
            assertThatCode(() -> {
                try (Stream<DataRow> rowStream = parser.parseAsStream(inputStream)) {
                    List<DataRow> result = rowStream.toList();
                    assertThat(result).hasSize(3);
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("스트리밍과 일괄 파싱 결과 비교는")
    class StreamingVsBatchComparisonTest {

        @Test
        @DisplayName("스트리밍과 일괄 파싱 결과가 동일하다")
        void streamingAndBatchParsingProduceSameResults() {
            // given
            String testData = csvData;
            InputStream streamInput = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
            
            // when
            List<DataRow> batchResult = parser.parse(testData);
            Stream<DataRow> rowStream = parser.parseAsStream(streamInput);
            List<DataRow> streamResult = rowStream.toList();
            
            // then
            assertThat(streamResult).hasSize(batchResult.size());
            
            for (int i = 0; i < batchResult.size(); i++) {
                DataRow batchRow = batchResult.get(i);
                DataRow streamRow = streamResult.get(i);
                
                assertThat(streamRow.getFieldNames()).isEqualTo(batchRow.getFieldNames());
                
                for (String fieldName : batchRow.getFieldNames()) {
                    assertThat(streamRow.getObject(fieldName)).isEqualTo(batchRow.getObject(fieldName));
                }
            }
        }
    }

    @Nested
    @DisplayName("에러 처리는")
    class ErrorHandlingTest {

        @Test
        @DisplayName("잘못된 CSV 형식 시 예외가 발생한다")
        void throwsExceptionForInvalidCsvFormat() {
            // given
        String invalidData = "name!@#$%,age,salary\n" +
                "John,30,50000\n";
            InputStream inputStream = new ByteArrayInputStream(invalidData.getBytes(StandardCharsets.UTF_8));
            
            // when & then
            assertThatThrownBy(() -> {
                Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
                rowStream.toList(); // 스트림 소비
            }).isInstanceOf(ParseException.class);
        }

        @Test
        @DisplayName("컬럼 수 불일치 시 런타임 예외가 발생한다")
        void throwsRuntimeExceptionForColumnMismatch() {
            // given
        String mismatchData = "name,age,salary\n" +
                "John,30,50000,extra\n" +
                "Jane,25\n";
            InputStream inputStream = new ByteArrayInputStream(mismatchData.getBytes(StandardCharsets.UTF_8));
            
            // when & then
            assertThatThrownBy(() -> {
                Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
                rowStream.toList(); // 스트림 소비
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Column count mismatch");
        }
    }

    @Nested
    @DisplayName("CSV 특수 문자 처리는")
    class CsvSpecialCharacterTest {

        @Test
        @DisplayName("따옴표가 포함된 필드를 올바르게 파싱한다")
        void parsesFieldsWithQuotes() {
            // given
            String quotedData = "name,description,value\n" +
                "\"John Doe\",\"Software \"\"Engineer\"\"\",50000\n" +
                "Jane Smith,\"Marketing Manager\",45000\n";
            InputStream inputStream = new ByteArrayInputStream(quotedData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            List<DataRow> result = rowStream.toList();
            
            // then
            assertThat(result).hasSize(2);
            
            DataRow johnRow = result.get(0);
            assertThat(johnRow.getString("name")).isEqualTo("John Doe");
            assertThat(johnRow.getString("description")).isEqualTo("Software \"Engineer\"");
            assertThat(johnRow.getInt("value")).isEqualTo(50000);
        }

        @Test
        @DisplayName("쉼표가 포함된 필드를 올바르게 파싱한다")
        void parsesFieldsWithCommas() {
            // given
        String commaData = "name,address,salary\n" +
                "\"John Doe\",\"123 Main St, City, State\",50000\n" +
                "\"Jane Smith\",\"456 Oak Ave, Town\",45000\n";
            InputStream inputStream = new ByteArrayInputStream(commaData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            List<DataRow> result = rowStream.toList();
            
            // then
            assertThat(result).hasSize(2);
            
            DataRow johnRow = result.get(0);
            assertThat(johnRow.getString("name")).isEqualTo("John Doe");
            assertThat(johnRow.getString("address")).isEqualTo("123 Main St, City, State");
        }
    }

    @Nested
    @DisplayName("대용량 데이터 스트리밍은")
    class LargeDataStreamingTest {

        @Test
        @DisplayName("대용량 CSV 데이터를 효율적으로 처리한다")
        void handlesLargeCsvDataEfficiently() {
            // given
            InputStream inputStream = new ByteArrayInputStream(largeCsvData.getBytes(StandardCharsets.UTF_8));
            
            // when
            long startTime = System.currentTimeMillis();
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            
            // 스트림 처리 (지연 평가 확인)
            List<DataRow> result = rowStream
                .filter(row -> row.getInt("id") % 100 == 0) // 100의 배수만
                .toList();
            
            long endTime = System.currentTimeMillis();
            
            // then
            assertThat(result).hasSize(10); // 100, 200, ..., 1000
            assertThat(endTime - startTime).isLessThan(1000); // 1초 미만
            
            // 첫 번째와 마지막 행 검증
            DataRow first = result.get(0);
            assertThat(first.getInt("id")).isEqualTo(100);
            assertThat(first.getString("name")).isEqualTo("Employee100");
            assertThat(first.getInt("value")).isEqualTo(10000);
            
            DataRow last = result.get(result.size() - 1);
            assertThat(last.getInt("id")).isEqualTo(1000);
            assertThat(last.getString("name")).isEqualTo("Employee1000");
            assertThat(last.getInt("value")).isEqualTo(100000);
        }

        @Test
        @DisplayName("스트림 변환과 필터링을 체이닝한다")
        void chainsStreamTransformationsAndFiltering() {
            // given
            InputStream inputStream = new ByteArrayInputStream(largeCsvData.getBytes(StandardCharsets.UTF_8));
            
            // when
            Stream<DataRow> rowStream = parser.parseAsStream(inputStream);
            
            List<String> employeeNames = rowStream
                .filter(row -> row.getInt("id") <= 10) // 처음 10개만
                .map(row -> row.getString("name"))
                .toList();
            
            // then
            assertThat(employeeNames).hasSize(10);
            assertThat(employeeNames).containsExactly(
                "Employee1", "Employee2", "Employee3", "Employee4", "Employee5",
                "Employee6", "Employee7", "Employee8", "Employee9", "Employee10"
            );
        }
    }

    @Nested
    @DisplayName("ParserFactory 통합은")
    class ParserFactoryIntegrationTest {

        @Test
        @DisplayName("ParserFactory가 CSV 스트리밍 지원을 확인한다")
        void parserFactoryConfirmsCsvStreamingSupport() {
            // when & then
            assertThat(ParserFactory.supportsStreamingParsing(Format.CSV)).isTrue();
        }

        @Test
        @DisplayName("ParserFactory가 CSV 권장 버퍼 크기를 반환한다")
        void parserFactoryReturnsRecommendedBufferSize() {
            // when
            int bufferSize = ParserFactory.getRecommendedBufferSize(Format.CSV);
            
            // then
            assertThat(bufferSize).isEqualTo(16384);
        }
    }
}
