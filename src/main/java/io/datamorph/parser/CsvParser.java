package io.datamorph.parser;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CsvParser extends AbstractParser {
	private char delimiter = ',';
	private static final int ONLY_HEADER = 1;

	public CsvParser () {
	}

	private CsvParser (char delimiter) {
		this.delimiter = delimiter;
	}

	@Override
	public List<DataRow> parse (String content) {
		validateInput(content, "CSV");

		if (isEmpty(content)) {
			return getEmptyResult();
		}

		List<String> lines = splitIntoLines(content);

		if (lines.isEmpty()) {
			return getEmptyResult();
		}

		String headerLine = lines.get(0);

		if (!isValidCsvFormat(headerLine)) {
			throw createParseException("Invalid CSV format");
		}

		String[] headers = parseFields(headerLine);

		if (lines.size() == ONLY_HEADER) {
			return getEmptyResult();
		}

		List<DataRow> result = new ArrayList<>();

		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				continue;
			}

			String[] fields = parseFields(line);

			if (fields.length != headers.length) {
				throw createParseException("Column count mismatch at line " + (i + 1) +
						". Expected: " + headers.length + " columns, Got: " + fields.length + " columns");
			}

			DataRow row = new DataRow();
			for (int j = 0; j < headers.length; j++) {
				String header = headers[j].trim();
				String value = fields[j];

				Object convertedValue = processFieldValue(value);
				row.set(header, convertedValue);
			}
			result.add(row);
		}

		return result;
	}

	@Override
	public Stream<DataRow> parseAsStream(InputStream input) throws ParseException {
		if (input == null) {
			throw new IllegalArgumentException("InputStream cannot be null");
		}
		
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(input, StandardCharsets.UTF_8));
		
		try {
			// 헤더 행 읽기
			String headerLine = reader.readLine();
			if (headerLine == null || headerLine.trim().isEmpty()) {
				reader.close();
				return Stream.empty();
			}
			
			if (!isValidCsvFormat(headerLine)) {
				reader.close();
				throw createParseException("Invalid CSV format");
			}
			
			String[] headers = parseFields(headerLine);
			if (headers.length == 0) {
				reader.close();
				return Stream.empty();
			}
			
			// 데이터 행 반복자 생성
			CsvRowIterator iterator = new CsvRowIterator(reader, headers);
			
			// Stream 생성 (자동 리소스 관리 포함)
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
				
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException closeException) {
				e.addSuppressed(closeException);
			}
			throw createParseException("Failed to read CSV header: " + e.getMessage());
		}
	}
	
	@Override
	public boolean supportsStreamingParsing() {
		return true;
	}
	
	@Override
	public int getRecommendedBufferSize() {
		return 16384; // 16KB for CSV
	}

	/**
	 * 필드 값을 적절히 처리하는 메서드
	 * - 따옴표 안의 공백 유지
	 * - 빈 따옴표 ""를 빈 문자열로 처리
	 */
	private Object processFieldValue (String value) {
		if (value == null) {
			return null;
		}

		if (value.isEmpty()) {
			return null;
		}

		if (value.startsWith("\"") && value.endsWith("\"")) {
			if (value.length() == 2) {
				return "";
			}

			String innerValue = value.substring(1, value.length() - 1);

			innerValue = innerValue.replace("\"\"", "\"");

			return innerValue;
		}

		String trimmed = value.trim();

		if (trimmed.isEmpty()) {
			return null;
		}

		return convertValue(trimmed);
	}

	/**
	 * CSV 내용을 라인별로 분할 (따옴표 안의 줄바꿈 고려)
	 */
	private List<String> splitIntoLines (String content) {
		List<String> lines = new ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < content.length(); i++) {
			char ch = content.charAt(i);
			boolean isQuote = ch == '"';

			if (isQuote) {
				if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
					currentLine.append(ch).append(ch);
					i++;
				} else {
					inQuotes = !inQuotes;
					currentLine.append(ch);
				}
			} else if (ch == '\n' || ch == '\r') {
				if (inQuotes) {
					currentLine.append(ch);
				} else {
					lines.add(currentLine.toString());
					currentLine.setLength(0);

					// \r\n 처리
					if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
						i++;
					}
				}
			} else {
				currentLine.append(ch);
			}
		}

		// 마지막 라인 추가
		if (!currentLine.isEmpty()) {
			lines.add(currentLine.toString());
		}

		return lines;
	}

	/**
	 * 기본적인 CSV 형식인지 검증
	 */
	private boolean isValidCsvFormat (String line) {
		return !line.matches(".*[!@#$%^&*()]+.*");
	}

	public CsvParser withDelimiter (char delimiter) {
		return new CsvParser(delimiter);
	}

	/**
	 * CSV 라인을 필드 배열로 파싱
	 * 따옴표, 이스케이프 처리를 포함
	 */
	private String[] parseFields (String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder currentField = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			boolean isQuote = ch == '"';

			if (isQuote) {
				if (inQuotes) {
					// 다음 문자가 따옴표인지 확인 (이스케이프)
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						currentField.append('"').append('"'); // 이스케이프된 따옴표 유지
						i++; // 다음 따옴표 건너뛰기
					} else {
						inQuotes = false;
						currentField.append(ch);
					}
				} else {
					inQuotes = true;
					currentField.append(ch);
				}
			} else if (ch == delimiter && !inQuotes) {
				String field = currentField.toString();

				fields.add(field);
				currentField.setLength(0);
			} else {
				currentField.append(ch);
			}
		}

		String finalField = currentField.toString();
		fields.add(finalField);

		return fields.toArray(new String[0]);
	}

	/**
	 * 문자열 값을 적절한 데이터 타입으로 변환
	 * 
	 * 우선순위: 숫자(Integer/Double) > 불린 > 문자열
	 * 이렇게 하면 "0"은 정수 0으로, "false"는 불린 false로 변환됩니다.
	 */
	private Object convertValue (String value) {
		if (value == null) {
			return null;
		}

		if (value.isEmpty()) {
			return "";
		}

		String trimmed = value.trim();

		// 1순위: 숫자 타입 체크 (정수 먼저)
		if (isInteger(trimmed)) {
			return parseInteger(trimmed);
		}

		if (isDouble(trimmed)) {
			return parseDouble(trimmed);
		}

		// 2순위: 불린 타입 체크 (숫자가 아닌 경우에만)
		if (isBoolean(trimmed)) {
			return parseBoolean(trimmed);
		}

		// 3순위: 문자열
		return trimmed;
	}
	
	/**
	 * CSV 행을 반복 처리하는 Iterator 구현체
	 */
	private class CsvRowIterator implements Iterator<DataRow> {
		private final BufferedReader reader;
		private final String[] headers;
		private String nextLine;
		private boolean hasCheckedNext = false;
		private int currentLineNumber = 1; // 헤더 다음부터 시작
		
		public CsvRowIterator(BufferedReader reader, String[] headers) {
			this.reader = reader;
			this.headers = headers;
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
				throw new NoSuchElementException("No more rows to read");
			}
			
			try {
				String line = nextLine;
				hasCheckedNext = false;
				currentLineNumber++;
				
				String[] fields = parseFields(line);
				
				if (fields.length != headers.length) {
					throw new RuntimeException("Column count mismatch at line " + currentLineNumber +
							". Expected: " + headers.length + " columns, Got: " + fields.length + " columns");
				}
				
				DataRow row = new DataRow();
				for (int i = 0; i < headers.length; i++) {
					String header = headers[i].trim();
					String value = fields[i];
					
					Object convertedValue = processFieldValue(value);
					row.set(header, convertedValue);
				}
				
				return row;
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse line " + currentLineNumber + ": " + e.getMessage(), e);
			}
		}
	}
}
