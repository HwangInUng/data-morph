package io.datamorph.writer;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.WriteException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writer 인터페이스의 추상 구현체
 * <p>
 * 공통 기능을 제공하고 구체적인 포맷별 Writer들의 기반이 됩니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.02
 */
public abstract class AbstractWriter implements Writer {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String writeToString(List<DataRow> data) throws WriteException {
		if (data == null) {
			throw new IllegalArgumentException("Data cannot be null");
		}
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			write(data, baos);
			return baos.toString(StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new WriteException("Failed to convert data to string", e);
		}
	}
	
	/**
	 * 데이터 유효성을 검증합니다.
	 *
	 * @param data 검증할 데이터
	 * @param output 검증할 출력 스트림
	 * @throws IllegalArgumentException 데이터나 출력 스트림이 null인 경우
	 */
	protected void validateInputs(List<DataRow> data, OutputStream output) {
		if (data == null) {
			throw new IllegalArgumentException("Data cannot be null");
		}
		if (output == null) {
			throw new IllegalArgumentException("OutputStream cannot be null");
		}
	}
}
