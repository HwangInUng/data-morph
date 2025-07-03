package io.datamorph.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * DataSource 인터페이스의 구현체들이 지켜야 할 계약을 테스트합니다.
 */
class DataSourceTest {

	@Nested
	@DisplayName("toList() 메서드는")
	class ToListTest {

		@Test
		@DisplayName("데이터가 있을 때 DataRow 리스트를 반환한다")
		void returnsListOfDataRowsWhenDataExists () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource.toList();

			// then
			assertThat(result).isNotNull();
			assertThat(result).hasSize(3);
			assertThat(result.get(0).getString("name")).isEqualTo("John");
			assertThat(result.get(0).getInt("age")).isEqualTo(30);
			assertThat(result.get(1).getString("name")).isEqualTo("Jane");
			assertThat(result.get(1).getInt("age")).isEqualTo(25);
		}

		@Test
		@DisplayName("데이터가 없을 때 빈 리스트를 반환한다")
		void returnsEmptyListWhenNoData () {
			// given
			DataSource dataSource = createEmptyDataSource();

			// when
			List<DataRow> result = dataSource.toList();

			// then
			assertThat(result).isNotNull();
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("filter() 메서드는")
	class FilterTest {

		@Test
		@DisplayName("조건에 맞는 행만 필터링한다")
		void filtersRowsMatchingPredicate () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.filter(row -> row.getInt("age") > 25)
					.toList();

			// then
			assertThat(result).hasSize(2);
			assertThat(result.get(0).getString("name")).isEqualTo("John");
			assertThat(result.get(1).getString("name")).isEqualTo("Bob");
		}

		@Test
		@DisplayName("여러 필터를 연속으로 적용할 수 있다")
		void canChainMultipleFilters () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.filter(row -> row.getInt("age") > 20)
					.filter(row -> row.getString("city").equals("Seoul"))
					.toList();

			// then
			assertThat(result).hasSize(2);
			assertThat(result).allMatch(row -> row.getString("city").equals("Seoul"));
		}

		@Test
		@DisplayName("모든 행이 필터링되면 빈 리스트를 반환한다")
		void returnsEmptyListWhenAllRowsFiltered () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.filter(row -> row.getInt("age") > 100)
					.toList();

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("transform() 메서드는")
	class TransformTest {

		@Test
		@DisplayName("각 행의 데이터를 변환한다")
		void transformsEachRow () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.transform(row -> row.set("name", row.getString("name").toUpperCase()))
					.toList();

			// then
			assertThat(result).hasSize(3);
			assertThat(result.get(0).getString("name")).isEqualTo("JOHN");
			assertThat(result.get(1).getString("name")).isEqualTo("JANE");
			assertThat(result.get(2).getString("name")).isEqualTo("BOB");
		}

		@Test
		@DisplayName("새로운 필드를 추가할 수 있다")
		void canAddNewFields () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.transform(row -> row.set("isAdult", row.getInt("age") >= 18))
					.toList();

			// then
			assertThat(result).allMatch(row -> row.has("isAdult"));
			assertThat(result.get(0).getBoolean("isAdult")).isTrue();
		}

		@Test
		@DisplayName("여러 변환을 연속으로 적용할 수 있다")
		void canChainMultipleTransforms () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.transform(row -> row.set("age", row.getInt("age") + 1))
					.transform(row -> row.set("ageGroup", row.getInt("age") >= 30 ? "Senior" : "Junior"))
					.toList();

			// then
			assertThat(result.get(0).getInt("age")).isEqualTo(31);
			assertThat(result.get(0).getString("ageGroup")).isEqualTo("Senior");
			assertThat(result.get(1).getInt("age")).isEqualTo(26);
			assertThat(result.get(1).getString("ageGroup")).isEqualTo("Junior");
		}
	}

	@Nested
	@DisplayName("filter()와 transform()을 함께 사용할 때")
	class FilterAndTransformTest {

		@Test
		@DisplayName("필터 후 변환을 적용할 수 있다")
		void canFilterThenTransform () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.filter(row -> row.getString("city").equals("Seoul"))
					.transform(row -> row.set("city", "서울"))
					.toList();

			// then
			assertThat(result).hasSize(2);
			assertThat(result).allMatch(row -> row.getString("city").equals("서울"));
		}

		@Test
		@DisplayName("변환 후 필터를 적용할 수 있다")
		void canTransformThenFilter () {
			// given
			DataSource dataSource = createTestDataSource();

			// when
			List<DataRow> result = dataSource
					.transform(row -> row.set("ageNextYear", row.getInt("age") + 1))
					.filter(row -> row.getInt("ageNextYear") > 30)
					.toList();

			// then
			assertThat(result).hasSize(2);
			assertThat(result.get(0).getString("name")).isEqualTo("John");
			assertThat(result.get(1).getString("name")).isEqualTo("Bob");
		}
	}

	// === Test Helper Methods ===

	/**
	 * 테스트용 DataSource를 생성합니다.
	 * 실제 구현체가 없으므로 일단 컴파일 에러가 발생합니다.
	 */
	private DataSource createTestDataSource () {
		// TODO: 실제 구현체가 필요합니다.
		// 다음과 같은 데이터를 가진 DataSource:
		// name,age,city
		// John,30,Seoul
		// Jane,25,Busan
		// Bob,35,Seoul

		List<DataRow> rows = Arrays.asList(
				createDataRow("John", 30, "Seoul"),
				createDataRow("Jane", 25, "Busan"),
				createDataRow("Bob", 35, "Seoul")
		);

		return new ListDataSource(rows);
	}

	private DataSource createEmptyDataSource () {
		return new ListDataSource(Arrays.asList());
	}

	private DataRow createDataRow (String name, int age, String city) {
		DataRow row = new DataRow();
		row.set("name", name);
		row.set("age", age);
		row.set("city", city);
		return row;
	}
}
