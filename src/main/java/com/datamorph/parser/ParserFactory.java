package com.datamorph.parser;

import com.datamorph.core.Format;
import com.datamorph.util.FormatDetector;

/**
 * 데이터 포맷에 따른 적절한 Parser 인스턴스를 생성하는 팩토리 클래스
 * 
 * @version 1.1.0
 * @since 2025.06.25
 */
public final class ParserFactory {
    
    private ParserFactory() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 지정된 포맷에 대응하는 Parser 인스턴스를 생성합니다.
     * 
     * @param format 데이터 포맷
     * @return 해당 포맷에 맞는 Parser 인스턴스
     * @throws IllegalArgumentException format이 null이거나 지원하지 않는 포맷인 경우
     */
    public static Parser createParser(Format format) {
        if (format == null) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        
        return switch (format) {
            case CSV -> new CsvParser();
            case JSON -> new JsonParser();
        };
    }
    
    /**
     * 파일 경로를 기반으로 포맷을 자동 감지하여 Parser 인스턴스를 생성합니다.
     * 
     * @param filePath 파일 경로
     * @return 자동 감지된 포맷에 맞는 Parser 인스턴스
     * @throws IllegalArgumentException 파일 경로가 null이거나 지원하지 않는 포맷인 경우
     */
    public static Parser createParserFromPath(String filePath) {
        Format format = FormatDetector.detectFromExtension(filePath);
        return createParser(format);
    }
    
    /**
     * 지정된 포맷이 스트리밍 파싱을 지원하는지 확인합니다.
     * 
     * @param format 확인할 데이터 포맷
     * @return 스트리밍 파싱 지원 여부
     * @throws IllegalArgumentException format이 null인 경우
     * @since 1.1.0
     */
    public static boolean supportsStreamingParsing(Format format) {
        if (format == null) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        
        Parser parser = createParser(format);
        return parser.supportsStreamingParsing();
    }
    
    /**
     * 지정된 포맷의 권장 버퍼 크기를 반환합니다.
     * 
     * @param format 데이터 포맷
     * @return 권장 버퍼 크기 (바이트)
     * @throws IllegalArgumentException format이 null인 경우
     * @since 1.1.0
     */
    public static int getRecommendedBufferSize(Format format) {
        if (format == null) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        
        Parser parser = createParser(format);
        return parser.getRecommendedBufferSize();
    }
}
