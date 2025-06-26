package com.datamorph.parser;

import com.datamorph.core.DataRow;

import java.io.InputStream;
import java.util.List;

public class JsonParser implements Parser{
	@Override
	public List<DataRow> parse (String content) {
		return List.of();
	}

	@Override
	public List<DataRow> parse (InputStream input) {
		return List.of();
	}
}
