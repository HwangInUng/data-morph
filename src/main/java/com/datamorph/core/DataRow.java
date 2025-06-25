package com.datamorph.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 단일 데이터 행을 표현하는 클래스
 *
 * @version 1.0.0
 * @since 2025.06.25
 */
public class DataRow {
	private final Map<String, Object> fields;

	public DataRow () {
		// 필드 순서 보장
		this.fields = new LinkedHashMap<>();
	}

	/**
	 * 지정된 필드의 문자열 값을 반환합니다.
	 *
	 * @param fieldName 필드명
	 * @return 필드의 문자열 값
	 * @throws IllegalArgumentException 필드가 존재하지 않을 때
	 */
	public String getString (String fieldName) {
		validateFieldExists(fieldName);
		Object value = fields.get(fieldName);
		return value != null ? value.toString() : null;
	}

	/**
	 * 지정된 필드의 정수 값을 반환합니다.
	 *
	 * @param fieldName 필드명
	 * @return 필드의 정수 값, null일 경우 null 반환
	 * @throws IllegalArgumentException 필드를 정수로 변환할 수 없을 때
	 */
	public Integer getInt (String fieldName) {
		validateFieldExists(fieldName);
		Object value = fields.get(fieldName);

		if (value == null) {
			return null;
		}

		if (value instanceof Integer integer) {
			return integer;
		}

		if (value instanceof Number integerAsNumber) {
			return integerAsNumber.intValue();
		}

		try {
			return Integer.parseInt(value.toString().trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"Field '" + fieldName + "' cannot be converted to int: " + value
			);
		}
	}

	/**
	 * 지정된 필드의 boolean 값을 반환합니다.
	 *
	 * @param fieldName 필드명
	 * @return 필드의 boolean 값, null일 경우 null 반환
	 * @throws IllegalArgumentException 필드를 boolean 값으로 변환할 수 없을 때
	 */
	public boolean getBoolean (String fieldName) {
		validateFieldExists(fieldName);
		Object value = fields.get(fieldName);

		if (value == null) {
			return false;
		}

		if (value instanceof String) {
			String strValue = value.toString().trim().toLowerCase();
			if ("true".equals(strValue) || "1".equals(strValue) || "y".equals(strValue)) {
				return true;
			}

			if ("false".equals(strValue) || "0".equals(strValue) || "n".equals(strValue)) {
				return false;
			}
		}

		try {
			return (Boolean) value;
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"Field '" + fieldName + "' cannot be converted to boolean: " + value
			);
		}
	}

	/**
	 * 필드에 값을 설정합니다.
	 *
	 * @param fieldName 필드명
	 * @param value     설정할 값
	 */
	public void set (String fieldName, Object value) {
		if (fieldName == null || fieldName.trim().isEmpty()) {
			throw new IllegalArgumentException("Field name cannot be null or empty");
		}
		fields.put(fieldName, value);
	}

	/**
	 * 필드의 존재 여부를 확인합니다.
	 *
	 * @param fieldName 필드명
	 * @return 필드가 존재하면 true
	 */
	public boolean has (String fieldName) {
		return fields.containsKey(fieldName);
	}

	/**
	 * 필드가 존재하는지 검증합니다.
	 *
	 * @param fieldName 필드명
	 * @throws IllegalArgumentException 필드가 존재하지 않을 때
	 */
	private void validateFieldExists (String fieldName) {
		if (!has(fieldName)) {
			throw new IllegalArgumentException("No such field: " + fieldName);
		}
	}

	@Override
	public String toString () {
		return "DataRow" + fields;
	}
}