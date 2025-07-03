package io.datamorph.core;

import io.datamorph.mapper.ObjectMapper;
import io.datamorph.exceptions.ObjectMappingException;
import io.datamorph.exceptions.WriteException;
import io.datamorph.transform.Transform;
import io.datamorph.writer.Writer;
import io.datamorph.writer.WriterFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 데이터 소스를 표현하는 인터페이스
 * <p>
 * Fluent API를 통해 데이터 변환, 필터링 등의 연산을 체이닝할 수 있습니다.
 * 모든 중간 연산(transform, filter)은 새로운 DataSource를 반환하며,
 * 원본 데이터는 변경되지 않습니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.06.25
 */
public interface DataSource {

	/**
	 * 각 데이터 행에 변환을 적용합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * dataSource.transform(row -> row.set("name", row.getString("name").toUpperCase()))
	 * }</pre>
	 * </p>
	 *
	 * @param transformer 각 행에 적용할 변환 함수
	 * @return 변환이 적용된 새로운 DataSource
	 * @throws NullPointerException transformer가 null인 경우
	 */
	DataSource transform (Consumer<DataRow> transformer);

	/**
	 * Transform 객체를 사용하여 구조화된 변환을 적용합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * dataSource.transform(Transform.builder()
	 *     .rename("emp_name", "name")
	 *     .add("bonus", 1000)
	 *     .build())
	 * }</pre>
	 * </p>
	 *
	 * @param transform 적용할 Transform 객체
	 * @return 변환이 적용된 새로운 DataSource
	 * @throws NullPointerException transform이 null인 경우
	 */
	DataSource transform (Transform transform);

	/**
	 * 주어진 조건에 맞는 행만 필터링합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * dataSource.filter(row -> row.getInt("age") > 20)
	 * }</pre>
	 * </p>
	 *
	 * @param predicate 필터 조건
	 * @return 필터링된 새로운 DataSource
	 * @throws NullPointerException predicate가 null인 경우
	 */
	DataSource filter (Predicate<DataRow> predicate);

	/**
	 * 모든 데이터를 List로 반환합니다.
	 * <p>
	 * 이는 terminal operation으로, 실제 데이터 처리가 수행됩니다.
	 * 반환된 리스트는 수정할 수 없습니다.
	 * </p>
	 *
	 * @return 데이터 행의 불변 리스트
	 */
	List<DataRow> toList ();

	/**
	 * 데이터를 지정된 클래스의 객체 리스트로 변환합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * List<Employee> employees = dataSource.toList(Employee.class);
	 * }</pre>
	 * </p>
	 *
	 * @param <T>         변환 대상 타입
	 * @param targetClass 변환 대상 클래스
	 * @return 변환된 객체의 리스트
	 * @throws RuntimeException         객체 변환 중 오류가 발생한 경우
	 * @throws IllegalArgumentException targetClass가 null인 경우
	 */
	default <T> List<T> toList (Class<T> targetClass) {
		if (targetClass == null) {
			throw new IllegalArgumentException("Target class cannot be null");
		}

		ObjectMapper mapper = new ObjectMapper();

		return toList().stream()
					   .map(dataRow -> {
						   try {
							   return mapper.toObject(dataRow, targetClass);
						   } catch (ObjectMappingException e) {
							   throw new RuntimeException("Failed to convert DataRow to "
									   + targetClass.getSimpleName(), e);
						   }
					   })
					   .toList();
	}

	/**
	 * 데이터를 파일에 저장합니다.
	 * <p>
	 * 파일 확장자를 기반으로 자동으로 포맷을 결정합니다.
	 * 예시:
	 * <pre>{@code
	 * dataSource.toFile("output.csv");  // CSV로 저장
	 * dataSource.toFile("data.json");   // JSON으로 저장
	 * }</pre>
	 * </p>
	 *
	 * @param filePath 저장할 파일 경로
	 * @throws IllegalArgumentException 파일 경로가 null이거나 빈 문자열인 경우, 지원하지 않는 확장자인 경우
	 * @throws RuntimeException 파일 쓰기 중 오류가 발생한 경우
	 */
	default void toFile(String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("File path cannot be null or empty");
		}

		try {
			Writer writer = WriterFactory.createWriterFromPath(filePath);
			List<DataRow> data = toList();
			
			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				writer.write(data, fos);
			}
		} catch (WriteException | IOException e) {
			throw new RuntimeException("Failed to write data to file: " + filePath, e);
		}
	}

	/**
	 * 데이터를 지정된 포맷의 파일에 저장합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * dataSource.toFile("output.txt", Format.CSV);  // CSV 포맷으로 저장
	 * dataSource.toFile("data.txt", Format.JSON);   // JSON 포맷으로 저장
	 * }</pre>
	 * </p>
	 *
	 * @param filePath 저장할 파일 경로
	 * @param format 저장할 포맷
	 * @throws IllegalArgumentException 파일 경로나 포맷이 null인 경우
	 * @throws RuntimeException 파일 쓰기 중 오류가 발생한 경우
	 */
	default void toFile(String filePath, Format format) {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("File path cannot be null or empty");
		}
		if (format == null) {
			throw new IllegalArgumentException("Format cannot be null");
		}

		try {
			Writer writer = WriterFactory.createWriter(format);
			List<DataRow> data = toList();
			
			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				writer.write(data, fos);
			}
		} catch (WriteException | IOException e) {
			throw new RuntimeException("Failed to write data to file: " + filePath, e);
		}
	}

	/**
	 * 데이터를 문자열로 변환합니다.
	 * <p>
	 * 예시:
	 * <pre>{@code
	 * String csvString = dataSource.toString(Format.CSV);
	 * String jsonString = dataSource.toString(Format.JSON);
	 * }</pre>
	 * </p>
	 *
	 * @param format 변환할 포맷
	 * @return 변환된 문자열
	 * @throws IllegalArgumentException 포맷이 null인 경우
	 * @throws RuntimeException 변환 중 오류가 발생한 경우
	 */
	default String toString(Format format) {
		if (format == null) {
			throw new IllegalArgumentException("Format cannot be null");
		}

		try {
			Writer writer = WriterFactory.createWriter(format);
			List<DataRow> data = toList();
			return writer.writeToString(data);
		} catch (WriteException e) {
			throw new RuntimeException("Failed to convert data to string", e);
		}
	}
}