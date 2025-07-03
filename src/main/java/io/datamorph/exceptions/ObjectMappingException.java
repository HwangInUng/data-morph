package io.datamorph.exceptions;

/**
 * 객체 매핑 과정에서 발생하는 예외를 나타내는 클래스
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class ObjectMappingException extends Exception {

    /**
     * 메시지만으로 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public ObjectMappingException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인으로 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param cause   원인 예외
     */
    public ObjectMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인 예외만으로 예외를 생성합니다.
     *
     * @param cause 원인 예외
     */
    public ObjectMappingException(Throwable cause) {
        super(cause);
    }
}
