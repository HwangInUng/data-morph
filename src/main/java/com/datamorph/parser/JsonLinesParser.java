package com.datamorph.parser;

import com.datamorph.core.DataRow;
import com.datamorph.exceptions.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * JSON Lines 형태의 데이터를 스트리밍으로 파싱하는 클래스
 * 각 라인이 독립적인 JSON 객체인 경우에 사용합니다.
 * 
 * 예시:
 * {"name": "John", "age": 30}
 * {"name": "Jane", "age": 25}
 * {"name": "Bob", "age": 35}
 */
public class JsonLinesParser extends JsonParser {
    
    @Override
    public Stream<DataRow> parseAsStream(InputStream input) throws ParseException {
        if (input == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8));
        
        try {
            JsonLinesIterator iterator = new JsonLinesIterator(reader);
            
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 
                    Spliterator.ORDERED | Spliterator.NONNULL), false)
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        System.err.println("Warning: Failed to close BufferedReader: " + e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            try {
                reader.close();
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw createParseException("Failed to initialize JSON Lines parser: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supportsStreamingParsing() {
        return true;
    }
    
    @Override
    public int getRecommendedBufferSize() {
        return 8192; // 8KB for JSON Lines
    }
    
    /**
     * 단일 JSON 객체 라인을 DataRow로 파싱
     */
    private DataRow parseJsonObjectLine(String jsonLine) throws ParseException {
        if (jsonLine == null || jsonLine.trim().isEmpty()) {
            throw createParseException("Empty JSON line");
        }
        
        try {
            String trimmedLine = jsonLine.trim();
            
            // JSON 객체인지 확인 (중괄호로 시작하고 끝나는지)
            if (!trimmedLine.startsWith("{") || !trimmedLine.endsWith("}")) {
                throw createParseException("Each line must be a valid JSON object starting with '{' and ending with '}'");
            }
            
            // JsonParser의 convertJsonToDataRow 메서드를 사용하여 개별 객체 파싱
            return convertJsonToDataRow(trimmedLine);
            
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw e;
            }
            throw createParseException("Failed to parse JSON object: " + e.getMessage(), e);
        }
    }
    
    /**
     * JSON Lines를 행별로 처리하는 Iterator
     */
    private class JsonLinesIterator implements Iterator<DataRow> {
        private final BufferedReader reader;
        private String nextLine;
        private boolean hasCheckedNext = false;
        private int currentLineNumber = 0;
        
        public JsonLinesIterator(BufferedReader reader) {
            this.reader = reader;
        }
        
        @Override
        public boolean hasNext() {
            if (!hasCheckedNext) {
                try {
                    nextLine = reader.readLine();
                    hasCheckedNext = true;
                    
                    // 빈 행들은 건너뛰기
                    while (nextLine != null && nextLine.trim().isEmpty()) {
                        currentLineNumber++;
                        nextLine = reader.readLine();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read next line", e);
                }
            }
            return nextLine != null;
        }
        
        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more lines to read");
            }
            
            try {
                String line = nextLine;
                hasCheckedNext = false;
                currentLineNumber++;
                
                // 각 라인을 개별 JSON 객체로 파싱
                try {
                    DataRow dataRow = parseJsonObjectLine(line);
                    return dataRow;
                } catch (ParseException e) {
                    throw new RuntimeException("Invalid JSON at line " + currentLineNumber + ": " + line, e);
                }
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON at line " + currentLineNumber + ": " + e.getMessage(), e);
            }
        }
    }
}
