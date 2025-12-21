package info.openrocket.core.thrustcurve;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON parser to avoid external dependencies.
 * Capable of parsing the standard ThrustCurve API v1 responses.
 */
public class SimpleJsonParser {

	private final String json;
	private int index;

	public SimpleJsonParser(String json) {
		this.json = json;
		this.index = 0;
	}

	public static Object parse(String json) {
		return new SimpleJsonParser(json).parseValue();
	}

	private Object parseValue() {
		skipWhitespace();
		if (index >= json.length()) return null;

		char c = json.charAt(index);
		if (c == '{') return parseObject();
		if (c == '[') return parseArray();
		if (c == '"') return parseString();
		if (c == 't') { consume("true"); return true; }
		if (c == 'f') { consume("false"); return false; }
		if (c == 'n') { consume("null"); return null; }
		if (c == '-' || c == '.' || Character.isDigit(c)) return parseNumber();
		throw new RuntimeException("Unexpected character '" + c + "' at " + index);
	}

	private Map<String, Object> parseObject() {
		Map<String, Object> map = new HashMap<>();
		consume('{');
		skipWhitespace();
		if (peek() == '}') {
			consume('}');
			return map;
		}
		while (true) {
			String key = parseString();
			skipWhitespace();
			consume(':');
			Object value = parseValue();
			map.put(key, value);
			skipWhitespace();
			if (peek() == '}') break;
			consume(',');
			skipWhitespace();
		}
		consume('}');
		return map;
	}

	private List<Object> parseArray() {
		List<Object> list = new ArrayList<>();
		consume('[');
		skipWhitespace();
		if (peek() == ']') {
			consume(']');
			return list;
		}
		while (true) {
			list.add(parseValue());
			skipWhitespace();
			if (peek() == ']') break;
			consume(',');
			skipWhitespace();
		}
		consume(']');
		return list;
	}

	private String parseString() {
		consume('"');
		StringBuilder sb = new StringBuilder();
		while (index < json.length()) {
			char c = json.charAt(index++);
			if (c == '"') return sb.toString();
			if (c == '\\') {
				char escaped = json.charAt(index++);
				switch (escaped) {
					case '"': sb.append('"'); break;
					case '\\': sb.append('\\'); break;
					case '/': sb.append('/'); break;
					case 'b': sb.append('\b'); break;
					case 'f': sb.append('\f'); break;
					case 'n': sb.append('\n'); break;
					case 'r': sb.append('\r'); break;
					case 't': sb.append('\t'); break;
					case 'u':
						String hex = json.substring(index, index + 4);
						sb.append((char) Integer.parseInt(hex, 16));
						index += 4;
						break;
				}
			} else {
				sb.append(c);
			}
		}
		throw new RuntimeException("Unterminated string");
	}

	private Number parseNumber() {
		int start = index;
		while (index < json.length()) {
			char c = json.charAt(index);
			if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
				index++;
			} else {
				break;
			}
		}
		String numStr = json.substring(start, index);
		if (numStr.isEmpty()) {
			throw new RuntimeException("Invalid number at " + index);
		}
		if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
			return Double.parseDouble(numStr);
		} else {
			return Long.parseLong(numStr); // Use Long to be safe, cast to Int later
		}
	}

	private void skipWhitespace() {
		while (index < json.length()) {
			char c = json.charAt(index);
			if (Character.isWhitespace(c) || c == '\uFEFF') {
				index++;
				continue;
			}
			break;
		}
	}

	private void consume(char expected) {
		if (index >= json.length() || json.charAt(index) != expected) {
			throw new RuntimeException("Expected '" + expected + "' at " + index);
		}
		index++;
	}

	private void consume(String expected) {
		if (!json.startsWith(expected, index)) {
			throw new RuntimeException("Expected \"" + expected + "\" at " + index);
		}
		index += expected.length();
	}

	private char peek() {
		if (index >= json.length()) return 0;
		return json.charAt(index);
	}
}
