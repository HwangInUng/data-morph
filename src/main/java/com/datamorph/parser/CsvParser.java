package com.datamorph.parser;

import com.datamorph.core.DataRow;

import java.io.InputStream;
import java.util.List;

public class CsvParser implements Parser{
	private char delimiter;

	public CsvParser () {
	}

	private CsvParser (char delimiter) {
		this.delimiter = delimiter;
	}

	@Override
	public List<DataRow> parse (String content) {
		if( content == null || content.trim().isEmpty()) {
			throw new IllegalArgumentException("Content cannot be null or empty");
		}

		return List.of();
	}

	@Override
	public List<DataRow> parse (InputStream input) {
		return List.of();
	}

	public CsvParser withDelimiter (char delimiter) {
		return new CsvParser(delimiter);
	}
}
