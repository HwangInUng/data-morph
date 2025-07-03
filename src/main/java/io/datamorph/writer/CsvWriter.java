package io.datamorph.writer;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.WriteException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV 포맷으로 데이터를 쓰는 Writer 구현체
 * <p>
 * RFC 4180 CSV 표준을 따라 구현되었습니다.
 * - 필드는 쉼표로 구분
 * - 필드에 쉼표, 개행문자, 따옴표가 포함된 경우 따옴표로 감쌈
 * - 필드 내의 따옴표는 두 개의 따옴표로 이스케이프
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.02
 */
public class CsvWriter extends AbstractWriter {
	
	private static final String DELIMITER = ",";
	private static final String QUOTE = "\"";
	private static final String ESCAPED_QUOTE = "\"\"";
	private static final String LINE_SEPARATOR = "\n";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(List<DataRow> data, OutputStream output) throws WriteException {
		validateInputs(data, output);
		
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
			
			if (data.isEmpty()) {
				return;
			}
			
			Set<String> allColumns = extractAllColumns(data);
			List<String> sortedColumns = new ArrayList<>(allColumns);
			Collections.sort(sortedColumns);
			
			writeHeader(writer, sortedColumns);
			
			for (DataRow row : data) {
				writeDataRow(writer, row, sortedColumns);
			}
			
		} catch (IOException e) {
			throw new WriteException("Failed to write CSV data", e);
		}
	}
	
	/**
	 * 모든 데이터 행에서 컬럼명을 추출합니다.
	 *
	 * @param data 데이터 리스트
	 * @return 모든 컬럼명의 집합
	 */
	private Set<String> extractAllColumns(List<DataRow> data) {
		Set<String> allColumns = new LinkedHashSet<>();
		for (DataRow row : data) {
			allColumns.addAll(row.getFieldNames());
		}
		return allColumns;
	}
	
	/**
	 * CSV 헤더를 씁니다.
	 *
	 * @param writer 출력 Writer
	 * @param columns 컬럼명 리스트
	 * @throws IOException 쓰기 중 오류가 발생한 경우
	 */
	private void writeHeader(BufferedWriter writer, List<String> columns) throws IOException {
		for (int i = 0; i < columns.size(); i++) {
			if (i > 0) {
				writer.write(DELIMITER);
			}
			writer.write(escapeCsvField(columns.get(i)));
		}
		writer.write(LINE_SEPARATOR);
	}
	
	/**
	 * 데이터 행을 씁니다.
	 *
	 * @param writer 출력 Writer
	 * @param row 데이터 행
	 * @param columns 컬럼명 리스트
	 * @throws IOException 쓰기 중 오류가 발생한 경우
	 */
	private void writeDataRow(BufferedWriter writer, DataRow row, List<String> columns) throws IOException {
		for (int i = 0; i < columns.size(); i++) {
			if (i > 0) {
				writer.write(DELIMITER);
			}
			
			String columnName = columns.get(i);
			Object value = row.getObject(columnName);
			String stringValue = value != null ? value.toString() : "";
			
			writer.write(escapeCsvField(stringValue));
		}
		writer.write(LINE_SEPARATOR);
	}
	
	/**
	 * CSV 필드를 이스케이프 처리합니다.
	 * RFC 4180 표준을 따라 필요한 경우 따옴표로 감싸고 내부 따옴표를 이스케이프합니다.
	 *
	 * @param field 이스케이프할 필드
	 * @return 이스케이프된 필드
	 */
	private String escapeCsvField(String field) {
		if (field == null || field.isEmpty()) {
			return "";
		}
		
		boolean needsQuoting = field.contains(DELIMITER)
				|| field.contains(LINE_SEPARATOR) 
				|| field.contains("\r")
				|| field.contains(QUOTE);
		
		if (needsQuoting) {
			String escapedField = field.replace(QUOTE, ESCAPED_QUOTE);

			return QUOTE + escapedField + QUOTE;
		}
		
		return field;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSupportedExtension() {
		return "csv";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMimeType() {
		return "text/csv";
	}
}
