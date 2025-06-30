package com.datamorph.mapper;

import com.datamorph.core.DataRow;
import com.datamorph.exceptions.ObjectMappingException;
import com.datamorph.fixtures.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * ObjectMapper 클래스의 테스트
 */
class ObjectMapperTest {

	private ObjectMapper mapper;

	@BeforeEach
	void setUp () {
		mapper = new ObjectMapper();
	}

	@Nested
	@DisplayName("toObject() 메서드는")
	class ToObjectTest {

		@Test
		@DisplayName("null DataRow를 받으면 IllegalArgumentException을 던진다")
		void throwsExceptionWhenDataRowIsNull () {
			// when & then
			assertThatThrownBy(() -> mapper.toObject(null, Person.class))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("DataRow cannot be null");
		}

		@Test
		@DisplayName("null 대상 클래스를 받으면 IllegalArgumentException을 던진다")
		void throwsExceptionWhenTargetClassIsNull () {
			// given
			DataRow dataRow = new DataRow();

			// when & then
			assertThatThrownBy(() -> mapper.toObject(dataRow, null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Target class cannot be null");
		}

		@Test
		@DisplayName("기본 타입 필드를 가진 DataRow를 Person 객체로 변환한다")
		void convertsDataRowWithBasicTypesToPerson () throws ObjectMappingException {
			// given
			DataRow dataRow = new DataRow();
			dataRow.set("name", "John Doe");
			dataRow.set("age", 30);
			dataRow.set("salary", 50000.0);
			dataRow.set("isActive", true);

			// when
			Person result = mapper.toObject(dataRow, Person.class);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getName()).isEqualTo("John Doe");
			assertThat(result.getAge()).isEqualTo(30);
			assertThat(result.getSalary()).isEqualTo(50000.0);
			assertThat(result.getIsActive()).isTrue();
		}

		@Test
		@DisplayName("문자열 값을 적절한 타입으로 변환한다")
		void convertsStringValuesToAppropriateTypes () throws ObjectMappingException {
			// given
			DataRow dataRow = new DataRow();
			dataRow.set("name", "Jane Smith");
			dataRow.set("age", "25");
			dataRow.set("salary", "45000.5");
			dataRow.set("isActive", "true");

			// when
			Person result = mapper.toObject(dataRow, Person.class);

			// then
			assertThat(result.getName()).isEqualTo("Jane Smith");
			assertThat(result.getAge()).isEqualTo(25);
			assertThat(result.getSalary()).isEqualTo(45000.5);
			assertThat(result.getIsActive()).isTrue();
		}

		@Test
		@DisplayName("LocalDate 필드를 올바르게 변환한다")
		void convertsLocalDateFieldCorrectly () throws ObjectMappingException {
			// given
			DataRow dataRow = new DataRow();
			dataRow.set("name", "Alice");
			dataRow.set("birthDate", "1990-05-15");

			// when
			Person result = mapper.toObject(dataRow, Person.class);

			// then
			assertThat(result.getName()).isEqualTo("Alice");
			assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
		}

		@Test
		@DisplayName("일부 필드만 있는 DataRow도 처리한다")
		void handlesPartialDataRow () throws ObjectMappingException {
			// given
			DataRow dataRow = new DataRow();
			dataRow.set("name", "Bob");

			// when
			Person result = mapper.toObject(dataRow, Person.class);

			// then
			assertThat(result.getName()).isEqualTo("Bob");
			assertThat(result.getAge()).isNull();
			assertThat(result.getSalary()).isNull();
			assertThat(result.getIsActive()).isNull();
			assertThat(result.getBirthDate()).isNull();
		}

		@Test
		@DisplayName("빈 DataRow를 처리한다")
		void handlesEmptyDataRow () throws ObjectMappingException {
			// given
			DataRow dataRow = new DataRow();

			// when
			Person result = mapper.toObject(dataRow, Person.class);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getName()).isNull();
			assertThat(result.getAge()).isNull();
			assertThat(result.getSalary()).isNull();
			assertThat(result.getIsActive()).isNull();
			assertThat(result.getBirthDate()).isNull();
		}

		@Test
		@DisplayName("잘못된 숫자 형식에 대해 ObjectMappingException을 던진다")
		void throwsExceptionForInvalidNumberFormat () {
			// given
			DataRow dataRow = new DataRow();
			dataRow.set("name", "Charlie");
			dataRow.set("age", "not_a_number");

			// when & then
			assertThatThrownBy(() -> mapper.toObject(dataRow, Person.class))
					.isInstanceOf(ObjectMappingException.class)
					.hasMessage("Failed to convert DataRow to Person")
					.hasCauseInstanceOf(ObjectMappingException.class);
		}

		@Test
		@DisplayName("잘못된 날짜 형식에 대해 ObjectMappingException을 던진다")
		void throwsExceptionForInvalidDateFormat () {
			// given
			DataRow dataRow = new DataRow();
			dataRow.set("name", "David");
			dataRow.set("birthDate", "invalid_date");

			// when & then
			assertThatThrownBy(() -> mapper.toObject(dataRow, Person.class))
					.isInstanceOf(ObjectMappingException.class)
					.hasMessage("Failed to convert DataRow to Person")
					.hasCauseInstanceOf(ObjectMappingException.class);
		}
	}

	@Nested
	@DisplayName("toDataRow() 메서드는")
	class ToDataRowTest {

		@Test
		@DisplayName("null 객체를 받으면 IllegalArgumentException을 던진다")
		void throwsExceptionWhenObjectIsNull () {
			// when & then
			assertThatThrownBy(() -> mapper.toDataRow(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Object cannot be null");
		}

		@Test
		@DisplayName("Person 객체를 DataRow로 변환한다")
		void convertsPersonToDataRow () throws ObjectMappingException {
			// given
			Person person = new Person();
			person.setName("John Doe");
			person.setAge(30);
			person.setSalary(50000.0);
			person.setIsActive(true);
			person.setBirthDate(LocalDate.of(1993, 3, 15));

			// when
			DataRow result = mapper.toDataRow(person);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getString("name")).isEqualTo("John Doe");
			assertThat(result.getInt("age")).isEqualTo(30);
			assertThat(result.getObject("salary")).isEqualTo(50000.0);
			assertThat(result.getBoolean("isActive")).isTrue();
			assertThat(result.getObject("birthDate")).isEqualTo(LocalDate.of(1993, 3, 15));
		}

		@Test
		@DisplayName("null 필드가 있는 객체를 DataRow로 변환한다")
		void convertsObjectWithNullFieldsToDataRow () throws ObjectMappingException {
			// given
			Person person = new Person();
			person.setName("Jane Smith");

			// when
			DataRow result = mapper.toDataRow(person);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getString("name")).isEqualTo("Jane Smith");
			assertThat(result.has("age")).isFalse();
			assertThat(result.has("salary")).isFalse();
			assertThat(result.has("isActive")).isFalse();
			assertThat(result.has("birthDate")).isFalse();
		}

		@Test
		@DisplayName("모든 필드가 null인 객체를 빈 DataRow로 변환한다")
		void convertsObjectWithAllNullFieldsToEmptyDataRow () throws ObjectMappingException {
			// given
			Person person = new Person();

			// when
			DataRow result = mapper.toDataRow(person);

			// then
			assertThat(result).isNotNull();
			assertThat(result.has("name")).isFalse();
			assertThat(result.has("age")).isFalse();
			assertThat(result.has("salary")).isFalse();
			assertThat(result.has("isActive")).isFalse();
			assertThat(result.has("birthDate")).isFalse();
		}
	}

	@Nested
	@DisplayName("양방향 변환 테스트")
	class BidirectionalConversionTest {

		@Test
		@DisplayName("Person -> DataRow -> Person 변환이 올바르게 동작한다")
		void roundTripConversionWorksCorrectly () throws ObjectMappingException {
			// given
			Person original = new Person();
			original.setName("Test User");
			original.setAge(25);
			original.setSalary(40000.0);
			original.setIsActive(true);
			original.setBirthDate(LocalDate.of(1998, 12, 25));

			// when
			DataRow dataRow = mapper.toDataRow(original);
			Person converted = mapper.toObject(dataRow, Person.class);

			// then
			assertThat(converted).isEqualTo(original);
		}

		@Test
		@DisplayName("DataRow -> Person -> DataRow 변환이 올바르게 동작한다")
		void dataRowToPersonToDataRowWorksCorrectly () throws ObjectMappingException {
			// given
			DataRow original = new DataRow();
			original.set("name", "Original User");
			original.set("age", 35);
			original.set("salary", 60000.0);
			original.set("isActive", false);
			original.set("birthDate", "1988-07-10");

			// when
			Person person = mapper.toObject(original, Person.class);
			DataRow converted = mapper.toDataRow(person);

			// then
			assertThat(converted.getString("name")).isEqualTo(original.getString("name"));
			assertThat(converted.getInt("age")).isEqualTo(original.getInt("age"));
			assertThat(converted.getObject("salary")).isEqualTo(original.getObject("salary"));
			assertThat(converted.getBoolean("isActive")).isEqualTo(original.getBoolean("isActive"));
			assertThat(converted.getObject("birthDate")).isEqualTo(LocalDate.of(1988, 7, 10));
		}
	}
}
