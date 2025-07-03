package io.datamorph.parser;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.ParseException;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

/**
 * 데이터 파싱을 담당하는 인터페이스
 * <p>
 * 전통적인 일괄 파싱과 스트리밍 파싱을 모두 지원합니다.
 * </p>
 *
 * @version 1.1.0
 * @since 2025.06.25
 */
public interface Parser {
	
	/**
	 * 문자열 컨텐츠를 파싱하여 DataRow 리스트로 반환합니다.
	 *
	 * @param content 파싱할 문자열 데이터
	 * @return 파싱된 DataRow 리스트
	 * @throws ParseException 파싱 중 오류가 발생한 경우
	 */
	List<DataRow> parse(String content) throws ParseException;
	
	/**
	 * InputStream을 파싱하여 DataRow 리스트로 반환합니다.
	 *
	 * @param input 파싱할 InputStream
	 * @return 파싱된 DataRow 리스트
	 * @throws ParseException 파싱 중 오류가 발생한 경우
	 */
	List<DataRow> parse(InputStream input) throws ParseException;
	
	/**
	 * InputStream을 스트리밍 방식으로 파싱하여 DataRow Stream을 반환합니다.
	 * <p>
	 * 대용량 데이터 처리를 위한 메모리 효율적인 파싱 방법입니다.
	 * 기본 구현은 일괄 파싱 후 스트림으로 변환하므로, 
	 * 실제 스트리밍이 필요한 경우 구현체에서 오버라이드해야 합니다.
	 * </p>
	 *
	 * @param input 파싱할 InputStream
	 * @return 파싱된 DataRow 스트림
	 * @throws ParseException 파싱 중 오류가 발생한 경우
	 * @since 1.1.0
	 */
	default Stream<DataRow> parseAsStream(InputStream input) throws ParseException {
		// 기본 구현: 일괄 파싱 후 스트림으로 변환
		return parse(input).stream();
	}
	
	/**
	 * 이 파서가 진정한 스트리밍 파싱을 지원하는지 여부를 반환합니다.
	 * <p>
	 * true를 반환하는 경우 parseAsStream()이 메모리 효율적으로 구현되어 있음을 의미합니다.
	 * false를 반환하는 경우 parseAsStream()이 기본 구현(일괄 파싱)을 사용함을 의미합니다.
	 * </p>
	 *
	 * @return 스트리밍 파싱 지원 여부
	 * @since 1.1.0
	 */
	default boolean supportsStreamingParsing() {
		return false;
	}
	
	/**
	 * 스트리밍 파싱 시 권장하는 버퍼 크기를 반환합니다.
	 *
	 * @return 권장 버퍼 크기 (바이트)
	 * @since 1.1.0
	 */
	default int getRecommendedBufferSize() {
		return 8192; // 8KB 기본값
	}
}
