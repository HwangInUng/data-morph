package com.datamorph.core;

import java.io.File;

/**
 * DataMorph 라이브러리의 메인 진입점
 */
public final class DataMorph {
    
    private DataMorph() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 파일 경로로부터 DataSource를 생성합니다.
     */
    public static DataSource from(String filePath) {
        // Validation
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        File file = new File(filePath);
        
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
        
        // TODO: 실제 DataSource 구현체를 반환해야 함
        // 일단 테스트를 통과하기 위해 임시 구현체 반환
        return new DataSource() {};
    }
    
    /**
     * 문자열 content로부터 DataSource를 생성합니다.
     */
    public static DataSource fromString(String content, Format format) {
        // Validation
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        if (format == null) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        
        // TODO: 실제 DataSource 구현체를 반환해야 함
        // 일단 테스트를 통과하기 위해 임시 구현체 반환
        return new DataSource() {};
    }
}
