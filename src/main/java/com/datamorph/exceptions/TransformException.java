package com.datamorph.exceptions;

/**
 * Transform 연산 중 발생하는 예외를 나타내는 클래스
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class TransformException extends RuntimeException {
	private final String operationType;
	private final String fieldName;

	public TransformException (String message) {
		super(message);
		this.operationType = null;
		this.fieldName = null;
	}

	public TransformException (String message, Throwable cause) {
		super(message, cause);
		this.operationType = null;
		this.fieldName = null;
	}

	public TransformException (String operationType, String fieldName, String message) {
		super(String.format("[%s] %s (field: %s)", operationType, message, fieldName));
		this.operationType = operationType;
		this.fieldName = fieldName;
	}

	public TransformException (String operationType, String fieldName, String message, Throwable cause) {
		super(String.format("[%s] %s (field: %s)", operationType, message, fieldName), cause);
		this.operationType = operationType;
		this.fieldName = fieldName;
	}

	public String getOperationType () {
		return operationType;
	}

	public String getFieldName () {
		return fieldName;
	}
}
