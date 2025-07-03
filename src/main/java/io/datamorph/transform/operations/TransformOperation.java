package io.datamorph.transform.operations;

import io.datamorph.core.DataRow;

/**
 * Transform 연산을 정의하는 인터페이스
 * 
 * @version 1.0.0
 * @since 2025.06.30
 */
public interface TransformOperation {
    
    /**
     * 주어진 DataRow에 변환을 적용합니다.
     * 
     * @param row 변환할 DataRow
     * @return 변환된 DataRow (새로운 인스턴스)
     */
    DataRow apply(DataRow row);
    
    /**
     * 이 연산에 대한 설명을 반환합니다.
     * 
     * @return 연산 설명
     */
    String getDescription();
}
