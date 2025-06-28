package com.datamorph.core;

import java.util.*;

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
	 * @return 필드의 문자열 값, 필드가 존재하지 않거나 null이면 null 반환
	 */
	public String getString (String fieldName) {
		if (!has(fieldName)) {
			return null;
		}

		Object value = fields.get(fieldName);
		return value != null ? value.toString() : null;
	}

	/**
	 * 지정된 필드의 정수 값을 반환합니다.
	 *
	 * @param fieldName 필드명
	 * @return 필드의 정수 값, 필드가 존재하지 않거나 null일 경우 null 반환
	 * @throws IllegalArgumentException 필드를 정수로 변환할 수 없을 때
	 */
	public Integer getInt (String fieldName) {
		if (!has(fieldName)) {
			return null;
		}

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
	 * @return 필드의 boolean 값, 필드가 존재하지 않거나 null일 경우 null 반환
	 * @throws IllegalArgumentException 필드를 boolean 값으로 변환할 수 없을 때
	 */
	public Boolean getBoolean (String fieldName) {
		if (!has(fieldName)) {
			return null;
		}

		Object value = fields.get(fieldName);

		if (value == null) {
			return null;
		}

		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		if (value instanceof String) {
			String strValue = value.toString().trim().toLowerCase();
			if ("true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue) || "y".equals(strValue)) {
				return true;
			}

			if ("false".equals(strValue) || "0".equals(strValue) || "no".equals(strValue) || "n".equals(strValue)) {
				return false;
			}
		}

		throw new IllegalArgumentException(
				"Field '" + fieldName + "' cannot be converted to boolean: " + value
		);
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
	 * 현재 DataRow의 깊은 복사본을 생성합니다.
	 * 모든 필드 값이 복사되며, mutable 객체도 안전하게 복사됩니다.
	 *
	 * @return 복사된 DataRow
	 */
	public DataRow copy () {
		DataRow copy = new DataRow();
		for (Map.Entry<String, Object> entry : this.fields.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			copy.fields.put(key, deepCopyValue(value));
		}
		
		return copy;
	}
	
	/**
	 * 값을 깊은 복사합니다.
	 * 
	 * @param value 복사할 값
	 * @return 복사된 값
	 */
	@SuppressWarnings("unchecked")
	private Object deepCopyValue(Object value) {
		if (value == null) {
			return null;
		}
		
		// Immutable types - no need to copy
		if (value instanceof String || 
		    value instanceof Number || 
		    value instanceof Boolean ||
		    value instanceof Character) {
			return value;
		}
		
		// Collections
		if (value instanceof List) {
			List<Object> original = (List<Object>) value;
			List<Object> copy = new ArrayList<>(original.size());
			for (Object item : original) {
				copy.add(deepCopyValue(item));
			}
			return copy;
		}
		
		if (value instanceof Set) {
			Set<Object> original = (Set<Object>) value;
			Set<Object> copy = new LinkedHashSet<>(original.size());
			for (Object item : original) {
				copy.add(deepCopyValue(item));
			}
			return copy;
		}
		
		if (value instanceof Map) {
			Map<Object, Object> original = (Map<Object, Object>) value;
			Map<Object, Object> copy = new LinkedHashMap<>(original.size());
			for (Map.Entry<Object, Object> entry : original.entrySet()) {
				copy.put(deepCopyValue(entry.getKey()), deepCopyValue(entry.getValue()));
			}
			return copy;
		}
		
		// Arrays
		if (value.getClass().isArray()) {
			if (value instanceof Object[] objects) {
				Object[] original = objects;
				Object[] copy = new Object[original.length];
				for (int i = 0; i < original.length; i++) {
					copy[i] = deepCopyValue(original[i]);
				}
				return copy;
			}
			// Primitive arrays
			if (value instanceof int[] ints) {
				return ints.clone();
			}
			if (value instanceof long[] longs) {
				return longs.clone();
			}
			if (value instanceof double[] doubles) {
				return doubles.clone();
			}
			if (value instanceof float[] floats) {
				return floats.clone();
			}
			if (value instanceof boolean[] booleans) {
				return booleans.clone();
			}
			if (value instanceof byte[] bytes) {
				return bytes.clone();
			}
			if (value instanceof short[] shorts) {
				return shorts.clone();
			}
			if (value instanceof char[] chars) {
				return chars.clone();
			}
		}
		
		// Date/Time types (immutable in Java 8+)
		if (value instanceof java.time.LocalDate ||
		    value instanceof java.time.LocalDateTime ||
		    value instanceof java.time.LocalTime ||
		    value instanceof java.time.ZonedDateTime) {
			return value;
		}
		
		// Legacy Date (mutable)
		if (value instanceof Date date) {
			return new Date((date).getTime());
		}
		
		// For other objects, attempt to use clone if available
		if (value instanceof Cloneable) {
			try {
				java.lang.reflect.Method cloneMethod = value.getClass().getMethod("clone");
				return cloneMethod.invoke(value);
			} catch (Exception e) {
				return value;
			}
		}
		
		return value;
	}

	@Override
	public String toString () {
		return "DataRow :: " + fields;
	}
}