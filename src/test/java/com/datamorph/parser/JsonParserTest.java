package com.datamorph.parser;

import com.datamorph.exceptions.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * JsonParser의 테스트 클래스
 * Parser 인터페이스의 공통 계약과 JSON 특화 기능을 모두 테스트합니다.
 */
class JsonParserTest extends ParserContractTest {

	@Override
	protected Parser createParser () {
		return new JsonParser();
	}

	@Override
	protected String createBasicData () {
		return """
				[
				    {"name": "John", "age": 30, "city": "Seoul"}
				]
				""".trim();
	}

	@Override
	protected String createMultiRowData () {
		return """
				[
				    {"name": "John", "age": 30, "city": "Seoul"},
				    {"name": "Jane", "age": 25, "city": "Busan"}
				]
				""".trim();
	}

	@Override
	protected String createDataWithEmptyFields () {
		return """
				[
				    {"name": "John", "age": null, "city": "Seoul"}
				]
				""".trim();
	}

	@Override
	protected String createEmptyData () {
		return "[]";
	}

	// ===== JSON 특화 테스트 =====

	@Nested
	@DisplayName("JSON 특화: 중첩 객체 처리")
	class NestedObjectHandling {

		@Test
		@DisplayName("중첩된 객체를 평면화하여 처리한다")
		void handleNestedObjects () {
			// given
			String json = """
					[
					    {
					        "name": "John",
					        "address": {
					            "city": "Seoul",
					            "country": "Korea"
					        }
					    }
					]
					""".trim();

			// when
			var result = createParser().parse(json);

			// then
			assertThat(result).hasSize(1);
			var row = result.get(0);
			assertThat(row.getString("name")).isEqualTo("John");
			assertThat(row.getString("address.city")).isEqualTo("Seoul");
			assertThat(row.getString("address.country")).isEqualTo("Korea");
		}
	}

	@Nested
	@DisplayName("JSON 특화: 배열 처리")
	class ArrayHandling {

		@Test
		@DisplayName("배열 필드를 문자열로 변환한다")
		void handleArrayFields () {
			// given
			String json = """
					[
					    {
					        "name": "John",
					        "skills": ["Java", "Python", "JavaScript"]
					    }
					]
					""".trim();

			// when
			var result = createParser().parse(json);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getString("skills"))
					.isEqualTo("[Java, Python, JavaScript]");
		}
	}

	@Nested
	@DisplayName("JSON 특화: 다양한 데이터 타입")
	class DataTypeHandling {

		@Test
		@DisplayName("JSON의 다양한 데이터 타입을 올바르게 처리한다")
		void handleVariousDataTypes () {
			// given
			String json = """
					[
					    {
					        "string": "text",
					        "integer": 42,
					        "float": 3.14,
					        "boolean": true,
					        "null": null
					    }
					]
					""".trim();

			// when
			var result = createParser().parse(json);

			// then
			assertThat(result).hasSize(1);
			var row = result.get(0);
			assertThat(row.getString("string")).isEqualTo("text");
			assertThat(row.getInt("integer")).isEqualTo(42);
			assertThat(row.getString("float")).isEqualTo("3.14");
			assertThat(row.getBoolean("boolean")).isTrue();
			assertThat(row.getString("null")).isNull();
		}
	}

	@Nested
	@DisplayName("JSON 특화: 형식 오류 처리")
	class MalformedJsonHandling {

		@ParameterizedTest
		@ValueSource(strings = {
				"{",                    // 불완전한 JSON
				"[{\"name\": }]",      // 값 누락
				"[{name: \"John\"}]",  // 따옴표 없는 키
				"{'name': 'John'}",    // 작은따옴표 사용
		})
		@DisplayName("잘못된 JSON 형식에 대해 ParseException을 던진다")
		void throwExceptionForMalformedJson (String json) {
			// when & then
			assertThatThrownBy(() -> createParser().parse(json))
					.isInstanceOf(ParseException.class)
					.hasMessageContaining("Invalid JSON");
		}
	}
}