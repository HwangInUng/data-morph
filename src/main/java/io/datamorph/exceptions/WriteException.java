package io.datamorph.exceptions;

/**
 * 데이터 쓰기 과정에서 발생하는 예외
 * 
 * @version 1.0.0
 * @since 2025.07.02
 */
public class WriteException extends Exception {
    
    /**
     * 기본 생성자
     */
    public WriteException() {
        super();
    }
    
    /**
     * 메시지를 포함하는 생성자
     *
     * @param message 예외 메시지
     */
    public WriteException(String message) {
        super(message);
    }
    
    /**
     * 메시지와 원인을 포함하는 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public WriteException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 원인만 포함하는 생성자
     *
     * @param cause 원인 예외
     */
    public WriteException(Throwable cause) {
        super(cause);
    }
}
