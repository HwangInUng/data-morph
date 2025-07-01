package com.datamorph.transform.operations;

import com.datamorph.core.DataRow;
import com.datamorph.exceptions.TransformException;

/**
 * 새로운 필드를 추가하는 Transform 연산
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class AddOperation implements TransformOperation {
	private final String fieldName;
	private final Object value;

	public AddOperation (String fieldName, Object value) {
		if (fieldName == null || fieldName.trim().isEmpty()) {
			throw new TransformException("ADD", fieldName, "Field name cannot be null or empty");
		}

		this.fieldName = fieldName;
		this.value = value;
	}

	@Override
	public DataRow apply (DataRow row) {
		DataRow newRow = row.copy();
		newRow.set(fieldName, value);

		return newRow;
	}

	@Override
	public String getDescription () {
		return String.format("Add field '%s' with value '%s'", fieldName, value);
	}
}
