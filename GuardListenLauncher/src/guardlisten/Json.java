package guardlisten;

import java.util.*;

/** Минимальный JSON-парсер/писатель (Map, List, String, Double, Boolean, null). */
final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    static Object parse(String text) {
        Json j = new Json(text);
        j.ws();
        return j.value();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) { return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>(); }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) { return o instanceof List ? (List<Object>) o : new ArrayList<>(); }

    static String str(Map<String, Object> m, String k) { Object o = m.get(k); return o instanceof String ? (String) o : null; }

    static long num(Map<String, Object> m, String k) { Object o = m.get(k); return o instanceof Number ? ((Number) o).longValue() : 0; }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private Object value() {
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': i += 4; return Boolean.TRUE;
            case 'f': i += 5; return Boolean.FALSE;
            case 'n': i += 4; return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = string();
            ws(); i++; ws(); // ':'
            m.put(k, value());
            ws();
            if (s.charAt(i++) == '}') return m;
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++; ws();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            if (s.charAt(i++) == ']') return l;
        }
    }

    private String string() {
        StringBuilder b = new StringBuilder();
        i++;
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n': b.append('\n'); break;
                case 't': b.append('\t'); break;
                case 'r': b.append('\r'); break;
                case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break;
                case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                default: b.append(e);
            }
        }
    }

    private Double number() {
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        return Double.parseDouble(s.substring(st, i));
    }

    static String write(Object o) {
        StringBuilder b = new StringBuilder();
        write(o, b, 0);
        return b.toString();
    }

    private static void write(Object o, StringBuilder b, int d) {
        if (o == null) b.append("null");
        else if (o instanceof String) {
            b.append('"');
            for (char c : ((String) o).toCharArray()) {
                if (c == '"' || c == '\\') b.append('\\').append(c);
                else if (c == '\n') b.append("\\n");
                else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                else b.append(c);
            }
            b.append('"');
        } else if (o instanceof Number) {
            Number n = (Number) o;
            if (n.doubleValue() == Math.rint(n.doubleValue())) b.append(n.longValue()); else b.append(n);
        } else if (o instanceof Boolean) b.append(o);
        else if (o instanceof Map) {
            b.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) b.append(",");
                first = false;
                b.append("\n").append("  ".repeat(d + 1));
                write(String.valueOf(e.getKey()), b, d + 1);
                b.append(": ");
                write(e.getValue(), b, d + 1);
            }
            b.append("\n").append("  ".repeat(d)).append("}");
        } else if (o instanceof List) {
            b.append("[");
            boolean first = true;
            for (Object e : (List<?>) o) {
                if (!first) b.append(",");
                first = false;
                b.append("\n").append("  ".repeat(d + 1));
                write(e, b, d + 1);
            }
            b.append("\n").append("  ".repeat(d)).append("]");
        } else write(String.valueOf(o), b, d);
    }
}
