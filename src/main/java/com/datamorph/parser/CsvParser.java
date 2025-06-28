package com.datamorph.parser;

import com.datamorph.core.DataRow;

import java.util.ArrayList;
import java.util.List;

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
	 */
	private Object convertValue (String value) {
		if (value == null) {
			return null;
		}

		if (value.isEmpty()) {
			return "";
		}

		String trimmed = value.trim();

		if (isBoolean(trimmed)) {
			return parseBoolean(trimmed);
		}

		if (isInteger(trimmed)) {
			return parseInteger(trimmed);
		}

		if (isDouble(trimmed)) {
			return parseDouble(trimmed);
		}

		return trimmed;
	}
}
