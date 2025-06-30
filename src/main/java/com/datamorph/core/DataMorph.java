package com.datamorph.core;

import com.datamorph.exceptions.ParseException;
import com.datamorph.mapper.ObjectMapper;
import com.datamorph.exceptions.ObjectMappingException;
import com.datamorph.parser.Parser;
import com.datamorph.parser.ParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * DataMorph 라이브러리의 메인 진입점
 * <p>
 * 이 클래스는 다양한 데이터 소스로부터 DataSource 객체를 생성하는
 * 정적 팩토리 메서드들을 제공합니다.
 *
 * @version 1.0.0
 * @since 2025.06.25
 */
public final class DataMorph {

	private DataMorph () {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * 파일 경로로부터 DataSource를 생성합니다.
	 * 파일 확장자를 기반으로 자동으로 포맷을 감지합니다.
	 *
	 * @param filePath 데이터 파일의 경로
	 * @return 파싱된 데이터를 포함하는 DataSource
	 * @throws IllegalArgumentException 파일 경로가 null이거나 빈 문자열인 경우,
	 *                                  파일이 존재하지 않거나 파일이 아닌 경우,
	 *                                  지원하지 않는 파일 포맷인 경우
	 * @throws ParseException           파일 파싱 중 오류가 발생한 경우
	 */
	public static DataSource from (String filePath) throws ParseException {
		validateFilePath(filePath);

		File file = new File(filePath);
		validateFile(file);

		try {
			Parser parser = ParserFactory.createParserFromPath(filePath);
			List<DataRow> rows;

			try (FileInputStream inputStream = new FileInputStream(file)) {
				rows = parser.parse(inputStream);
			}

			return new ListDataSource(rows);
		} catch (IOException e) {
			throw new ParseException("Failed to read file: " + filePath, e);
		} catch (ParseException e) {
			throw e; // ParseException은 그대로 전파
		} catch (Exception e) {
			throw new ParseException("Failed to parse file: " + filePath, e);
		}
	}

	/**
	 * 문자열 content로부터 DataSource를 생성합니다.
	 *
	 * @param content 파싱할 데이터 문자열
	 * @param format  데이터의 포맷
	 * @return 파싱된 데이터를 포함하는 DataSource
	 * @throws IllegalArgumentException content가 null이거나 format이 null인 경우
	 * @throws ParseException           파싱 중 오류가 발생한 경우
	 */
	public static DataSource fromString (String content, Format format) throws ParseException {
		validateContent(content);
		validateFormat(format);

		try {
			Parser parser = ParserFactory.createParser(format);
			List<DataRow> rows = parser.parse(content);

			return new ListDataSource(rows);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException("Failed to parse content with format: " + format, e);
		}
	}

	/**
	 * POJO 객체 리스트로부터 DataSource를 생성합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * List<Employee> employees = Arrays.asList(
	 *     new Employee("John", 30, 50000),
	 *     new Employee("Jane", 25, 45000)
	 * );
	 * DataSource dataSource = DataMorph.fromObjects(employees);
	 * }</pre>
	 * </p>
	 *
	 * @param objects POJO 객체 리스트
	 * @param <T>     객체 타입
	 * @return 변환된 DataSource
	 * @throws IllegalArgumentException objects가 null인 경우
	 * @throws RuntimeException         객체 변환 중 오류가 발생한 경우
	 */
	public static <T> DataSource fromObjects (List<T> objects) {
		if (objects == null) {
			throw new IllegalArgumentException("Objects list cannot be null");
		}

		ObjectMapper mapper = new ObjectMapper();
		List<DataRow> rows = objects.stream()
									.map(obj -> {
										try {
											return mapper.toDataRow(obj);
										} catch (ObjectMappingException e) {
											throw new RuntimeException("Failed to convert object to DataRow", e);
										}
									})
									.toList();

		return new ListDataSource(rows);
	}

	/**
	 * 파일 경로 유효성을 검증합니다.
	 */
	private static void validateFilePath (String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("File path cannot be null or empty");
		}
	}

	/**
	 * 파일 존재성 및 유효성을 검증합니다.
	 */
	private static void validateFile (File file) {
		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
		}

		if (!file.isFile()) {
			throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
		}

		if (!file.canRead()) {
			throw new IllegalArgumentException("Cannot read file: " + file.getAbsolutePath());
		}
	}

	/**
	 * 컨텐츠 유효성을 검증합니다.
	 */
	private static void validateContent (String content) {
		if (content == null) {
			throw new IllegalArgumentException("Content cannot be null");
		}
	}

	/**
	 * 포맷 유효성을 검증합니다.
	 */
	private static void validateFormat (Format format) {
		if (format == null) {
			throw new IllegalArgumentException("Format cannot be null");
		}
	}
}
