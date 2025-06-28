package com.datamorph.error;

/**
 * 데이터 파싱 중 발생하는 예외를 나타내는 클래스
 */
public class ParseException extends RuntimeException {
	
	/**
	 * 기본 생성자
	 */
	public ParseException() {
		super();
	}
	
	/**
	 * 메시지를 포함한 생성자
	 * 
	 * @param message 예외 메시지
	 */
	public ParseException(String message) {
		super(message);
	}
	
	/**
	 * 메시지와 원인을 포함한 생성자
	 * 
	 * @param message 예외 메시지
	 * @param cause 원인 예외
	 */
	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * 원인만 포함한 생성자
	 * 
	 * @param cause 원인 예외
	 */
	public ParseException(Throwable cause) {
		super(cause);
	}
}
