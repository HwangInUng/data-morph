package com.datamorph.util;

import com.datamorph.core.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FormatDetector 테스트")
class FormatDetectorTest {

	@Nested
	@DisplayName("확장자 기반 포맷 감지")
	class ExtensionBasedDetection {

		@ParameterizedTest
		@ValueSource(strings = {
				"data.csv",
				"users.CSV",
				"report.Csv",
				"/path/to/file.csv",
				"C:\\Users\\data.csv",
				"file.name.with.dots.csv"
		})
		@DisplayName("CSV 파일 확장자를 올바르게 감지한다")
		void detectCsvFormat (String filePath) {
			// when
			Format format = FormatDetector.detectFromExtension(filePath);

			// then
			assertThat(format).isEqualTo(Format.CSV);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"data.json",
				"config.JSON",
				"api_response.Json",
				"/api/data.json",
				"C:\\config\\settings.json",
				"complex.file.name.json"
		})
		@DisplayName("JSON 파일 확장자를 올바르게 감지한다")
		void detectJsonFormat (String filePath) {
			// when
			Format format = FormatDetector.detectFromExtension(filePath);

			// then
			assertThat(format).isEqualTo(Format.JSON);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"data.txt",
				"readme.md",
				"config.xml",
				"script.js",
				"style.css",
				"document.pdf",
				"image.png"
		})
		@DisplayName("지원하지 않는 확장자에 대해 예외를 던진다")
		void throwExceptionForUnsupportedExtension (String filePath) {
			// when & then
			assertThatThrownBy(() -> FormatDetector.detectFromExtension(filePath))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Unsupported file format");
		}

		@Test
		@DisplayName("확장자가 없는 파일에 대해 예외를 던진다")
		void throwExceptionForNoExtension () {
			// given
			String filePath = "filename_without_extension";

			// when & then
			assertThatThrownBy(() -> FormatDetector.detectFromExtension(filePath))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("No file extension found");
		}

		@Test
		@DisplayName("빈 확장자에 대해 예외를 던진다")
		void throwExceptionForEmptyExtension () {
			// given
			String filePath = "filename.";

			// when & then
			assertThatThrownBy(() -> FormatDetector.detectFromExtension(filePath))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("No file extension found");
		}
	}

	@Nested
	@DisplayName("입력 검증")
	class InputValidation {

		@Test
		@DisplayName("null 파일 경로에 대해 예외를 던진다")
		void throwExceptionForNullPath () {
			// when & then
			assertThatThrownBy(() -> FormatDetector.detectFromExtension(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("File path cannot be null or empty");
		}

		@ParameterizedTest
		@ValueSource(strings = {"", "   ", "\t", "\n"})
		@DisplayName("빈 문자열이나 공백만 있는 파일 경로에 대해 예외를 던진다")
		void throwExceptionForEmptyPath (String filePath) {
			// when & then
			assertThatThrownBy(() -> FormatDetector.detectFromExtension(filePath))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("File path cannot be null or empty");
		}
	}

	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCases {

		@Test
		@DisplayName("점으로만 이루어진 파일명을 처리한다")
		void handleDotOnlyFilename () {
			// given
			String filePath = "...csv";

			// when
			Format format = FormatDetector.detectFromExtension(filePath);

			// then
			assertThat(format).isEqualTo(Format.CSV);
		}

		@Test
		@DisplayName("숨김 파일의 확장자를 올바르게 감지한다")
		void detectHiddenFileExtension () {
			// given
			String filePath = ".hidden.json";

			// when
			Format format = FormatDetector.detectFromExtension(filePath);

			// then
			assertThat(format).isEqualTo(Format.JSON);
		}

		@Test
		@DisplayName("복잡한 경로의 확장자를 올바르게 감지한다")
		void detectExtensionInComplexPath () {
			// given
			String filePath = "/very/long/path/with.dots/in.directory.names/final.file.csv";

			// when
			Format format = FormatDetector.detectFromExtension(filePath);

			// then
			assertThat(format).isEqualTo(Format.CSV);
		}
	}
}
