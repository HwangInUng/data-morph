package com.datamorph.writer;

import com.datamorph.core.DataRow;
import com.datamorph.exceptions.WriteException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JSON 포맷으로 데이터를 쓰는 Writer 구현체
 * <p>
 * JSON 배열 형태로 데이터를 출력합니다. 각 DataRow는 JSON 객체로 변환됩니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.02
 */
public class JsonWriter extends AbstractWriter {
	
	private static final String INDENT = "  ";
	private static final String LINE_SEPARATOR = "\n";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(List<DataRow> data, OutputStream output) throws WriteException {
		validateInputs(data, output);
		
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
			
			writeJsonArray(writer, data);
			
		} catch (IOException e) {
			throw new WriteException("Failed to write JSON data", e);
		}
	}
	
	/**
	 * JSON 배열을 씁니다.
	 *
	 * @param writer 출력 Writer
	 * @param data 데이터 리스트
	 * @throws IOException 쓰기 중 오류가 발생한 경우
	 */
	private void writeJsonArray(BufferedWriter writer, List<DataRow> data) throws IOException {
		writer.write("[");
		writer.write(LINE_SEPARATOR);
		
		for (int i = 0; i < data.size(); i++) {
			DataRow row = data.get(i);
			
			writer.write(INDENT);
			writeJsonObject(writer, row);
			
			if (i < data.size() - 1) {
				writer.write(",");
			}
			writer.write(LINE_SEPARATOR);
		}
		
		writer.write("]");
	}
	
	/**
	 * JSON 객체를 씁니다.
	 *
	 * @param writer 출력 Writer
	 * @param row 데이터 행
	 * @throws IOException 쓰기 중 오류가 발생한 경우
	 */
	private void writeJsonObject(BufferedWriter writer, DataRow row) throws IOException {
		writer.write("{");
		
		List<String> columnNames = row.getFieldNames().stream().sorted().toList();
		
		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			Object value = row.getObject(columnName);
			
			if (i > 0) {
				writer.write(",");
			}
			
			writer.write(LINE_SEPARATOR);
			writer.write(INDENT);
			writer.write(INDENT);
			
			// 키 쓰기
			writer.write(escapeJsonString(columnName));
			writer.write(": ");
			
			// 값 쓰기
			writeJsonValue(writer, value);
		}
		
		if (!columnNames.isEmpty()) {
			writer.write(LINE_SEPARATOR);
			writer.write(INDENT);
		}
		
		writer.write("}");
	}
	
	/**
	 * JSON 값을 씁니다.
	 *
	 * @param writer 출력 Writer
	 * @param value 값
	 * @throws IOException 쓰기 중 오류가 발생한 경우
	 */
	private void writeJsonValue(BufferedWriter writer, Object value) throws IOException {
		if (value == null) {
			writer.write("null");
		} else if (value instanceof String string) {
			writer.write(escapeJsonString(string));
		} else if (value instanceof Number) {
			writer.write(value.toString());
		} else if (value instanceof Boolean) {
			writer.write(value.toString());
		} else {
			// 기타 타입은 문자열로 변환
			writer.write(escapeJsonString(value.toString()));
		}
	}
	
	/**
	 * JSON 문자열을 이스케이프 처리합니다.
	 * RFC 7159 JSON 표준을 따라 특수문자를 이스케이프합니다.
	 *
	 * @param str 이스케이프할 문자열
	 * @return 이스케이프된 JSON 문자열
	 */
	private String escapeJsonString(String str) {
		if (str == null) {
			return "null";
		}
		
		StringBuilder escaped = new StringBuilder("\"");
		
		for (char c : str.toCharArray()) {
			switch (c) {
				case '"':
					escaped.append("\\\"");
					break;
				case '\\':
					escaped.append("\\\\");
					break;
				case '\b':
					escaped.append("\\b");
					break;
				case '\f':
					escaped.append("\\f");
					break;
				case '\n':
					escaped.append("\\n");
					break;
				case '\r':
					escaped.append("\\r");
					break;
				case '\t':
					escaped.append("\\t");
					break;
				default:
					if (c < ' ') {
						// 제어 문자는 유니코드 이스케이프로 처리
						escaped.append(String.format("\\u%04x", (int) c));
					} else {
						escaped.append(c);
					}
					break;
			}
		}
		
		escaped.append("\"");
		return escaped.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSupportedExtension() {
		return "json";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMimeType() {
		return "application/json";
	}
}
