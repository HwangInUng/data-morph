package com.datamorph.parser;

import com.datamorph.core.Format;
import com.datamorph.util.FormatDetector;

/**
 * 데이터 포맷에 따른 적절한 Parser 인스턴스를 생성하는 팩토리 클래스
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
}
