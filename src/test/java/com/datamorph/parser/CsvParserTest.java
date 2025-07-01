package com.datamorph.parser;

import com.datamorph.core.DataRow;
import com.datamorph.exceptions.ParseException;
import com.datamorph.fixtures.CsvFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class CsvParserTest extends ParserContractTest {

	@Override
	protected Parser createParser () {
		return new CsvParser();
	}

	@Override
	protected String createBasicData () {
		return CsvFixtures.basicCsv();
	}

	@Override
	protected String createMultiRowData () {
		return CsvFixtures.multiRowCsv();
	}

	@Override
	protected String createDataWithEmptyFields () {
		return CsvFixtures.csvWithEmptyFields();
	}

	@Override
	protected String createEmptyData () {
		return CsvFixtures.emptyCsv();
	}

	@Nested
	@DisplayName("CSV 특화: 구분자 처리")
	class DelimiterHandling {
		@ParameterizedTest
		@MethodSource("delimiterTestCases")
		@DisplayName("다양한 구분자를 처리할 수 있다")
		void handleDifferentDelimiters (String csv, char delimiter, String expectedName) {
			// given
			CsvParser parser = ((CsvParser) createParser()).withDelimiter(delimiter);

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getString("name")).isEqualTo(expectedName);
		}

		static Stream<Arguments> delimiterTestCases () {
			String expectedName = "John";
			return Stream.of(
					Arguments.arguments("name|age|city\nJohn|30|Seoul", '|', expectedName),
					Arguments.arguments("name\tage\tcity\nJohn\t30\tSeoul", '\t', expectedName),
					Arguments.arguments("name;age;city\nJohn;30;Seoul", ';', expectedName)
			);
		}
	}

	@Nested
	@DisplayName("기본 CSV 파싱")
	class BasicParsing {
		final CsvParser parser = (CsvParser) createParser();

		@Test
		@DisplayName("헤더와 데이터가 있는 기본 CSV를 파싱한다")
		void parseBasicCsv () {
			// when
			List<DataRow> result = parser.parse(CsvFixtures.basicCsvWithTwoRows());

			// then
			assertThat(result).hasSize(2);

			DataRow firstRow = result.get(0);
			assertThat(firstRow.getString("name")).isEqualTo("John");
			assertThat(firstRow.getInt("age")).isEqualTo(30);
			assertThat(firstRow.getString("city")).isEqualTo("Seoul");

			DataRow secondRow = result.get(1);
			assertThat(secondRow.getString("name")).isEqualTo("Jane");
			assertThat(secondRow.getInt("age")).isEqualTo(25);
			assertThat(secondRow.getString("city")).isEqualTo("Busan");
		}

		@ParameterizedTest
		@ValueSource(strings = {"name,age,city", "name", "a,b,c,d,e,f"})
		@DisplayName("헤더만 있는 CSV를 파싱하면 빈 리스트를 반환한다")
		void parseHeaderOnlyCsv (String csv) {
			// when & then
			assertThat(parser.parse(csv)).isEmpty();
		}

		@ParameterizedTest
		@ValueSource(strings = {"", "   ", "\n", "\r\n"})
		@DisplayName("빈 문자열이나 공백만 있으면 빈 List<Data>를 반환")
		void parseEmptyOrBlankString (String csv) {
			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("빈 필드 처리")
	class EmptyFieldHandling {
		final CsvParser parser = (CsvParser) createParser();

		@ParameterizedTest
		@MethodSource("emptyFieldTestCases")
		@DisplayName("다양한 빈 필드 패턴을 올바르게 처리한다")
		void handleEmptyFields (String csv, int expectedRows, String description) {
			// when & then
			List<DataRow> result = parser.parse(csv);
			assertThat(result).as(description).hasSize(expectedRows);
		}

		@ParameterizedTest
		@MethodSource("emptyFieldNullCases")
		@DisplayName("빈 필드를 null로 처리한다")
		void emptyFieldsAsNull (String csv, String field1, String expected1, String field2, String expected2) {
			// when & then
			List<DataRow> result = parser.parse(csv);

			assertThat(result).hasSize(1);
			DataRow row = result.get(0);

			if ("null".equals(expected1)) {
				assertThat(row.getString(field1)).isNull();
			} else {
				assertThat(row.getString(field1)).isEqualTo(expected1);
			}

			if ("null".equals(expected2)) {
				assertThat(row.getString(field2)).isNull();
			} else {
				assertThat(row.getString(field2)).isEqualTo(expected2);
			}
		}

		static Stream<Arguments> emptyFieldTestCases () {
			return Stream.of(
					Arguments.arguments("a,b,c\n1,,3", 1, "중간 필드가 비어있는 경우"),
					Arguments.arguments("a,b,c\n,2,3", 1, "첫 번째 필드가 비어있는 경우"),
					Arguments.arguments("a,b,c\n1,2,", 1, "마지막 필드가 비어있는 경우"),
					Arguments.arguments("a,b,c\n,,", 1, "모든 필드가 비어있는 경우"),
					Arguments.arguments("a,b,c,d\n1,,,4", 1, "연속된 빈 필드가 있는 경우")
			);
		}

		static Stream<Arguments> emptyFieldNullCases () {
			return Stream.of(
					Arguments.arguments("name,age\nJohn,", "name", "John", "age", "null"),
					Arguments.arguments("name,age\n,25", "name", "null", "age", "25"),
					Arguments.arguments("a,b,c\n,,x", "a", "null", "c", "x")
			);
		}
	}

	@Nested
	@DisplayName("따옴표 처리")
	class QuoteHandling {
		final CsvParser parser = (CsvParser) createParser();

		@ParameterizedTest
		@MethodSource("quotedFieldTestCases")
		@DisplayName("다양한 따옴표 패턴을 올바르게 처리한다")
		void parseQuotedFields (String csv, String expected, String description) {
			// when & then
			List<DataRow> result = parser.parse(csv);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getString("value")).as(description).isEqualTo(expected);
		}

		@Test
		@DisplayName("복잡한 따옴표 시나리오를 처리한다")
		void complexQuotedScenario () {
			// given
			String csv = CsvFixtures.complexQuotedCsv();

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			DataRow row = result.get(0);
			assertThat(row.getString("name")).isEqualTo("John \"JD\" Doe");
			assertThat(row.getString("description")).isEqualTo("Software Engineer\nWorks at \"Big Tech\"");
			assertThat(row.getString("notes")).isEqualTo("Uses Java, Python, and \"other\" languages");
		}

		static Stream<Arguments> quotedFieldTestCases () {
			return Stream.of(
					Arguments.arguments("value\n\"simple\"", "simple", "기본 따옴표"),
					Arguments.arguments("value\n\"with,comma\"", "with,comma", "따옴표 안의 콤마"),
					Arguments.arguments("value\n\"with\nnewline\"", "with\nnewline", "따옴표 안의 줄바꿈"),
					Arguments.arguments("value\n\"with \"\"quotes\"\"\"", "with \"quotes\"", "이스케이프된 따옴표"),
					Arguments.arguments("value\n\" spaces \"", " spaces ", "따옴표 안의 공백 유지"),
					Arguments.arguments("value\n\"\"", "", "빈 따옴표"),
					Arguments.arguments("value\n\"a\"\"b\"\"c\"", "a\"b\"c", "여러 개의 이스케이프된 따옴표")
			);
		}
	}

	@Nested
	@DisplayName("공백 처리")
	class WhitespaceHandling {
		final CsvParser parser = (CsvParser) createParser();


		@ParameterizedTest
		@MethodSource("whitespaceTrimCases")
		@DisplayName("필드 앞뒤 공백을 제거한다")
		void trimFieldWhitespace (String input, String expected) {
			// given
			String csv = "name\n" + input;

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getString("name")).isEqualTo(expected);
		}

		@ParameterizedTest
		@MethodSource("whitespaceInQuotesTestCases")
		@DisplayName("따옴표 안의 공백은 유지한다")
		void preserveWhitespaceInQuotes (String fieldValue, String expected) {
			// given
			String csv = "text\n" + fieldValue;

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getString("text")).isEqualTo(expected);
		}

		public static Stream<Arguments> whitespaceInQuotesTestCases () {
			return Stream.of(
					Arguments.arguments("\" spaces \"", " spaces "),
					Arguments.arguments("\"  leading\"", "  leading"),
					Arguments.arguments("\"trailing  \"", "trailing  "),
					Arguments.arguments("\"\t\ttabs\t\t\"", "\t\ttabs\t\t"),
					Arguments.arguments("\" \"", " ")
			);
		}

		static Stream<Arguments> whitespaceTrimCases () {
			return Stream.of(
					Arguments.arguments(" John ", "John"),
					Arguments.arguments("  Jane  ", "Jane"),
					Arguments.arguments("\tBob\t", "Bob"),
					Arguments.arguments(" \t Mixed \t ", "Mixed")
			);
		}
	}

	@Nested
	@DisplayName("데이터 타입 변환")
	class DataTypeConversion {
		final CsvParser parser = (CsvParser) createParser();

		@ParameterizedTest
		@CsvSource({
				"123, 123",
				"'123', 123",
				"' 123 ', 123",
				"'+123', 123",
				"'-123', -123"
		})
		@DisplayName("정수 값을 올바르게 파싱한다")
		void parseIntegerValues (String input, int expected) {
			// given
			String csv = "number\n" + input;

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getInt("number")).isEqualTo(expected);
		}

		@ParameterizedTest
		@ValueSource(strings = {"true", "TRUE", "True", "yes", "YES", "y", "Y"})
		@DisplayName("다양한 true 값을 올바르게 파싱한다")
		void parseTrueValues (String input) {
			// given
			String csv = "flag\n" + input;

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getBoolean("flag")).isTrue();
		}

		@ParameterizedTest
		@ValueSource(strings = {"false", "FALSE", "False", "no", "NO", "n", "N"})
		@DisplayName("다양한 false 값을 올바르게 파싱한다")
		void parseFalseValues (String input) {
			// given
			String csv = "flag\n" + input;

			// when
			List<DataRow> result = parser.parse(csv);

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getBoolean("flag")).isFalse();
		}
	}

	@Nested
	@DisplayName("에러 처리")
	class ErrorHandling {
		final CsvParser parser = (CsvParser) createParser();

		@Test
		@DisplayName("null 입력 시 IllegalArgumentException을 던진다")
		void throwExceptionOnNull () {
			// given
			String csv = null;

			// when & then
			assertThatThrownBy(() -> parser.parse(csv))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("CSV content cannot be null");
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"a,b,c\n1,2",
				"a,b,c\n1,2,3,4"
		})
		@DisplayName("컬럼 수가 맞지 않으면 ParseException을 던진다")
		void throwExceptionOnColumnMismatch (String csv) {
			// when & then
			assertThatThrownBy(() -> parser.parse(csv))
					.isInstanceOf(ParseException.class)
					.hasMessageContaining("column");
		}
	}
}