package io.datamorph.parser;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Parser 인터페이스의 공통 계약을 테스트하는 추상 클래스
 * 모든 Parser 구현체는 이 테스트를 통과해야 합니다.
 */
public abstract class ParserContractTest {

	/**
	 * 테스트할 Parser 인스턴스를 생성합니다.
	 */
	protected abstract Parser createParser ();

	/**
	 * 기본 데이터를 해당 포맷으로 생성합니다.
	 * 데이터: name=John, age=30, city=Seoul
	 */
	protected abstract String createBasicData ();

	/**
	 * 여러 행의 데이터를 해당 포맷으로 생성합니다.
	 * 데이터:
	 * - name=John, age=30, city=Seoul
	 * - name=Jane, age=25, city=Busan
	 */
	protected abstract String createMultiRowData ();

	/**
	 * 빈 필드가 포함된 데이터를 생성합니다.
	 * 데이터: name=John, age=null, city=Seoul
	 */
	protected abstract String createDataWithEmptyFields ();

	/**
	 * 빈 데이터를 생성합니다. (데이터 없음, 헤더만 있거나 완전히 비어있음)
	 */
	protected abstract String createEmptyData ();

	@Nested
	@DisplayName("공통 계약: 기본 파싱")
	class BasicParsingContract {

		@Test
		@DisplayName("기본 데이터를 파싱할 수 있다")
		void shouldParseBasicData () {
			// given
			Parser parser = createParser();
			String data = createBasicData();

			// when
			List<DataRow> result = parser.parse(data);

			// then
			assertThat(result).hasSize(1);
			DataRow row = result.get(0);
			assertThat(row.getString("name")).isEqualTo("John");
			assertThat(row.getInt("age")).isEqualTo(30);
			assertThat(row.getString("city")).isEqualTo("Seoul");
		}

		@Test
		@DisplayName("여러 행의 데이터를 파싱할 수 있다")
		void shouldParseMultipleRows () {
			// given
			Parser parser = createParser();
			String data = createMultiRowData();

			// when
			List<DataRow> result = parser.parse(data);

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

		@Test
		@DisplayName("빈 데이터를 파싱하면 빈 리스트를 반환한다")
		void shouldReturnEmptyListForEmptyData () {
			// given
			Parser parser = createParser();
			String data = createEmptyData();

			// when
			List<DataRow> result = parser.parse(data);

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("공통 계약: null 및 빈 필드 처리")
	class NullAndEmptyHandlingContract {

		@Test
		@DisplayName("null 입력 시 IllegalArgumentException을 던진다")
		void shouldThrowExceptionForNullInput () {
			// given
			Parser parser = createParser();

			// when & then
			assertThatThrownBy(() -> parser.parse((String) null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("null");
		}

		@Test
		@DisplayName("빈 필드를 null로 처리한다")
		void shouldHandleEmptyFieldsAsNull () {
			// given
			Parser parser = createParser();
			String data = createDataWithEmptyFields();

			// when
			List<DataRow> result = parser.parse(data);

			// then
			assertThat(result).hasSize(1);
			DataRow row = result.get(0);
			assertThat(row.getString("name")).isEqualTo("John");
			assertThat(row.getString("age")).isNull();
			assertThat(row.getString("city")).isEqualTo("Seoul");
		}
	}

	@Nested
	@DisplayName("공통 계약: 입력 스트림 파싱")
	class InputStreamParsingContract {

		@Test
		@DisplayName("InputStream으로부터 데이터를 파싱할 수 있다")
		void shouldParseFromInputStream () throws IOException {
			// given
			Parser parser = createParser();
			String data = createBasicData();
			InputStream input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

			// when
			List<DataRow> result = parser.parse(input);

			// then
			assertThat(result).hasSize(1);
			DataRow row = result.get(0);
			assertThat(row.getString("name")).isEqualTo("John");
			assertThat(row.getInt("age")).isEqualTo(30);
			assertThat(row.getString("city")).isEqualTo("Seoul");
		}

		@Test
		@DisplayName("null InputStream 시 IllegalArgumentException을 던진다")
		void shouldThrowExceptionForNullInputStream () {
			// given
			Parser parser = createParser();

			// when & then
			assertThatThrownBy(() -> parser.parse((InputStream) null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("null");
		}

		@Test
		@DisplayName("빈 InputStream을 파싱하면 빈 리스트를 반환한다")
		void shouldReturnEmptyListForEmptyInputStream () throws IOException {
			// given
			Parser parser = createParser();
			InputStream input = new ByteArrayInputStream(new byte[0]);

			// when
			List<DataRow> result = parser.parse(input);

			// then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("공통 계약: 데이터 타입 변환")
	class DataTypeConversionContract {

		@Test
		@DisplayName("기본 데이터 타입을 올바르게 처리한다")
		void shouldHandleBasicDataTypes () {
			// given
			Parser parser = createParser();
			String data = createBasicData();

			// when
			List<DataRow> result = parser.parse(data);

			// then
			DataRow row = result.get(0);

			// String 타입
			assertThat(row.getString("name")).isEqualTo("John");
			assertThat(row.getString("city")).isEqualTo("Seoul");

			// Integer 타입
			assertThat(row.getInt("age")).isEqualTo(30);

			// null 처리
			assertThat(row.getString("nonexistent")).isNull();
		}
	}

	@Nested
	@DisplayName("공통 계약: 에러 처리")
	class ErrorHandlingContract {

		@Test
		@DisplayName("잘못된 형식의 데이터 파싱 시 ParseException을 던진다")
		void shouldThrowParseExceptionForInvalidFormat () {
			// given
			Parser parser = createParser();
			String invalidData = "This is not a valid format for any parser!@#$%^&*()";

			// when & then
			assertThatThrownBy(() -> parser.parse(invalidData))
					.isInstanceOf(ParseException.class);
		}
	}
}