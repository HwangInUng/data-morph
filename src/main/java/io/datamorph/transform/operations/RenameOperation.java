package io.datamorph.transform.operations;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.TransformException;

/**
 * 필드 이름을 변경하는 Transform 연산
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class RenameOperation implements TransformOperation {
	private final String oldName;
	private final String newName;

	public RenameOperation (String oldName, String newName) {
		if (oldName == null || oldName.trim().isEmpty()) {
			throw new TransformException("RENAME", oldName, "Old field name cannot be null or empty");
		}
		if (newName == null || newName.trim().isEmpty()) {
			throw new TransformException("RENAME", newName, "New field name cannot be null or empty");
		}

		this.oldName = oldName;
		this.newName = newName;
	}

	@Override
	public DataRow apply (DataRow row) {
		DataRow newRow = row.copy();

		if (newRow.has(oldName)) {
			Object value = newRow.getObject(oldName);

			newRow.set(newName, value);
			newRow.remove(oldName);
		}

		return newRow;
	}

	@Override
	public String getDescription () {
		return String.format("Rename field '%s' to '%s'", oldName, newName);
	}
}
