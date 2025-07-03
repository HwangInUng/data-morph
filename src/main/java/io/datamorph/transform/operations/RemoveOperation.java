package io.datamorph.transform.operations;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.TransformException;

/**
 * 필드를 제거하는 Transform 연산
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class RemoveOperation implements TransformOperation {
	private final String fieldName;

	public RemoveOperation (String fieldName) {
		if (fieldName == null || fieldName.trim().isEmpty()) {
			throw new TransformException("REMOVE", fieldName, "Field name cannot be null or empty");
		}

		this.fieldName = fieldName;
	}

	@Override
	public DataRow apply (DataRow row) {
		DataRow newRow = row.copy();
		newRow.remove(fieldName);

		return newRow;
	}

	@Override
	public String getDescription () {
		return String.format("Remove field '%s'", fieldName);
	}
}
