package io.datamorph.transform.operations;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.TransformException;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 조건부 변환을 수행하는 Transform 연산
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class ConditionalOperation implements TransformOperation {
	private final Predicate<DataRow> condition;
	private final Function<DataRow, DataRow> action;
	private final String description;

	public ConditionalOperation (Predicate<DataRow> condition, Function<DataRow, DataRow> action) {
		this(condition, action, "Conditional transformation");
	}

	public ConditionalOperation (Predicate<DataRow> condition, Function<DataRow, DataRow> action, String description) {
		if (condition == null) {
			throw new TransformException("CONDITIONAL", null, "Condition cannot be null");
		}
		if (action == null) {
			throw new TransformException("CONDITIONAL", null, "Action cannot be null");
		}

		this.condition = condition;
		this.action = action;
		this.description = description != null ? description : "Conditional transformation";
	}

	@Override
	public DataRow apply (DataRow row) {
		DataRow newRow = row.copy();

		if (condition.test(newRow)) {
			return action.apply(newRow);
		}

		return newRow;
	}

	@Override
	public String getDescription () {
		return description;
	}
}
