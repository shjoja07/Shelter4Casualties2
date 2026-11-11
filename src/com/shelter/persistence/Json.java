package com.shelter.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector y escritor de JSON mínimo, sin librerías externas.
 *
 * Escritura: acepta Map (objeto), List (arreglo), String, Boolean, números y null.
 * Lectura:   objetos -> LinkedHashMap, arreglos -> ArrayList, enteros -> Long,
 *            decimales -> Double, textos -> String, true/false -> Boolean, null -> null.
 */
public final class Json {

    private Json() {
    }

    // =========================================================
    // ESCRITURA
    // =========================================================

    /** Convierte el valor a JSON con sangría de 2 espacios. */
    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribirValor(valor, sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void escribirValor(Object v, StringBuilder sb, int nivel) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            escribirTexto(s, sb);
        } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            sb.append(v);
        } else if (v instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON no admite NaN ni infinito.");
            }
            sb.append(d);
        } else if (v instanceof Map<?, ?> mapa) {
            escribirObjeto(mapa, sb, nivel);
        } else if (v instanceof List<?> lista) {
            escribirArreglo(lista, sb, nivel);
        } else {
            throw new IllegalArgumentException("Tipo no soportado en JSON: " + v.getClass().getName());
        }
    }

    private static void escribirObjeto(Map<?, ?> mapa, StringBuilder sb, int nivel) {
        if (mapa.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<?, ?> e : mapa.entrySet()) {
            sangria(sb, nivel + 1);
            escribirTexto(String.valueOf(e.getKey()), sb);
            sb.append(": ");
            escribirValor(e.getValue(), sb, nivel + 1);
            if (++i < mapa.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sangria(sb, nivel);
        sb.append('}');
    }

    private static void escribirArreglo(List<?> lista, StringBuilder sb, int nivel) {
        if (lista.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < lista.size(); i++) {
            sangria(sb, nivel + 1);
            escribirValor(lista.get(i), sb, nivel + 1);
            if (i < lista.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sangria(sb, nivel);
        sb.append(']');
    }

    private static void sangria(StringBuilder sb, int nivel) {
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
    }

    private static void escribirTexto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // =========================================================
    // LECTURA
    // =========================================================

    /** Convierte un texto JSON en Map / List / String / Long / Double / Boolean / null. */
    public static Object leer(String texto) {
        Lector lector = new Lector(texto);
        lector.saltarEspacios();
        Object valor = lector.leerValor();
        lector.saltarEspacios();
        if (!lector.fin()) {
            throw lector.error("Hay texto después del JSON");
        }
        return valor;
    }

    private static final class Lector {
        private final String t;
        private int pos;

        Lector(String texto) {
            this.t = texto;
            // Ignora la marca BOM que algunos editores agregan al inicio
            this.pos = (!texto.isEmpty() && texto.charAt(0) == '\uFEFF') ? 1 : 0;
        }

        boolean fin() {
            return pos >= t.length();
        }

        void saltarEspacios() {
            while (!fin()) {
                char c = t.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        IllegalArgumentException error(String mensaje) {
            int linea = 1;
            int columna = 1;
            for (int i = 0; i < Math.min(pos, t.length()); i++) {
                if (t.charAt(i) == '\n') {
                    linea++;
                    columna = 1;
                } else {
                    columna++;
                }
            }
            return new IllegalArgumentException("JSON inválido (línea " + linea + ", columna " + columna + "): " + mensaje);
        }

        Object leerValor() {
            if (fin()) {
                throw error("Se esperaba un valor");
            }
            switch (t.charAt(pos)) {
                case '{':
                    return leerObjeto();
                case '[':
                    return leerArreglo();
                case '"':
                    return leerTexto();
                case 't':
                    literal("true");
                    return Boolean.TRUE;
                case 'f':
                    literal("false");
                    return Boolean.FALSE;
                case 'n':
                    literal("null");
                    return null;
                default:
                    return leerNumero();
            }
        }

        private void literal(String esperado) {
            if (!t.startsWith(esperado, pos)) {
                throw error("Valor no reconocido");
            }
            pos += esperado.length();
        }

        private void esperar(char c) {
            if (fin() || t.charAt(pos) != c) {
                throw error("Se esperaba '" + c + "'");
            }
            pos++;
        }

        private Map<String, Object> leerObjeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++; // {
            saltarEspacios();
            if (!fin() && t.charAt(pos) == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                saltarEspacios();
                if (fin() || t.charAt(pos) != '"') {
                    throw error("Se esperaba el nombre de un campo entre comillas");
                }
                String clave = leerTexto();
                saltarEspacios();
                esperar(':');
                saltarEspacios();
                mapa.put(clave, leerValor());
                saltarEspacios();
                if (fin()) {
                    throw error("Objeto sin cerrar");
                }
                char c = t.charAt(pos);
                if (c == '}') {
                    pos++;
                    return mapa;
                }
                if (c != ',') {
                    throw error("Se esperaba ',' o '}'");
                }
                pos++;
            }
        }

        private List<Object> leerArreglo() {
            List<Object> lista = new ArrayList<>();
            pos++; // [
            saltarEspacios();
            if (!fin() && t.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                saltarEspacios();
                lista.add(leerValor());
                saltarEspacios();
                if (fin()) {
                    throw error("Arreglo sin cerrar");
                }
                char c = t.charAt(pos);
                if (c == ']') {
                    pos++;
                    return lista;
                }
                if (c != ',') {
                    throw error("Se esperaba ',' o ']'");
                }
                pos++;
            }
        }

        private String leerTexto() {
            StringBuilder sb = new StringBuilder();
            pos++; // comilla inicial
            while (true) {
                if (fin()) {
                    throw error("Texto sin cerrar");
                }
                char c = t.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (fin()) {
                        throw error("Secuencia de escape incompleta");
                    }
                    char e = t.charAt(pos++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > t.length()) {
                                throw error("Secuencia \\u incompleta");
                            }
                            int codigo = 0;
                            for (int i = 0; i < 4; i++) {
                                int digito = Character.digit(t.charAt(pos + i), 16);
                                if (digito < 0) {
                                    throw error("Secuencia \\u inválida");
                                }
                                codigo = codigo * 16 + digito;
                            }
                            sb.append((char) codigo);
                            pos += 4;
                        }
                        default -> throw error("Escape no válido: \\" + e);
                    }
                } else if (c < 0x20) {
                    throw error("Carácter de control dentro de un texto");
                } else {
                    sb.append(c);
                }
            }
        }

        private Object leerNumero() {
            int inicio = pos;
            while (!fin() && "+-0123456789.eE".indexOf(t.charAt(pos)) >= 0) {
                pos++;
            }
            if (inicio == pos) {
                throw error("Valor no reconocido");
            }
            String numero = t.substring(inicio, pos);
            try {
                if (numero.indexOf('.') >= 0 || numero.indexOf('e') >= 0 || numero.indexOf('E') >= 0) {
                    return Double.parseDouble(numero);
                }
                return Long.parseLong(numero);
            } catch (NumberFormatException e) {
                pos = inicio;
                throw error("Número inválido: " + numero);
            }
        }
    }
}
