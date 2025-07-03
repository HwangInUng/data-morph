package io.datamorph.writer;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.WriteException;

import java.io.OutputStream;
import java.util.List;

/**
 * 데이터 쓰기를 담당하는 인터페이스
 * <p>
 * 다양한 포맷으로 데이터를 출력할 수 있는 기능을 제공합니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.02
 */
public interface Writer {
	
	/**
	 * DataRow 리스트를 OutputStream에 씁니다.
	 *
	 * @param data DataRow 리스트
	 * @param output 출력 스트림
	 * @throws WriteException 쓰기 중 오류가 발생한 경우
	 */
	void write(List<DataRow> data, OutputStream output) throws WriteException;
	
	/**
	 * DataRow 리스트를 문자열로 변환합니다.
	 *
	 * @param data DataRow 리스트
	 * @return 변환된 문자열
	 * @throws WriteException 변환 중 오류가 발생한 경우
	 */
	String writeToString(List<DataRow> data) throws WriteException;
	
	/**
	 * 이 Writer가 지원하는 파일 확장자를 반환합니다.
	 *
	 * @return 지원하는 파일 확장자 (예: "csv", "json")
	 */
	String getSupportedExtension();
	
	/**
	 * 이 Writer의 MIME 타입을 반환합니다.
	 *
	 * @return MIME 타입 (예: "text/csv", "application/json")
	 */
	String getMimeType();
}
