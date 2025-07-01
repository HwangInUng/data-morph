package com.datamorph.core;

/**
 * DataMorph가 지원하는 파일 포맷
 */
public enum Format {
    CSV,
    JSON;

    /**
     * 파일 확장자로부터 포맷을 추론합니다.
     *
     * @param filePath 파일 경로
     * @return 추론된 포맷
     * @throws IllegalArgumentException 지원하지 않는 확장자인 경우
     */
    public static Format fromExtension(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".csv")) {
            return CSV;
        } else if (lowerPath.endsWith(".json")) {
            return JSON;
        } else {
            throw new IllegalArgumentException("Unsupported file extension: " + filePath);
        }
    }
}
