package com.datamorph.parser;

import com.datamorph.core.DataRow;
import com.datamorph.error.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser 인터페이스의 추상 구현체
 * 공통적인 파싱 로직과 유틸리티 메서드를 제공
 */
public abstract class AbstractParser implements Parser {
	private static final Pattern BOOLEAN_TRUE = Pattern.compile("^(true|1|yes|y)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern BOOLEAN_FALSE = Pattern.compile("^(false|0|no|n)$", Pattern.CASE_INSENSITIVE);

	/**
	 * InputStream을 문자열로 변환하는 공통 메서드
	 *
	 * @param input 입력 스트림
	 * @return 변환된 문자열
	 * @throws ParseException 읽기 실패 시
	 */
	protected String readInputStream (InputStream input) {
		if (input == null) {
			throw new IllegalArgumentException("InputStream cannot be null");
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			return content.toString().trim();
		} catch (IOException e) {
			throw new ParseException("Failed to read from InputStream", e);
		}
	}

	/**
	 * 입력 내용의 유효성을 검증하는 공통 메서드
	 *
	 * @param content    검증할 내용
	 * @param formatName 포맷 이름 (오류 메시지용)
	 * @throws IllegalArgumentException 내용이 null인 경우
	 */
	protected void validateInput (String content, String formatName) {
		if (content == null) {
			throw new IllegalArgumentException(formatName + " content cannot be null");
		}
	}

	/**
	 * 빈 내용에 대한 처리를 담당하는 메서드
	 *
	 * @param content 검사할 내용
	 * @return 빈 내용인 경우 true
	 */
	protected boolean isEmpty (String content) {
		return content.trim().isEmpty();
	}

	/**
	 * 빈 내용일 때 반환할 기본 결과
	 *
	 * @return 빈 DataRow 리스트
	 */
	protected List<DataRow> getEmptyResult () {
		return new ArrayList<>();
	}

	/**
	 * ParseException을 생성하는 헬퍼 메서드
	 *
	 * @param message 오류 메시지
	 * @param cause   원인 예외
	 * @return ParseException 인스턴스
	 */
	protected ParseException createParseException (String message, Throwable cause) {
		return new ParseException(message, cause);
	}

	/**
	 * ParseException을 생성하는 헬퍼 메서드
	 *
	 * @param message 오류 메시지
	 * @return ParseException 인스턴스
	 */
	protected ParseException createParseException (String message) {
		return new ParseException(message);
	}

	/**
	 * 문자열 내용을 파싱하는 추상 메서드
	 * 구현체에서 실제 파싱 로직을 제공해야 함
	 *
	 * @param content 파싱할 문자열 내용
	 * @return 파싱된 DataRow 리스트
	 */
	@Override
	public abstract List<DataRow> parse (String content);

	/**
	 * InputStream을 파싱하는 기본 구현
	 * 대부분의 파서에서 공통으로 사용할 수 있는 구현
	 *
	 * @param input 입력 스트림
	 * @return 파싱된 DataRow 리스트
	 */
	@Override
	public List<DataRow> parse (InputStream input) {
		String content = readInputStream(input);
		if (isEmpty(content)) {
			return getEmptyResult();
		}
		return parse(content);
	}

	/**
	 * 문자열이 정수인지 확인
	 *
	 * @param value 확인할 문자열
	 * @return 정수인 경우 true
	 */
	protected boolean isInteger (String value) {
		if (value == null || value.trim().isEmpty()) {
			return false;
		}
		try {
			Integer.parseInt(value.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 문자열이 실수인지 확인
	 *
	 * @param value 확인할 문자열
	 * @return 실수인 경우 true
	 */
	protected boolean isDouble (String value) {
		if (value == null || value.trim().isEmpty()) {
			return false;
		}
		try {
			Double.parseDouble(value.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 문자열이 불린 값인지 확인
	 *
	 * @param value 확인할 문자열
	 * @return 불린 값인 경우 true
	 */
	protected boolean isBoolean (String value) {
		if (value == null || value.trim().isEmpty()) {
			return false;
		}
		String trimmed = value.trim().toLowerCase();
		return BOOLEAN_TRUE.matcher(trimmed).matches() || BOOLEAN_FALSE.matcher(trimmed).matches();
	}

	/**
	 * 문자열을 정수로 변환
	 *
	 * @param value 변환할 문자열
	 * @return 변환된 정수값
	 * @throws NumberFormatException 변환 실패 시
	 */
	protected Integer parseInteger (String value) {
		return Integer.parseInt(value.trim());
	}

	/**
	 * 문자열을 실수로 변환
	 *
	 * @param value 변환할 문자열
	 * @return 변환된 실수값
	 * @throws NumberFormatException 변환 실패 시
	 */
	protected Double parseDouble (String value) {
		return Double.parseDouble(value.trim());
	}

	/**
	 * 문자열을 불린 값으로 변환
	 *
	 * @param value 변환할 문자열
	 * @return 변환된 불린값
	 */
	protected Boolean parseBoolean (String value) {
		return BOOLEAN_TRUE.matcher(value).matches();
	}
}
