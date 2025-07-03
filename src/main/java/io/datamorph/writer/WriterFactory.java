package io.datamorph.writer;

import io.datamorph.core.Format;

/**
 * Writer 인스턴스를 생성하는 팩토리 클래스
 * <p>
 * 포맷별로 적절한 Writer 구현체를 생성합니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.02
 */
public final class WriterFactory {
	
	private WriterFactory() {
		throw new UnsupportedOperationException("Utility class");
	}
	
	/**
	 * 지정된 포맷에 맞는 Writer를 생성합니다.
	 *
	 * @param format 출력 포맷
	 * @return 해당 포맷의 Writer 인스턴스
	 * @throws IllegalArgumentException 지원하지 않는 포맷인 경우
	 */
	public static Writer createWriter(Format format) {
		if (format == null) {
			throw new IllegalArgumentException("Format cannot be null");
		}
		
		return switch (format) {
			case CSV -> new CsvWriter();
			case JSON -> new JsonWriter();
		};
	}
	
	/**
	 * 파일 경로의 확장자를 기반으로 Writer를 생성합니다.
	 *
	 * @param filePath 출력 파일 경로
	 * @return 해당 확장자에 맞는 Writer 인스턴스
	 * @throws IllegalArgumentException 파일 경로가 null이거나 지원하지 않는 확장자인 경우
	 */
	public static Writer createWriterFromPath(String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("File path cannot be null or empty");
		}
		
		Format format = Format.fromExtension(filePath);
		return createWriter(format);
	}
	
	/**
	 * 확장자 문자열을 기반으로 Writer를 생성합니다.
	 *
	 * @param extension 파일 확장자 (예: "csv", "json")
	 * @return 해당 확장자에 맞는 Writer 인스턴스
	 * @throws IllegalArgumentException 확장자가 null이거나 지원하지 않는 확장자인 경우
	 */
	public static Writer createWriterFromExtension(String extension) {
		if (extension == null || extension.trim().isEmpty()) {
			throw new IllegalArgumentException("Extension cannot be null or empty");
		}
		
		String normalizedExt = extension.toLowerCase().trim();
		// 점이 포함된 경우 제거
		if (normalizedExt.startsWith(".")) {
			normalizedExt = normalizedExt.substring(1);
		}
		
		return switch (normalizedExt) {
			case "csv" -> new CsvWriter();
			case "json" -> new JsonWriter();
			default -> throw new IllegalArgumentException("Unsupported extension: " + extension);
		};
	}
}
