package io.datamorph.core;

import io.datamorph.exceptions.ParseException;
import io.datamorph.parser.Parser;
import io.datamorph.parser.ParserFactory;
import io.datamorph.transform.Transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * InputStream 기반 지연 평가 DataSource 구현체
 * <p>
 * InputStream과 Parser를 보관하여 지연 파싱 및 스트림 기반 처리를 수행합니다.
 * 실제 데이터 처리는 terminal operation(toList 등)이 호출될 때 수행됩니다.
 * </p>
 * 
 * <p>
 * 특징:
 * - 지연 평가 (Lazy Evaluation)
 * - 메모리 효율적 처리
 * - InputStream 자원 관리
 * - Transform 체이닝 지원
 * - 스레드 안전성 (InputStream 단일 소비)
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.01
 */
public class StreamDataSource implements DataSource {
    
    private final InputStream inputStream;
    private final Format format;
    private final List<TransformStep> transformSteps;
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    
    /**
     * Transform 단계를 나타내는 내부 클래스
     */
    private static abstract class TransformStep {
        /**
         * 변환 단계를 적용합니다.
         *
         * @param rows 입력 데이터 행들
         * @return 변환된 데이터 행들
         */
        abstract List<DataRow> apply(List<DataRow> rows);
        
        /**
         * 이 변환 단계의 설명을 반환합니다.
         *
         * @return 변환 단계 설명
         */
        abstract String getDescription();
    }
    
    /**
     * Consumer 기반 Transform 단계
     */
    private static class ConsumerTransformStep extends TransformStep {
        private final Consumer<DataRow> transformer;
        
        ConsumerTransformStep(Consumer<DataRow> transformer) {
            this.transformer = Objects.requireNonNull(transformer, "Transformer cannot be null");
        }
        
        @Override
        List<DataRow> apply(List<DataRow> rows) {
            return rows.stream()
                      .map(row -> {
                          DataRow newRow = row.copy();
                          transformer.accept(newRow);
                          return newRow;
                      })
                      .collect(Collectors.toList());
        }
        
        @Override
        String getDescription() {
            return "Consumer Transform";
        }
    }
    
    /**
     * Transform 객체 기반 Transform 단계
     */
    private static class TransformObjectStep extends TransformStep {
        private final Transform transform;
        
        TransformObjectStep(Transform transform) {
            this.transform = Objects.requireNonNull(transform, "Transform cannot be null");
        }
        
        @Override
        List<DataRow> apply(List<DataRow> rows) {
            return rows.stream()
                      .map(transform::apply)
                      .collect(Collectors.toList());
        }
        
        @Override
        String getDescription() {
            return "Transform Object: " + transform.toString();
        }
    }
    
    /**
     * 필터 단계
     */
    private static class FilterStep extends TransformStep {
        private final Predicate<DataRow> predicate;
        
        FilterStep(Predicate<DataRow> predicate) {
            this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        }
        
        @Override
        List<DataRow> apply(List<DataRow> rows) {
            return rows.stream()
                      .filter(predicate)
                      .collect(Collectors.toList());
        }
        
        @Override
        String getDescription() {
            return "Filter";
        }
    }
    
    /**
     * InputStream과 Format으로 새로운 StreamDataSource를 생성합니다.
     *
     * @param inputStream 데이터 입력 스트림
     * @param format      데이터 형식
     * @throws IllegalArgumentException inputStream 또는 format이 null인 경우
     */
    public StreamDataSource(InputStream inputStream, Format format) {
        this.inputStream = Objects.requireNonNull(inputStream, "InputStream cannot be null");
        this.format = Objects.requireNonNull(format, "Format cannot be null");
        this.transformSteps = new ArrayList<>();
    }
    
    /**
     * 기존 StreamDataSource를 복사하여 새로운 Transform 단계를 추가한 StreamDataSource를 생성합니다.
     */
    private StreamDataSource(InputStream inputStream, Format format, List<TransformStep> transformSteps) {
        this.inputStream = inputStream;
        this.format = format;
        this.transformSteps = new ArrayList<>(transformSteps);
    }
    
    @Override
    public DataSource transform(Consumer<DataRow> transformer) {
        Objects.requireNonNull(transformer, "Transformer cannot be null");
        
        List<TransformStep> newSteps = new ArrayList<>(transformSteps);
        newSteps.add(new ConsumerTransformStep(transformer));
        
        return new StreamDataSource(inputStream, format, newSteps);
    }
    
    @Override
    public DataSource transform(Transform transform) {
        Objects.requireNonNull(transform, "Transform cannot be null");
        
        List<TransformStep> newSteps = new ArrayList<>(transformSteps);
        newSteps.add(new TransformObjectStep(transform));
        
        return new StreamDataSource(inputStream, format, newSteps);
    }
    
    @Override
    public DataSource filter(Predicate<DataRow> predicate) {
        Objects.requireNonNull(predicate, "Predicate cannot be null");
        
        List<TransformStep> newSteps = new ArrayList<>(transformSteps);
        newSteps.add(new FilterStep(predicate));
        
        return new StreamDataSource(inputStream, format, newSteps);
    }
    
    @Override
    public List<DataRow> toList() {
        if (consumed.getAndSet(true)) {
            throw new IllegalStateException("InputStream has already been consumed. " +
                "StreamDataSource can only be used once.");
        }
        
        try {
            Parser parser = ParserFactory.createParser(format);
            List<DataRow> rows = parser.parse(inputStream);
            
            for (TransformStep step : transformSteps) {
                rows = step.apply(rows);
            }
            
            return List.copyOf(rows);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process stream data: " + e.getMessage(), e);
        } finally {
            closeInputStreamSafely();
        }
    }
    
    /**
     * InputStream을 안전하게 닫습니다.
     */
    private void closeInputStreamSafely() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                System.err.println("Warning: Failed to close InputStream: " + e.getMessage());
            }
        }
    }
    
    /**
     * 현재 설정된 변환 단계들의 설명을 반환합니다.
     *
     * @return 변환 단계 설명 리스트
     */
    public List<String> getTransformStepDescriptions() {
        List<String> descriptions = new ArrayList<>();
        
        for (int i = 0; i < transformSteps.size(); i++) {
            TransformStep step = transformSteps.get(i);
            descriptions.add("Step " + (i + 1) + ": " + step.getDescription());
        }
        
        return descriptions;
    }
    
    /**
     * 변환 단계의 개수를 반환합니다.
     *
     * @return 변환 단계 개수
     */
    public int getTransformStepCount() {
        return transformSteps.size();
    }
    
    /**
     * 사용된 데이터 형식을 반환합니다.
     *
     * @return 데이터 형식
     */
    public Format getFormat() {
        return format;
    }
    
    /**
     * InputStream이 사용 가능한지 확인합니다.
     * 
     * @return InputStream이 사용 가능하면 true
     */
    public boolean isInputStreamAvailable() {
        try {
            return inputStream != null && inputStream.available() >= 0 && !consumed.get();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * InputStream이 이미 소비되었는지 확인합니다.
     *
     * @return 이미 소비된 경우 true
     */
    public boolean isConsumed() {
        return consumed.get();
    }
    
    /**
     * StreamDataSource의 상태 정보를 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    public String getStatusInfo() {
        return String.format("StreamDataSource{format=%s, transformSteps=%d, consumed=%s, available=%s}",
            format, transformSteps.size(), consumed.get(), isInputStreamAvailable());
    }
    
    @Override
    public String toString() {
        return getStatusInfo();
    }
}
