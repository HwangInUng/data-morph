package io.datamorph.parser;

import io.datamorph.core.DataRow;
import io.datamorph.exceptions.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonParser extends AbstractParser {

	@Override
	public List<DataRow> parse(String content) {
		validateInput(content, "JSON");
		
		if (isEmpty(content)) {
			return getEmptyResult();
		}

		try {
			Object parsed = parseJson(content.trim());
			return convertToDataRows(parsed);
		} catch (Exception e) {
			throw createParseException("Invalid JSON format: " + e.getMessage(), e);
		}
	}

	/**
	 * 간단한 JSON 파서 (라이브러리 의존성 없이 구현)
	 */
	private Object parseJson(String json) {
		JsonTokenizer tokenizer = new JsonTokenizer(json);
		return parseValue(tokenizer);
	}

	private Object parseValue(JsonTokenizer tokenizer) {
		tokenizer.skipWhitespace();
		char ch = tokenizer.peek();

		return switch (ch) {
			case '{' -> parseObject(tokenizer);
			case '[' -> parseArray(tokenizer);
			case '"' -> parseString(tokenizer);
			case 't', 'f' -> parseBoolean(tokenizer);
			case 'n' -> parseNull(tokenizer);
			default -> parseNumber(tokenizer);
		};
	}

	private Map<String, Object> parseObject(JsonTokenizer tokenizer) {
		Map<String, Object> obj = new java.util.LinkedHashMap<>();
		tokenizer.consume('{');
		tokenizer.skipWhitespace();

		if (tokenizer.peek() == '}') {
			tokenizer.consume('}');
			return obj;
		}

		while (true) {
			tokenizer.skipWhitespace();
			String key = parseString(tokenizer);
			tokenizer.skipWhitespace();
			tokenizer.consume(':');
			Object value = parseValue(tokenizer);
			obj.put(key, value);

			tokenizer.skipWhitespace();
			char ch = tokenizer.peek();
			if (ch == '}') {
				tokenizer.consume('}');
				break;
			} else if (ch == ',') {
				tokenizer.consume(',');
			} else {
				throw createParseException("Expected ',' or '}' in object");
			}
		}

		return obj;
	}

	private List<Object> parseArray(JsonTokenizer tokenizer) {
		List<Object> array = new ArrayList<>();
		tokenizer.consume('[');
		tokenizer.skipWhitespace();

		if (tokenizer.peek() == ']') {
			tokenizer.consume(']');
			return array;
		}

		while (true) {
			Object value = parseValue(tokenizer);
			array.add(value);

			tokenizer.skipWhitespace();
			char ch = tokenizer.peek();
			if (ch == ']') {
				tokenizer.consume(']');
				break;
			} else if (ch == ',') {
				tokenizer.consume(',');
			} else {
				throw createParseException("Expected ',' or ']' in array");
			}
		}

		return array;
	}

	private String parseString(JsonTokenizer tokenizer) {
		StringBuilder sb = new StringBuilder();
		tokenizer.consume('"');

		while (tokenizer.hasNext()) {
			char ch = tokenizer.next();
			if (ch == '"') {
				return sb.toString();
			} else if (ch == '\\') {
				if (!tokenizer.hasNext()) {
					throw createParseException("Unterminated string escape");
				}
				char escaped = tokenizer.next();
				switch (escaped) {
					case '"' -> sb.append('"');
					case '\\' -> sb.append('\\');
					case '/' -> sb.append('/');
					case 'b' -> sb.append('\b');
					case 'f' -> sb.append('\f');
					case 'n' -> sb.append('\n');
					case 'r' -> sb.append('\r');
					case 't' -> sb.append('\t');
					default -> throw createParseException("Invalid escape character: \\" + escaped);
				}
			} else {
				sb.append(ch);
			}
		}

		throw createParseException("Unterminated string");
	}

	private Boolean parseBoolean(JsonTokenizer tokenizer) {
		if (tokenizer.peek() == 't') {
			tokenizer.consume("true");
			return true;
		} else {
			tokenizer.consume("false");
			return false;
		}
	}

	private Object parseNull(JsonTokenizer tokenizer) {
		tokenizer.consume("null");
		return null;
	}

	private Number parseNumber(JsonTokenizer tokenizer) {
		StringBuilder sb = new StringBuilder();

		if (tokenizer.peek() == '-') {
			sb.append(tokenizer.next());
		}

		while (tokenizer.hasNext() && Character.isDigit(tokenizer.peek())) {
			sb.append(tokenizer.next());
		}

		if (tokenizer.hasNext() && tokenizer.peek() == '.') {
			sb.append(tokenizer.next());
			while (tokenizer.hasNext() && Character.isDigit(tokenizer.peek())) {
				sb.append(tokenizer.next());
			}
			return Double.parseDouble(sb.toString());
		}

		return Integer.parseInt(sb.toString());
	}

	/**
	 * 파싱된 JSON을 DataRow 리스트로 변환
	 */
	@SuppressWarnings("unchecked")
	private List<DataRow> convertToDataRows(Object parsed) {
		if (parsed instanceof List) {
			List<Object> list = (List<Object>) parsed;
			List<DataRow> result = new ArrayList<>();

			for (Object item : list) {
				if (item instanceof Map) {
					DataRow row = convertMapToDataRow((Map<String, Object>) item);
					result.add(row);
				}
			}

			return result;
		}

		throw createParseException("JSON must be an array of objects");
	}

	/**
	 * 단일 JSON 객체를 DataRow로 변환 (JsonLinesParser에서 사용)
	 */
	public DataRow convertJsonToDataRow(String jsonObject) {
		try {
			Object parsed = parseJson(jsonObject.trim());
			if (parsed instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) parsed;
				return convertMapToDataRow(map);
			}
			throw createParseException("JSON must be an object");
		} catch (Exception e) {
			throw createParseException("Failed to parse JSON object: " + e.getMessage(), e);
		}
	}

	/**
	 * Map을 DataRow로 변환 (중첩 객체 평면화 포함)
	 */
	@SuppressWarnings("unchecked")
	protected DataRow convertMapToDataRow(Map<String, Object> map) {
		DataRow row = new DataRow();
		flattenMap(map, "", row);
		return row;
	}

	/**
	 * 중첩된 Map을 평면화하여 DataRow에 저장
	 */
	@SuppressWarnings("unchecked")
	private void flattenMap(Map<String, Object> map, String prefix, DataRow row) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			Object value = entry.getValue();

			if (value instanceof Map) {
				// 중첩 객체 재귀 처리
				flattenMap((Map<String, Object>) value, key, row);
			} else if (value instanceof List) {
				// 배열을 문자열로 변환
				List<Object> list = (List<Object>) value;
				String arrayStr = list.stream()
					.map(Object::toString)
					.reduce((a, b) -> a + ", " + b)
					.map(s -> "[" + s + "]")
					.orElse("[]");
				row.set(key, arrayStr);
			} else {
				row.set(key, value);
			}
		}
	}

	/**
	 * 간단한 JSON 토크나이저
	 */
	private static class JsonTokenizer {
		private final String json;
		private int position = 0;

		public JsonTokenizer(String json) {
			this.json = json;
		}

		public boolean hasNext() {
			return position < json.length();
		}

		public char next() {
			if (!hasNext()) {
				throw new ParseException("Unexpected end of JSON");
			}
			return json.charAt(position++);
		}

		public char peek() {
			if (!hasNext()) {
				throw new ParseException("Unexpected end of JSON");
			}
			return json.charAt(position);
		}

		public void consume(char expected) {
			if (!hasNext() || next() != expected) {
				throw new ParseException("Expected '" + expected + "' at position " + position);
			}
		}

		public void consume(String expected) {
			for (char ch : expected.toCharArray()) {
				consume(ch);
			}
		}

		public void skipWhitespace() {
			while (hasNext() && Character.isWhitespace(peek())) {
				next();
			}
		}
	}
}
