package io.datamorph.mapper;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.ObjectMappingException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * DataRow와 POJO 간의 변환을 담당하는 매퍼 클래스
 * <p>
 * 이 클래스는 리플렉션을 사용하여 DataRow 객체를 POJO로 변환하거나
 * POJO 객체를 DataRow로 변환하는 기능을 제공합니다.
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class ObjectMapper {

	private static final Set<Class<?>> PRIMITIVE_WRAPPER_TYPES = Set.of(
			Boolean.class, Byte.class, Character.class, Double.class,
			Float.class, Integer.class, Long.class, Short.class
	);

	private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
			DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("yyyy-MM-dd"),
			DateTimeFormatter.ofPattern("MM/dd/yyyy"),
			DateTimeFormatter.ofPattern("dd/MM/yyyy"),
			DateTimeFormatter.ofPattern("yyyy/MM/dd")
	);

	private static final List<DateTimeFormatter> DATETIME_FORMATTERS = Arrays.asList(
			DateTimeFormatter.ISO_LOCAL_DATE_TIME,
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
			DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
	);

	/**
	 * DataRow를 지정된 클래스의 인스턴스로 변환합니다.
	 *
	 * @param dataRow     변환할 DataRow
	 * @param targetClass 변환 대상 클래스
	 * @param <T>         변환 대상 타입
	 * @return 변환된 객체
	 * @throws ObjectMappingException 변환 중 오류가 발생한 경우
	 */
	public <T> T toObject (DataRow dataRow, Class<T> targetClass) throws ObjectMappingException {
		validateInputs(dataRow, targetClass);

		try {
			T instance = createInstance(targetClass);
			populateFields(dataRow, instance, targetClass);

			return instance;
		} catch (Exception e) {
			throw new ObjectMappingException(
					"Failed to convert DataRow to " + targetClass.getSimpleName(), e);
		}
	}

	/**
	 * POJO 객체를 DataRow로 변환합니다.
	 *
	 * @param object 변환할 객체
	 * @return 변환된 DataRow
	 * @throws ObjectMappingException 변환 중 오류가 발생한 경우
	 */
	public DataRow toDataRow (Object object) throws ObjectMappingException {
		if (object == null) {
			throw new IllegalArgumentException("Object cannot be null");
		}

		try {
			DataRow dataRow = new DataRow();
			Class<?> objectClass = object.getClass();
			Field[] fields = objectClass.getDeclaredFields();

			for (Field field : fields) {
				field.setAccessible(true);
				Object value = field.get(object);

				if (value != null) {
					dataRow.set(field.getName(), value);
				}
			}

			return dataRow;
		} catch (Exception e) {
			throw new ObjectMappingException(
					"Failed to convert " + object.getClass().getSimpleName() + " to DataRow", e);
		}
	}

	/**
	 * 입력값의 유효성을 검증합니다.
	 */
	private <T> void validateInputs (DataRow dataRow, Class<T> targetClass) {
		if (dataRow == null) {
			throw new IllegalArgumentException("DataRow cannot be null");
		}
		if (targetClass == null) {
			throw new IllegalArgumentException("Target class cannot be null");
		}
	}

	/**
	 * 지정된 클래스의 인스턴스를 생성합니다.
	 */
	private <T> T createInstance (Class<T> targetClass) throws ObjectMappingException {
		try {
			Constructor<T> constructor = targetClass.getDeclaredConstructor();
			constructor.setAccessible(true);

			return constructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new ObjectMappingException(
					"Class " + targetClass.getSimpleName() + " must have a default constructor", e);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ObjectMappingException(
					"Failed to create instance of " + targetClass.getSimpleName(), e);
		}
	}

	/**
	 * DataRow의 데이터를 객체의 필드에 할당합니다.
	 */
	private <T> void populateFields (DataRow dataRow, T instance, Class<T> targetClass)
			throws ObjectMappingException {
		Field[] fields = targetClass.getDeclaredFields();

		for (Field field : fields) {
			if (dataRow.has(field.getName())) {
				Object value = dataRow.getObject(field.getName());

				if (value != null) {
					setFieldValue(instance, field, value);
				}
			}
		}
	}

	/**
	 * 필드에 값을 설정합니다.
	 */
	private void setFieldValue (Object instance, Field field, Object value)
			throws ObjectMappingException {
		try {
			field.setAccessible(true);
			Object convertedValue = convertValue(value, field.getType());
			field.set(instance, convertedValue);
		} catch (IllegalAccessException e) {
			throw new ObjectMappingException(
					"Failed to set field " + field.getName(), e);
		}
	}

	/**
	 * 값을 대상 타입으로 변환합니다.
	 */
	private Object convertValue (Object value, Class<?> targetType) throws ObjectMappingException {
		if (value == null) {
			return null;
		}

		if (targetType.isAssignableFrom(value.getClass())) {
			return value;
		}

		if (targetType == String.class) {
			return value.toString();
		}

		if (targetType.isPrimitive() || PRIMITIVE_WRAPPER_TYPES.contains(targetType)) {
			return convertPrimitiveType(value, targetType);
		}

		if (targetType == BigDecimal.class) {
			return convertToBigDecimal(value);
		}

		if (targetType == LocalDate.class) {
			return convertToLocalDate(value);
		}

		if (targetType == LocalDateTime.class) {
			return convertToLocalDateTime(value);
		}

		return value;
	}

	/**
	 * 원시 타입 및 래퍼 타입으로 변환합니다.
	 */
	private Object convertPrimitiveType (Object value, Class<?> targetType)
			throws ObjectMappingException {
		String stringValue = value.toString().trim();

		try {
			if (targetType == boolean.class || targetType == Boolean.class) {
				return Boolean.valueOf(stringValue);
			}
			if (targetType == byte.class || targetType == Byte.class) {
				return Byte.valueOf(stringValue);
			}
			if (targetType == char.class || targetType == Character.class) {
				return stringValue.isEmpty() ? '\0' : stringValue.charAt(0);
			}
			if (targetType == short.class || targetType == Short.class) {
				return Short.valueOf(stringValue);
			}
			if (targetType == int.class || targetType == Integer.class) {
				return Integer.valueOf(stringValue);
			}
			if (targetType == long.class || targetType == Long.class) {
				return Long.valueOf(stringValue);
			}
			if (targetType == float.class || targetType == Float.class) {
				return Float.valueOf(stringValue);
			}
			if (targetType == double.class || targetType == Double.class) {
				return Double.valueOf(stringValue);
			}
		} catch (NumberFormatException e) {
			throw new ObjectMappingException(
					"Cannot convert '" + stringValue + "' to " + targetType.getSimpleName(), e);
		}

		throw new ObjectMappingException(
				"Unsupported primitive type: " + targetType.getSimpleName());
	}

	/**
	 * BigDecimal로 변환합니다.
	 */
	private BigDecimal convertToBigDecimal (Object value) throws ObjectMappingException {
		try {
			if (value instanceof Number number) {
				return BigDecimal.valueOf((number).doubleValue());
			}

			return new BigDecimal(value.toString().trim());
		} catch (NumberFormatException e) {
			throw new ObjectMappingException(
					"Cannot convert '" + value + "' to BigDecimal", e);
		}
	}

	/**
	 * LocalDate로 변환합니다.
	 */
	private LocalDate convertToLocalDate (Object value) throws ObjectMappingException {
		String stringValue = value.toString().trim();

		for (DateTimeFormatter formatter : DATE_FORMATTERS) {
			try {
				return LocalDate.parse(stringValue, formatter);
			} catch (DateTimeParseException ignored) {
				// 다음 포맷터로 시도
			}
		}

		throw new ObjectMappingException(
				"Cannot convert '" + stringValue + "' to LocalDate");
	}

	/**
	 * LocalDateTime으로 변환합니다.
	 */
	private LocalDateTime convertToLocalDateTime (Object value) throws ObjectMappingException {
		String stringValue = value.toString().trim();

		for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
			try {
				return LocalDateTime.parse(stringValue, formatter);
			} catch (DateTimeParseException ignored) {
				// 다음 포맷터로 시도
			}
		}

		throw new ObjectMappingException(
				"Cannot convert '" + stringValue + "' to LocalDateTime");
	}
}
