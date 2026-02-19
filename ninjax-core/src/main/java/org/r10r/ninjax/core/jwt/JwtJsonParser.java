package org.r10r.ninjax.core.jwt;

import java.util.LinkedHashMap;
import java.util.Map;

final class JwtJsonParser {

    // Hard limits to reduce DoS surface.
    private static final int MAX_DEPTH = 8;
    private static final int MAX_STRING_CHARS = 2048;
    private static final int MAX_KEYS = 256;

    static String minifyAndSerializeObject(Map<String, ?> obj) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, ?> e : obj.entrySet()) {
            if (e.getKey() == null) continue;
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            sb.append(serializeValue(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    static Map<String, Object> parseObject(String json) {
        if (json == null) throw new JwtException("JSON is null");
        Parser p = new Parser(json);
        Object v = p.parseValue(0);
        p.skipWs();
        if (!p.eof()) throw new JwtException("Trailing JSON content");
        if (!(v instanceof Map)) throw new JwtException("Expected JSON object");
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) v;
        return m;
    }

    private static String serializeValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof Number n) return n.toString();
        if (v instanceof Map<?, ?>) {
            // Not needed for JWT in your case, but keep it deterministic if nested maps appear
            @SuppressWarnings("unchecked")
            Map<String, ?> m = (Map<String, ?>) v;
            return minifyAndSerializeObject(m);
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s; }

        boolean eof() { return i >= s.length(); }

        void skipWs() {
            while (!eof()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
                else break;
            }
        }

        Object parseValue(int depth) {
            if (depth > MAX_DEPTH) throw new JwtException("JSON nesting too deep");
            skipWs();
            if (eof()) throw new JwtException("Unexpected end of JSON");
            char c = s.charAt(i);
            if (c == '{') return parseObject(depth + 1);
            if (c == '[') throw new JwtException("JSON arrays are not supported");
            if (c == '"') return parseString();
            if (c == 't') return parseLiteral("true", Boolean.TRUE);
            if (c == 'f') return parseLiteral("false", Boolean.FALSE);
            if (c == 'n') return parseLiteral("null", null);
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            throw new JwtException("Unexpected JSON character: " + c);
        }

        Map<String, Object> parseObject(int depth) {
            expect('{');
            skipWs();
            Map<String, Object> m = new LinkedHashMap<>();
            if (peek('}')) { i++; return m; }

            int keys = 0;
            while (true) {
                skipWs();
                String key = parseString();
                keys++;
                if (keys > MAX_KEYS) throw new JwtException("Too many JSON object keys");

                // (4) Reject duplicate keys to avoid ambiguity
                if (m.containsKey(key)) throw new JwtException("Duplicate JSON key: " + key);

                skipWs();
                expect(':');
                Object val = parseValue(depth);
                m.put(key, val);
                skipWs();
                if (peek('}')) { i++; return m; }
                expect(',');
            }
        }

        String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (eof()) throw new JwtException("Unterminated string");
                char c = s.charAt(i++);
                if (c == '"') break;

                if (c == '\\') {
                    if (eof()) throw new JwtException("Bad escape");
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"', '\\', '/' -> out.append(e);
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw new JwtException("Bad unicode escape");
                            int code = Integer.parseInt(s.substring(i, i + 4), 16);
                            i += 4;
                            out.append((char) code);
                        }
                        default -> throw new JwtException("Bad escape: \\" + e);
                    }
                } else {
                    out.append(c);
                }

                if (out.length() > MAX_STRING_CHARS) {
                    throw new JwtException("JSON string too long");
                }
            }
            return out.toString();
        }

        Number parseNumber() {
            int start = i;
            if (s.charAt(i) == '-') i++;
            while (!eof() && Character.isDigit(s.charAt(i))) i++;

            // Permit floats syntactically, but they’ll be rejected for date claims later.
            boolean isFloat = false;
            if (!eof() && s.charAt(i) == '.') {
                isFloat = true;
                i++;
                while (!eof() && Character.isDigit(s.charAt(i))) i++;
            }
            if (!eof() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                isFloat = true;
                i++;
                if (!eof() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                while (!eof() && Character.isDigit(s.charAt(i))) i++;
            }

            String num = s.substring(start, i);
            try {
                if (isFloat) return Double.parseDouble(num);
                long l = Long.parseLong(num);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                return l;
            } catch (NumberFormatException e) {
                throw new JwtException("Bad number: " + num);
            }
        }

        Object parseLiteral(String lit, Object val) {
            if (s.regionMatches(i, lit, 0, lit.length())) {
                i += lit.length();
                return val;
            }
            throw new JwtException("Expected literal: " + lit);
        }

        void expect(char c) {
            if (eof() || s.charAt(i) != c) throw new JwtException("Expected '" + c + "'");
            i++;
        }

        boolean peek(char c) {
            return !eof() && s.charAt(i) == c;
        }
    }

    private JwtJsonParser() {}
}
