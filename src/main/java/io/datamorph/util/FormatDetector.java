package io.datamorph.util;

import io.datamorph.core.Format;

import java.util.Locale;

/**
 * 파일 확장자를 기반으로 데이터 포맷을 감지하는 유틸리티 클래스
 */
public final class FormatDetector {
    
    private FormatDetector() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 파일 경로의 확장자를 분석하여 데이터 포맷을 감지합니다.
     * 
     * @param filePath 분석할 파일 경로
     * @return 감지된 데이터 포맷
     * @throws IllegalArgumentException 파일 경로가 null이거나 빈 문자열인 경우,
     *                                 또는 지원하지 않는 확장자인 경우
     */
    public static Format detectFromExtension(String filePath) {
        validateFilePath(filePath);
        
        String extension = extractExtension(filePath);
        return mapExtensionToFormat(extension);
    }
    
    /**
     * 파일 경로 유효성을 검증합니다.
     */
    private static void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
    }
    
    /**
     * 파일 경로에서 확장자를 추출합니다.
     */
    private static String extractExtension(String filePath) {
        String trimmedPath = filePath.trim();
        
        int lastDotIndex = trimmedPath.lastIndexOf('.');
        
        if (lastDotIndex == -1 || lastDotIndex == trimmedPath.length() - 1) {
            throw new IllegalArgumentException("No file extension found in: " + filePath);
        }
        
        String extension = trimmedPath.substring(lastDotIndex + 1);
        
        if (extension.trim().isEmpty()) {
            throw new IllegalArgumentException("No file extension found in: " + filePath);
        }
        
        return extension.toLowerCase(Locale.ROOT);
    }
    
    /**
     * 확장자를 Format enum으로 매핑합니다.
     */
    private static Format mapExtensionToFormat(String extension) {
		return switch (extension) {
			case "csv" -> Format.CSV;
			case "json" -> Format.JSON;
			default -> throw new IllegalArgumentException(
					"Unsupported file format: ." + extension +
							". Supported formats: .csv, .json"
			);
		};
    }
}
