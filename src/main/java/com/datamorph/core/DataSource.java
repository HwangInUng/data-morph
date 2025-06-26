package com.datamorph.core;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 데이터 소스를 표현하는 인터페이스
 * <p>
 * Fluent API를 통해 데이터 변환, 필터링 등의 연산을 체이닝할 수 있습니다.
 * 모든 중간 연산(transform, filter)은 새로운 DataSource를 반환하며,
 * 원본 데이터는 변경되지 않습니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.06.25
 */
public interface DataSource {
    
    /**
     * 각 데이터 행에 변환을 적용합니다.
     * <p>
     * 예시:
     * <pre>{@code
     * dataSource.transform(row -> row.set("name", row.getString("name").toUpperCase()))
     * }</pre>
     * </p>
     * 
     * @param transformer 각 행에 적용할 변환 함수
     * @return 변환이 적용된 새로운 DataSource
     * @throws NullPointerException transformer가 null인 경우
     */
    DataSource transform(Consumer<DataRow> transformer);
    
    /**
     * 주어진 조건에 맞는 행만 필터링합니다.
     * <p>
     * 예시:
     * <pre>{@code
     * dataSource.filter(row -> row.getInt("age") > 20)
     * }</pre>
     * </p>
     * 
     * @param predicate 필터 조건
     * @return 필터링된 새로운 DataSource
     * @throws NullPointerException predicate가 null인 경우
     */
    DataSource filter(Predicate<DataRow> predicate);
    
    /**
     * 모든 데이터를 List로 반환합니다.
     * <p>
     * 이는 terminal operation으로, 실제 데이터 처리가 수행됩니다.
     * 반환된 리스트는 수정할 수 없습니다.
     * </p>
     * 
     * @return 데이터 행의 불변 리스트
     */
    List<DataRow> toList();
}