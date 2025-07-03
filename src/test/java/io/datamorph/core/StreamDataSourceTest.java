package io.datamorph.core;

import io.datamorph.exceptions.ParseException;
import io.datamorph.transform.Transform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * StreamDataSource 테스트
 */
class StreamDataSourceTest {

    private InputStream csvInputStream;
    private InputStream jsonInputStream;
    private InputStream emptyInputStream;
    private InputStream malformedCsvInputStream;
    
    @BeforeEach
    void setUp() {
        // CSV 테스트 데이터
        String csvData = """
            name,age,salary,department
            John,30,50000,Engineering
            Jane,25,45000,Marketing
            Bob,35,60000,Engineering
            """;
        csvInputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        
        // JSON 테스트 데이터
        String jsonData = """
            [
                {"name": "John", "age": 30, "salary": 50000, "department": "Engineering"},
                {"name": "Jane", "age": 25, "salary": 45000, "department": "Marketing"},
                {"name": "Bob", "age": 35, "salary": 60000, "department": "Engineering"}
            ]
            """;
        jsonInputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
        
        // 빈 데이터
        emptyInputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        
        // 잘못된 형식의 CSV 데이터
        String malformedCsvData = """
            name,age,salary
            John,30,50000,extra_field
            Jane,25
            """;
        malformedCsvInputStream = new ByteArrayInputStream(malformedCsvData.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("StreamDataSource 생성은")
    class ConstructorTest {

        @Test
        @DisplayName("유효한 InputStream과 Format으로 생성된다")
        void createWithValidInputStreamAndFormat() {
            // when
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // then
            assertThat(dataSource).isNotNull();
            assertThat(dataSource.getFormat()).isEqualTo(Format.CSV);
            assertThat(dataSource.getTransformStepCount()).isEqualTo(0);
            assertThat(dataSource.isConsumed()).isFalse();
            assertThat(dataSource.isInputStreamAvailable()).isTrue();
        }

        @Test
        @DisplayName("null InputStream으로 생성 시 예외가 발생한다")
        void throwExceptionWhenInputStreamIsNull() {
            // when & then
            assertThatThrownBy(() -> new StreamDataSource(null, Format.CSV))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("InputStream cannot be null");
        }

        @Test
        @DisplayName("null Format으로 생성 시 예외가 발생한다")
        void throwExceptionWhenFormatIsNull() {
            // when & then
            assertThatThrownBy(() -> new StreamDataSource(csvInputStream, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Format cannot be null");
        }
    }

    @Nested
    @DisplayName("지연 평가는")
    class LazyEvaluationTest {

        @Test
        @DisplayName("toList() 호출 전까지 파싱하지 않는다")
        void notParseUntilToListCalled() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when - transform 단계만 추가 (아직 파싱되지 않음)
            DataSource transformedSource = dataSource
                .transform(row -> row.set("processed", true))
                .filter(row -> row.getInt("age") > 25);
            
            // then - 아직 실제 데이터 처리는 되지 않음
            assertThat(transformedSource).isInstanceOf(StreamDataSource.class);
            StreamDataSource streamSource = (StreamDataSource) transformedSource;
            assertThat(streamSource.getTransformStepCount()).isEqualTo(2); // transform + filter
            assertThat(streamSource.isConsumed()).isFalse();
        }

        @Test
        @DisplayName("변환 단계 체이닝 시 원본은 변경되지 않는다")
        void originalNotChangedWhenChaining() {
            // given
            StreamDataSource originalSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            DataSource transformedSource = originalSource
                .transform(row -> row.set("modified", true));
            
            // then
            assertThat(originalSource.getTransformStepCount()).isEqualTo(0);
            StreamDataSource transformed = (StreamDataSource) transformedSource;
            assertThat(transformed.getTransformStepCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("CSV 스트림 처리는")
    class CsvStreamProcessingTest {

        @Test
        @DisplayName("기본 CSV 데이터를 올바르게 파싱한다")
        void parseBasicCsvData() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            List<DataRow> result = dataSource.toList();
            
            // then
            assertThat(result).hasSize(3);
            
            DataRow firstRow = result.get(0);
            assertThat(firstRow.getString("name")).isEqualTo("John");
            assertThat(firstRow.getInt("age")).isEqualTo(30);
            assertThat(firstRow.getInt("salary")).isEqualTo(50000);
            assertThat(firstRow.getString("department")).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("변환과 필터를 적용하여 처리한다")
        void applyTransformAndFilter() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            List<DataRow> result = dataSource
                .transform(row -> row.set("bonus", 1000))
                .filter(row -> row.getString("department").equals("Engineering"))
                .toList();
            
            // then
            assertThat(result).hasSize(2); // John과 Bob만 Engineering
            
            assertThat(result)
                .allMatch(row -> row.getString("department").equals("Engineering"))
                .allMatch(row -> row.getInt("bonus") == 1000);
        }
    }

    @Nested
    @DisplayName("JSON 스트림 처리는")
    class JsonStreamProcessingTest {

        @Test
        @DisplayName("JSON 배열 데이터를 올바르게 파싱한다")
        void parseJsonArrayData() {
            // given
            StreamDataSource dataSource = new StreamDataSource(jsonInputStream, Format.JSON);
            
            // when
            List<DataRow> result = dataSource.toList();
            
            // then
            assertThat(result).hasSize(3);
            
            DataRow johnRow = result.stream()
                .filter(row -> "John".equals(row.getString("name")))
                .findFirst()
                .orElseThrow();
                
            assertThat(johnRow.getInt("age")).isEqualTo(30);
            assertThat(johnRow.getInt("salary")).isEqualTo(50000);
        }

        @Test
        @DisplayName("복잡한 변환을 적용하여 처리한다")
        void applyComplexTransformation() {
            // given
            StreamDataSource dataSource = new StreamDataSource(jsonInputStream, Format.JSON);
            
            // when
            List<DataRow> result = dataSource
                .transform(row -> {
                    int currentSalary = row.getInt("salary");
                    row.set("salary", currentSalary * 1.1); // 10% 인상
                })
                .filter(row -> row.getInt("age") >= 30)
                .toList();
            
            // then
            assertThat(result).hasSize(2); // John과 Bob만 30세 이상
            
            DataRow johnRow = result.stream()
                .filter(row -> "John".equals(row.getString("name")))
                .findFirst()
                .orElseThrow();
            
            assertThat(johnRow.getDouble("salary")).isEqualTo(55000.0, within(0.01)); // 50000 * 1.1
        }
    }

    @Nested
    @DisplayName("Transform 객체 사용은")
    class TransformObjectTest {

        @Test
        @DisplayName("Transform 빌더로 생성된 변환을 적용한다")
        void applyTransformBuilderTransformation() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            Transform transform = Transform.builder()
                .rename("name", "employee_name")
                .add("company", "DataMorph Inc.")
                .remove("department")
                .build();
            
            // when
            List<DataRow> result = dataSource
                .transform(transform)
                .toList();
            
            // then
            assertThat(result).hasSize(3);
            
            DataRow first = result.get(0);
            assertThat(first.getString("employee_name")).isEqualTo("John");
            assertThat(first.getString("company")).isEqualTo("DataMorph Inc.");
            assertThat(first.has("name")).isFalse(); // 이름이 변경됨
            assertThat(first.has("department")).isFalse(); // 제거됨
        }

        @Test
        @DisplayName("null Transform 객체 시 예외가 발생한다")
        void throwExceptionWhenTransformIsNull() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when & then
            assertThatThrownBy(() -> dataSource.transform((Transform) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Transform cannot be null");
        }
    }

    @Nested
    @DisplayName("변환 단계 체이닝은")
    class TransformChainingTest {

        @Test
        @DisplayName("여러 변환 단계를 순서대로 적용한다")
        void applyMultipleTransformStepsInOrder() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            DataSource chainedSource = dataSource
                .transform(row -> row.set("step1", "completed"))
                .filter(row -> row.getInt("age") > 25)
                .transform(row -> row.set("step2", "completed"))
                .transform(Transform.builder().add("step3", "completed").build());
            
            // then
            assertThat(chainedSource).isInstanceOf(StreamDataSource.class);
            StreamDataSource streamSource = (StreamDataSource) chainedSource;
            assertThat(streamSource.getTransformStepCount()).isEqualTo(4);
            
            List<String> descriptions = streamSource.getTransformStepDescriptions();
            assertThat(descriptions).hasSize(4);
            assertThat(descriptions.get(0)).contains("Consumer Transform");
            assertThat(descriptions.get(1)).contains("Filter");
            assertThat(descriptions.get(2)).contains("Consumer Transform");
            assertThat(descriptions.get(3)).contains("Transform Object");
        }

        @ParameterizedTest
        @MethodSource("transformStepTestCases")
        @DisplayName("다양한 변환 단계 조합을 처리한다")
        void handleVariousTransformStepCombinations(String description, int expectedSteps, 
                                                   java.util.function.Function<StreamDataSource, DataSource> transformation) {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            DataSource result = transformation.apply(dataSource);
            
            // then
            assertThat(result).as(description).isInstanceOf(StreamDataSource.class);
            assertThat(((StreamDataSource) result).getTransformStepCount()).as(description).isEqualTo(expectedSteps);
        }

        static Stream<Arguments> transformStepTestCases() {
            return Stream.of(
                Arguments.arguments("단일 필터", 1, 
                    (java.util.function.Function<StreamDataSource, DataSource>) ds -> ds.filter(row -> true)),
                Arguments.arguments("단일 변환", 1,
                    (java.util.function.Function<StreamDataSource, DataSource>) ds -> ds.transform(row -> {})),
                Arguments.arguments("필터 -> 변환", 2,
                    (java.util.function.Function<StreamDataSource, DataSource>) ds -> ds.filter(row -> true).transform(row -> {})),
                Arguments.arguments("변환 -> 필터 -> 변환", 3,
                    (java.util.function.Function<StreamDataSource, DataSource>) ds -> 
                        ds.transform(row -> {}).filter(row -> true).transform(row -> {}))
            );
        }
    }

    @Nested
    @DisplayName("필터링은")
    class FilteringTest {

        @Test
        @DisplayName("조건에 맞는 행만 필터링한다")
        void filterRowsMatchingCondition() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            List<DataRow> result = dataSource
                .filter(row -> row.getInt("age") >= 30)
                .toList();
            
            // then
            assertThat(result).hasSize(2); // John(30), Bob(35)
            assertThat(result)
                .allMatch(row -> row.getInt("age") >= 30);
        }

        @Test
        @DisplayName("모든 행이 필터링되면 빈 리스트를 반환한다")
        void returnEmptyListWhenAllRowsFiltered() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            List<DataRow> result = dataSource
                .filter(row -> row.getInt("age") > 100) // 100세 초과 (없음)
                .toList();
            
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null predicate 시 예외가 발생한다")
        void throwExceptionWhenPredicateIsNull() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when & then
            assertThatThrownBy(() -> dataSource.filter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Predicate cannot be null");
        }
    }

    @Nested
    @DisplayName("스트림 소비는")
    class StreamConsumptionTest {

        @Test
        @DisplayName("한 번만 소비할 수 있다")
        void canOnlyBeConsumedOnce() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when - 첫 번째 소비
            List<DataRow> firstResult = dataSource.toList();
            
            // then
            assertThat(firstResult).hasSize(3);
            assertThat(dataSource.isConsumed()).isTrue();
            
            // when & then - 두 번째 소비 시도
            assertThatThrownBy(() -> dataSource.toList())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("InputStream has already been consumed. StreamDataSource can only be used once.");
        }

        @Test
        @DisplayName("소비 후 상태 정보를 올바르게 반환한다")
        void returnCorrectStatusAfterConsumption() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            dataSource.toList();
            
            // then
            assertThat(dataSource.isConsumed()).isTrue();
            assertThat(dataSource.isInputStreamAvailable()).isFalse();
            
            String statusInfo = dataSource.getStatusInfo();
            assertThat(statusInfo)
                .contains("format=CSV")
                .contains("consumed=true")
                .contains("available=false");
        }
    }

    @Nested
    @DisplayName("빈 데이터 처리는")
    class EmptyDataHandlingTest {

        @Test
        @DisplayName("빈 스트림에서 빈 리스트를 반환한다")
        void returnEmptyListForEmptyStream() {
            // given
            StreamDataSource dataSource = new StreamDataSource(emptyInputStream, Format.CSV);
            
            // when
            List<DataRow> result = dataSource.toList();
            
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 스트림에 변환을 적용해도 빈 리스트를 반환한다")
        void returnEmptyListForEmptyStreamWithTransforms() {
            // given
            StreamDataSource dataSource = new StreamDataSource(emptyInputStream, Format.CSV);
            
            // when
            List<DataRow> result = dataSource
                .transform(row -> row.set("test", "value"))
                .filter(row -> true)
                .toList();
            
            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("에러 처리는")
    class ErrorHandlingTest {

        @Test
        @DisplayName("잘못된 형식의 데이터 파싱 시 예외가 발생한다")
        void throwExceptionForMalformedData() {
            // given
            StreamDataSource dataSource = new StreamDataSource(malformedCsvInputStream, Format.CSV);
            
            // when & then
            assertThatThrownBy(() -> dataSource.toList())
                .isInstanceOf(ParseException.class);
        }

        @Test
        @DisplayName("변환 중 예외 발생 시 RuntimeException으로 감싸서 전파한다")
        void wrapExceptionDuringTransformation() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when & then
            assertThatThrownBy(() -> dataSource
                .transform(row -> {
                    throw new RuntimeException("Transform error");
                })
                .toList())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process stream data");
        }

        @Test
        @DisplayName("null Consumer 변환 시 예외가 발생한다")
        void throwExceptionWhenConsumerTransformIsNull() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when & then
            assertThatThrownBy(() -> dataSource.transform((java.util.function.Consumer<DataRow>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Transformer cannot be null");
        }
    }

    @Nested
    @DisplayName("DataMorph 통합은")
    class DataMorphIntegrationTest {

        @Test
        @DisplayName("DataMorph.fromStream()으로 StreamDataSource를 생성한다")
        void createStreamDataSourceViaDataMorph() {
            // when
            DataSource dataSource = DataMorph.fromStream(csvInputStream, Format.CSV);
            
            // then
            assertThat(dataSource).isInstanceOf(StreamDataSource.class);
            StreamDataSource streamSource = (StreamDataSource) dataSource;
            assertThat(streamSource.getFormat()).isEqualTo(Format.CSV);
            assertThat(streamSource.getTransformStepCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("DataMorph.fromStreamFile()으로 파일 기반 StreamDataSource를 생성한다")
        void createFileBasedStreamDataSource() {
            // 이 테스트는 실제 파일이 필요하므로 현재는 스킵
            // 통합 테스트에서 다룰 예정
        }
    }

    @Nested
    @DisplayName("상태 정보는")
    class StatusInfoTest {

        @Test
        @DisplayName("올바른 상태 정보를 반환한다")
        void returnCorrectStatusInfo() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            String statusInfo = dataSource.getStatusInfo();
            
            // then
            assertThat(statusInfo)
                .contains("StreamDataSource")
                .contains("format=CSV")
                .contains("transformSteps=0")
                .contains("consumed=false")
                .contains("available=true");
        }

        @Test
        @DisplayName("toString()이 상태 정보를 반환한다")
        void toStringReturnsStatusInfo() {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when
            String result = dataSource.toString();
            
            // then
            assertThat(result).isEqualTo(dataSource.getStatusInfo());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 3, 5})
        @DisplayName("변환 단계 수에 따른 상태 정보를 올바르게 표시한다")
        void showCorrectTransformStepCount(int steps) {
            // given
			DataSource current = new StreamDataSource(csvInputStream, Format.CSV);
            
            // when - 지정된 수만큼 변환 단계 추가
            for (int i = 0; i < steps; i++) {
                int finalI = i;
                current = current.transform(row -> row.set("step" + finalI, "done"));
            }
            
            // then
            StreamDataSource streamSource = (StreamDataSource) current;
            assertThat(streamSource.getStatusInfo())
                .contains("transformSteps=" + steps);
        }
    }

    @Nested
    @DisplayName("스레드 안전성은")
    class ThreadSafetyTest {

        @Test
        @DisplayName("동시 소비 시도에서 하나만 성공한다")
        void onlyOneThreadCanConsume() throws InterruptedException {
            // given
            StreamDataSource dataSource = new StreamDataSource(csvInputStream, Format.CSV);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);
            java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger exceptionCount = new java.util.concurrent.atomic.AtomicInteger(0);
            
            // when - 두 스레드에서 동시에 소비 시도
            Thread thread1 = new Thread(() -> {
                try {
                    dataSource.toList();
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            
            Thread thread2 = new Thread(() -> {
                try {
                    dataSource.toList();
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            
            thread1.start();
            thread2.start();
            latch.await();
            
            // then - 하나만 성공하고 하나는 예외 발생
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(exceptionCount.get()).isEqualTo(1);
            assertThat(dataSource.isConsumed()).isTrue();
        }
    }
}
