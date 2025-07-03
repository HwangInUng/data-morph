package io.datamorph.transform;

import io.datamorph.core.DataRow;
import io.datamorph.core.DataSource;
import io.datamorph.core.ListDataSource;
import io.datamorph.exceptions.TransformException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Transform 시스템 테스트
 */
class TransformTest {

	@Nested
	@DisplayName("rename() 연산은")
	class RenameOperationTest {

		@Test
		@DisplayName("기존 필드명을 새로운 이름으로 변경한다")
		void renamesExistingFieldToNewName () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .rename("emp_name", "name")
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			DataRow firstRow = result.get(0);
			assertThat(firstRow.getString("name")).isEqualTo("John Doe");
			assertThat(firstRow.has("emp_name")).isFalse(); // 기존 필드는 제거되어야 함
			assertThat(firstRow.getInt("salary")).isEqualTo(50000);
		}

		@Test
		@DisplayName("존재하지 않는 필드를 변경하려고 하면 무시된다")
		void ignoresNonExistentField () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .rename("non_existent_field", "new_name")
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			// 원본 데이터가 그대로 유지되어야 함
			DataRow firstRow = result.get(0);
			assertThat(firstRow.getString("emp_name")).isEqualTo("John Doe");
			assertThat(firstRow.has("new_name")).isFalse();
		}
	}

	@Nested
	@DisplayName("add() 연산은")
	class AddOperationTest {

		@Test
		@DisplayName("새로운 필드를 고정값으로 추가한다")
		void addsNewFieldWithFixedValue () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .add("bonus", 1000)
										   .add("status", "active")
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			// 모든 행에 새 필드가 추가되었는지 확인
			for (DataRow row : result) {
				assertThat(row.getInt("bonus")).isEqualTo(1000);
				assertThat(row.getString("status")).isEqualTo("active");
			}
		}

		@Test
		@DisplayName("기존 필드가 있으면 덮어쓴다")
		void overwritesExistingField () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .add("salary", 99999)
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			// 모든 행의 급여가 덮어써졌는지 확인
			for (DataRow row : result) {
				assertThat(row.getInt("salary")).isEqualTo(99999);
			}
		}
	}

	@Nested
	@DisplayName("remove() 연산은")
	class RemoveOperationTest {

		@Test
		@DisplayName("지정된 필드를 제거한다")
		void removesSpecifiedField () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .remove("department")
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			// 모든 행에서 department 필드가 제거되었는지 확인
			for (DataRow row : result) {
				assertThat(row.has("department")).isFalse();
				assertThat(row.has("emp_name")).isTrue(); // 다른 필드는 유지
			}
		}

		@Test
		@DisplayName("존재하지 않는 필드를 제거하려고 하면 무시된다")
		void ignoresNonExistentFieldRemoval () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .remove("non_existent_field")
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			// 원본 데이터가 그대로 유지되어야 함
			DataRow firstRow = result.get(0);
			assertThat(firstRow.getString("emp_name")).isEqualTo("John Doe");
			assertThat(firstRow.getInt("salary")).isEqualTo(50000);
		}
	}

	@Nested
	@DisplayName("when() 조건부 연산은")
	class ConditionalOperationTest {

		@Test
		@DisplayName("조건을 만족하는 행에만 변환을 적용한다")
		void appliesTransformationOnlyToMatchingRows () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .when(row -> row.getInt("age") > 50,
												   senior -> {
													   senior.set("category", "senior");
													   return senior;
												   })
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			// 조건에 따른 변환 확인
			assertThat(result.get(0).getString("category")).isNull(); // age 30 -> 조건 불만족
			assertThat(result.get(1).getString("category")).isEqualTo("senior"); // age 55 -> 조건 만족
			assertThat(result.get(2).getString("category")).isNull(); // age 25 -> 조건 불만족
		}

		@Test
		@DisplayName("여러 조건부 변환을 체이닝할 수 있다")
		void canChainMultipleConditionalTransformations () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .when(row -> row.getInt("age") > 50,
												   senior -> {
													   senior.set("category", "senior");
													   return senior;
												   })
										   .when(row -> row.getInt("salary") > 55000,
												   highEarner -> {
													   highEarner.set("tax_bracket", "high");
													   return highEarner;
												   })
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			DataRow jane = result.get(1); // Jane: age=55, salary=60000
			assertThat(jane.getString("category")).isEqualTo("senior");
			assertThat(jane.getString("tax_bracket")).isEqualTo("high");
		}
	}

	@Nested
	@DisplayName("복합 Transform 체이닝은")
	class ChainedTransformTest {

		@Test
		@DisplayName("여러 연산을 순서대로 적용한다")
		void appliesMultipleOperationsInOrder () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder()
										   .rename("emp_name", "name")
										   .add("bonus", 1000)
										   .when(row -> row.getInt("age") > 50,
												   senior -> {
													   senior.set("category", "senior");
													   return senior;
												   })
										   .remove("age")
										   .build();

			// when
			List<DataRow> result = testData.transform(transform).toList();

			// then
			assertThat(result).hasSize(3);

			DataRow firstRow = result.get(0);
			assertThat(firstRow.getString("name")).isEqualTo("John Doe");
			assertThat(firstRow.has("emp_name")).isFalse();
			assertThat(firstRow.getInt("bonus")).isEqualTo(1000);
			assertThat(firstRow.has("age")).isFalse();
			assertThat(firstRow.getString("category")).isNull();

			DataRow secondRow = result.get(1);
			assertThat(secondRow.getString("name")).isEqualTo("Jane Smith");
			assertThat(secondRow.getString("category")).isEqualTo("senior");
			assertThat(secondRow.has("age")).isFalse();
		}
	}

	@Nested
	@DisplayName("Transform 메타데이터는")
	class TransformMetadataTest {

		@Test
		@DisplayName("적용된 연산들의 설명을 제공한다")
		void providesDescriptionsOfAppliedOperations () {
			// given
			Transform transform = Transform.builder()
										   .rename("emp_name", "name")
										   .add("bonus", 1000)
										   .remove("department")
										   .build();

			// when
			List<String> descriptions = transform.getOperationDescriptions();

			// then
			assertThat(descriptions).hasSize(3);
			assertThat(descriptions.get(0)).isEqualTo("Rename field 'emp_name' to 'name'");
			assertThat(descriptions.get(1)).isEqualTo("Add field 'bonus' with value '1000'");
			assertThat(descriptions.get(2)).isEqualTo("Remove field 'department'");
		}
	}

	@Nested
	@DisplayName("빈 Transform은")
	class EmptyTransformTest {

		@Test
		@DisplayName("원본 데이터를 그대로 반환한다")
		void returnsOriginalDataUnchanged () {
			// given
			DataSource testData = createEmployeeTestData();
			Transform transform = Transform.builder().build();

			// when
			List<DataRow> result = testData.transform(transform).toList();
			List<DataRow> original = testData.toList();

			// then
			assertThat(result).hasSize(original.size());

			for (int i = 0; i < original.size(); i++) {
				DataRow originalRow = original.get(i);
				DataRow resultRow = result.get(i);

				assertThat(resultRow.getString("emp_name")).isEqualTo(originalRow.getString("emp_name"));
				assertThat(resultRow.getInt("salary")).isEqualTo(originalRow.getInt("salary"));
				assertThat(resultRow.getInt("age")).isEqualTo(originalRow.getInt("age"));
			}
		}
	}

	@Nested
	@DisplayName("Transform 빌더 검증은")
	class TransformBuilderValidationTest {

		@Test
		@DisplayName("null 필드명으로 rename 시 예외를 던진다")
		void throwsExceptionForNullFieldNameInRename () {
			// when & then
			assertThatThrownBy(() -> Transform.builder().rename(null, "newName"))
					.isInstanceOf(TransformException.class)
					.hasMessageContaining("Old field name cannot be null or empty");
		}

		@Test
		@DisplayName("빈 필드명으로 add 시 예외를 던진다")
		void throwsExceptionForEmptyFieldNameInAdd () {
			// when & then
			assertThatThrownBy(() -> Transform.builder().add("", "value"))
					.isInstanceOf(TransformException.class)
					.hasMessageContaining("Field name cannot be null or empty");
		}

		@Test
		@DisplayName("null 조건으로 when 시 예외를 던진다")
		void throwsExceptionForNullConditionInWhen () {
			// when & then
			assertThatThrownBy(() -> Transform.builder().when(null, row -> row))
					.isInstanceOf(TransformException.class)
					.hasMessageContaining("Condition cannot be null");
		}
	}

	// === Test Helper Methods ===

	/**
	 * 테스트용 직원 데이터를 생성합니다.
	 */
	private DataSource createEmployeeTestData () {
		List<DataRow> rows = new ArrayList<>();

		DataRow row1 = new DataRow();
		row1.set("emp_name", "John Doe");
		row1.set("salary", 50000);
		row1.set("age", 30);
		row1.set("department", "IT");
		rows.add(row1);

		DataRow row2 = new DataRow();
		row2.set("emp_name", "Jane Smith");
		row2.set("salary", 60000);
		row2.set("age", 55);
		row2.set("department", "HR");
		rows.add(row2);

		DataRow row3 = new DataRow();
		row3.set("emp_name", "Bob Johnson");
		row3.set("salary", 45000);
		row3.set("age", 25);
		row3.set("department", "Finance");
		rows.add(row3);

		return new ListDataSource(rows);
	}
}
