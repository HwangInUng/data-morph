package com.datamorph.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 메모리 기반 DataSource 구현체
 * List<DataRow>를 내부적으로 관리합니다.
 *
 * @version 1.0.0
 * @since 2025.06.25
 */
public class ListDataSource implements DataSource {
	private final List<DataRow> rows;

	/**
	 * 주어진 DataRow 리스트로 새로운 ListDataSource를 생성합니다.
	 *
	 * @param rows DataRow 리스트
	 */
	public ListDataSource (List<DataRow> rows) {
		this.rows = new ArrayList<>(Objects.requireNonNull(rows, "Rows cannot be null"));
	}

	@Override
	public DataSource transform (Consumer<DataRow> transformer) {
		Objects.requireNonNull(transformer, "Transformer cannot be null");

		List<DataRow> transformedRows = rows.stream()
											.map(row -> {
												DataRow newRow = row.copy();
												transformer.accept(newRow);
												return newRow;
											})
											.collect(Collectors.toList());

		return new ListDataSource(transformedRows);
	}

	@Override
	public DataSource filter (Predicate<DataRow> predicate) {
		Objects.requireNonNull(predicate, "Predicate cannot be null");

		List<DataRow> filteredRows = rows.stream()
										 .filter(predicate)
										 .collect(Collectors.toList());

		return new ListDataSource(filteredRows);
	}

	@Override
	public List<DataRow> toList () {
		return List.copyOf(rows);
	}
}