package com.datamorph.parser;

import com.datamorph.core.DataRow;

import java.io.InputStream;
import java.util.List;

public interface Parser {
	List<DataRow> parse(String content);
	List<DataRow> parse(InputStream input);
}
