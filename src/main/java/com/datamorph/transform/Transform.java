package com.datamorph.transform;

import com.datamorph.core.DataRow;
import com.datamorph.transform.operations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Transform 연산을 구성하고 실행하는 클래스
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class Transform {
	private final List<TransformOperation> operations;

	private Transform (List<TransformOperation> operations) {
		this.operations = new ArrayList<>(operations);
	}

	/**
	 * 새로운 TransformBuilder를 생성합니다.
	 *
	 * @return TransformBuilder 인스턴스
	 */
	public static TransformBuilder builder () {
		return new TransformBuilder();
	}

	/**
	 * 주어진 DataRow에 모든 변환 연산을 적용합니다.
	 *
	 * @param row 변환할 DataRow
	 * @return 변환된 DataRow
	 */
	public DataRow apply (DataRow row) {
		DataRow result = row;

		for (TransformOperation operation : operations) {
			result = operation.apply(result);
		}

		return result;
	}

	/**
	 * 등록된 모든 연산들의 설명을 반환합니다.
	 *
	 * @return 연산 설명 리스트
	 */
	public List<String> getOperationDescriptions () {
		return operations.stream()
						 .map(TransformOperation::getDescription)
						 .toList();
	}

	/**
	 * Transform을 구성하는 빌더 클래스
	 */
	public static class TransformBuilder {
		private final List<TransformOperation> operations = new ArrayList<>();

		/**
		 * 필드 이름을 변경합니다.
		 *
		 * @param oldName 기존 필드명
		 * @param newName 새로운 필드명
		 * @return TransformBuilder (체이닝용)
		 */
		public TransformBuilder rename (String oldName, String newName) {
			operations.add(new RenameOperation(oldName, newName));
			return this;
		}

		/**
		 * 새로운 필드를 추가합니다.
		 *
		 * @param fieldName 필드명
		 * @param value     필드 값
		 * @return TransformBuilder (체이닝용)
		 */
		public TransformBuilder add (String fieldName, Object value) {
			operations.add(new AddOperation(fieldName, value));
			return this;
		}

		/**
		 * 필드를 제거합니다.
		 *
		 * @param fieldName 제거할 필드명
		 * @return TransformBuilder (체이닝용)
		 */
		public TransformBuilder remove (String fieldName) {
			operations.add(new RemoveOperation(fieldName));
			return this;
		}

		/**
		 * 조건부 변환을 추가합니다.
		 *
		 * @param condition 조건
		 * @param action    액션
		 * @return TransformBuilder (체이닝용)
		 */
		public TransformBuilder when (Predicate<DataRow> condition, Function<DataRow, DataRow> action) {
			operations.add(new ConditionalOperation(condition, action));
			return this;
		}

		/**
		 * 조건부 변환을 추가합니다. (설명 포함)
		 *
		 * @param condition   조건
		 * @param action      액션
		 * @param description 설명
		 * @return TransformBuilder (체이닝용)
		 */
		public TransformBuilder when (Predicate<DataRow> condition, Function<DataRow, DataRow> action, String description) {
			operations.add(new ConditionalOperation(condition, action, description));
			return this;
		}

		/**
		 * 커스텀 Transform 연산을 추가합니다.
		 *
		 * @param operation 커스텀 연산
		 * @return TransformBuilder (체이닝용)
		 */
		public TransformBuilder custom (TransformOperation operation) {
			if (operation == null) {
				throw new TransformException("CUSTOM", null, "Operation cannot be null");
			}
			operations.add(operation);
			return this;
		}

		/**
		 * Transform 객체를 빌드합니다.
		 *
		 * @return Transform 인스턴스
		 */
		public Transform build () {
			return new Transform(operations);
		}
	}
}
