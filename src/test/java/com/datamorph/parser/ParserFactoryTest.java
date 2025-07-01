package com.datamorph.parser;

import com.datamorph.core.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ParserFactory 테스트")
class ParserFactoryTest {

	@Nested
	@DisplayName("포맷 기반 Parser 생성")
	class FormatBasedCreation {

		@Test
		@DisplayName("CSV 포맷으로 CsvParser를 생성한다")
		void createCsvParserWithFormat () {
			// When
			Parser parser = ParserFactory.createParser(Format.CSV);

			// Then
			assertThat(parser).isNotNull();
			assertThat(parser).isInstanceOf(CsvParser.class);
		}

		@Test
		@DisplayName("JSON 포맷으로 JsonParser를 생성한다")
		void createJsonParserWithFormat () {
			// When
			Parser parser = ParserFactory.createParser(Format.JSON);

			// Then
			assertThat(parser).isNotNull();
			assertThat(parser).isInstanceOf(JsonParser.class);
		}

		@Test
		@DisplayName("매번 새로운 인스턴스를 생성한다")
		void createNewInstanceEachTime () {
			// When
			Parser parser1 = ParserFactory.createParser(Format.CSV);
			Parser parser2 = ParserFactory.createParser(Format.CSV);

			// Then
			assertThat(parser1).isNotNull();
			assertThat(parser2).isNotNull();
			assertThat(parser1).isNotSameAs(parser2);
		}
	}

	@Nested
	@DisplayName("파일 경로 기반 Parser 생성")
	class PathBasedCreation {

		@Test
		@DisplayName("CSV 파일 경로로 CsvParser를 생성한다")
		void createCsvParserFromPath () {
			// When
			Parser parser = ParserFactory.createParserFromPath("data.csv");

			// Then
			assertThat(parser).isNotNull();
			assertThat(parser).isInstanceOf(CsvParser.class);
		}

		@Test
		@DisplayName("JSON 파일 경로로 JsonParser를 생성한다")
		void createJsonParserFromPath () {
			// When
			Parser parser = ParserFactory.createParserFromPath("data.json");

			// Then
			assertThat(parser).isNotNull();
			assertThat(parser).isInstanceOf(JsonParser.class);
		}

		@Test
		@DisplayName("복잡한 경로에서도 올바른 Parser를 생성한다")
		void createParserFromComplexPath () {
			// When
			Parser csvParser = ParserFactory.createParserFromPath("/path/to/my/data.csv");
			Parser jsonParser = ParserFactory.createParserFromPath("C:\\Users\\Documents\\config.json");

			// Then
			assertThat(csvParser).isInstanceOf(CsvParser.class);
			assertThat(jsonParser).isInstanceOf(JsonParser.class);
		}
	}

	@Nested
	@DisplayName("입력 검증")
	class InputValidation {

		@Test
		@DisplayName("null 포맷에 대해 예외를 던진다")
		void throwExceptionForNullFormat () {
			// When & Then
			assertThatThrownBy(() -> ParserFactory.createParser(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Format cannot be null");
		}

		@Test
		@DisplayName("null 파일 경로에 대해 예외를 던진다")
		void throwExceptionForNullPath () {
			// When & Then
			assertThatThrownBy(() -> ParserFactory.createParserFromPath(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("File path cannot be null or empty");
		}

		@Test
		@DisplayName("빈 파일 경로에 대해 예외를 던진다")
		void throwExceptionForEmptyPath () {
			// When & Then
			assertThatThrownBy(() -> ParserFactory.createParserFromPath(""))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("File path cannot be null or empty");
		}

		@Test
		@DisplayName("지원하지 않는 확장자에 대해 예외를 던진다")
		void throwExceptionForUnsupportedExtension () {
			// When & Then
			assertThatThrownBy(() -> ParserFactory.createParserFromPath("data.xml"))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Unsupported file format");
		}

		@Test
		@DisplayName("확장자가 없는 파일에 대해 예외를 던진다")
		void throwExceptionForNoExtension () {
			// When & Then
			assertThatThrownBy(() -> ParserFactory.createParserFromPath("data"))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("No file extension found in: data");
		}
	}
}
