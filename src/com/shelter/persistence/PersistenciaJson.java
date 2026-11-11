package com.shelter.persistence;

import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoMenor;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda y carga el estado del sistema (albergues con sus damnificados y recursos,
 * más el historial de asignaciones) en un archivo JSON.
 *
 * Estructura del archivo:
 * {
 *   "version": 1,
 *   "albergues": [ { ..., "damnificados": [...], "recursos": [...] } ],
 *   "historial": [ ... ]
 * }
 */
public final class PersistenciaJson {

    /** Versión del formato. Se sube si cambia la estructura del archivo. */
    public static final int VERSION = 1;

    private PersistenciaJson() {
    }

    /** Datos que se leen de un archivo JSON. */
    public record Datos(List<Albergue> albergues, List<RegistroAsignacion> historial) {
    }

    // =========================================================
    // GUARDAR
    // =========================================================

    public static void guardar(Path ruta, List<Albergue> albergues, List<RegistroAsignacion> historial)
            throws IOException {
        String json;
        try {
            Map<String, Object> raiz = new LinkedHashMap<>();
            raiz.put("version", VERSION);

            List<Object> listaAlbergues = new ArrayList<>();
            for (Albergue a : albergues) {
                listaAlbergues.add(albergueAMapa(a));
            }
            raiz.put("albergues", listaAlbergues);

            List<Object> listaHistorial = new ArrayList<>();
            for (RegistroAsignacion r : historial) {
                listaHistorial.add(registroAMapa(r));
            }
            raiz.put("historial", listaHistorial);

            json = Json.escribir(raiz);
        } catch (IllegalArgumentException e) {
            throw new IOException("No se pudo convertir los datos a JSON: " + e.getMessage(), e);
        }

        // Se escribe primero en un archivo temporal para no dañar el archivo anterior si algo falla
        Path destino = ruta.toAbsolutePath();
        Path carpeta = destino.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.writeString(temporal, json, StandardCharsets.UTF_8);
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Map<String, Object> albergueAMapa(Albergue a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("nombre", a.getNombre());
        m.put("distrito", a.getDistrito());
        // Campos nuevos (v1 compatible): al cargar son opcionales, así los archivos viejos siguen funcionando
        m.put("tipoInfraestructura", a.getTipoInfraestructura());
        m.put("areaM2", a.getAreaM2());
        m.put("capacidadMaxima", a.getCapacidadMaxima());
        m.put("ocupacionActual", a.getOcupacionActual());
        // El semáforo se guarda solo como información: al cargar se RECALCULA (es un valor derivado)
        m.put("estadoSemaforo", a.getEstadoSemaforo().name());

        List<Object> damnificados = new ArrayList<>();
        for (Damnificado d : a.getDamnificados()) {
            damnificados.add(damnificadoAMapa(d));
        }
        m.put("damnificados", damnificados);

        List<Object> recursos = new ArrayList<>();
        for (Recurso r : a.getRecursos()) {
            recursos.add(recursoAMapa(r));
        }
        m.put("recursos", recursos);
        return m;
    }

    private static Map<String, Object> damnificadoAMapa(Damnificado d) {
        Map<String, Object> m = new LinkedHashMap<>();
        // El campo "tipo" permite reconstruir la subclase correcta al cargar
        if (d instanceof DamnificadoVulnerable) {
            m.put("tipo", "VULNERABLE");
        } else if (d instanceof DamnificadoMenor) {
            m.put("tipo", "MENOR");
        } else if (d instanceof DamnificadoGeneral) {
            m.put("tipo", "GENERAL");
        } else {
            throw new IllegalArgumentException(
                    "Tipo de damnificado no soportado para guardar: " + d.getClass().getSimpleName());
        }
        m.put("dni", d.getDni());
        m.put("nombreCompleto", d.getNombreCompleto());
        m.put("edad", d.getEdad());
        // La atención médica ahora es de TODOS los tipos (antes solo se guardaba en VULNERABLE)
        m.put("atencionMedica", d.isAtencionMedica());

        if (d instanceof DamnificadoVulnerable v) {
            m.put("estadoGestacion", v.isEstadoGestacion());
            m.put("enfermedadCronica", v.isEnfermedadCronica());
            m.put("condicion", v.getCondicion());
            m.put("atendido", v.isAtendido());
            m.put("estable", v.isEstable());
        } else if (d instanceof DamnificadoMenor menor) {
            m.put("dniTutor", menor.getDniTutor());
            m.put("nombreTutor", menor.getNombreTutor());
            m.put("parentesco", menor.getParentesco());
        }
        return m;
    }

    private static Map<String, Object> recursoAMapa(Recurso r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tipo", r.getTipo().name());
        m.put("nombre", r.getNombre());
        m.put("unidad", r.getUnidad());
        m.put("cantidadDisponible", r.getCantidadDisponible());
        LocalDate vence = r.getFechaVencimiento();
        m.put("fechaVencimiento", vence == null ? null : vence.toString());
        return m;
    }

    private static Map<String, Object> registroAMapa(RegistroAsignacion r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idAlbergue", r.getIdAlbergue());
        m.put("dni", r.getDni());
        m.put("nombreDamnificado", r.getNombreDamnificado());
        m.put("recurso", r.getRecurso());
        m.put("unidad", r.getUnidad());
        m.put("cantidad", r.getCantidad());
        m.put("fechaHora", r.getFechaHora().toString());
        return m;
    }

    // =========================================================
    // CARGAR
    // =========================================================

    /**
     * Lee el archivo y devuelve los datos. No modifica nada del sistema: quien llama
     * decide si reemplaza su estado, así un archivo dañado nunca borra los datos actuales.
     */
    public static Datos cargar(Path ruta) throws IOException {
        String texto = Files.readString(ruta, StandardCharsets.UTF_8);
        try {
            Map<String, Object> raiz = comoObjeto(Json.leer(texto), "archivo");

            int version = entero(raiz, "version");
            if (version != VERSION) {
                throw new IllegalArgumentException(
                        "Versión de archivo no soportada: " + version + " (se esperaba " + VERSION + ").");
            }

            List<Albergue> albergues = new ArrayList<>();
            for (Object o : lista(raiz, "albergues")) {
                albergues.add(mapaAAlbergue(comoObjeto(o, "albergues")));
            }

            List<RegistroAsignacion> historial = new ArrayList<>();
            for (Object o : lista(raiz, "historial")) {
                historial.add(mapaARegistro(comoObjeto(o, "historial")));
            }
            return new Datos(albergues, historial);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IOException("Archivo JSON inválido: " + e.getMessage(), e);
        }
    }

    private static Albergue mapaAAlbergue(Map<String, Object> m) {
        String tipoInfra = textoOpcional(m, "tipoInfraestructura");
        Albergue a = new Albergue(texto(m, "id"), texto(m, "nombre"), texto(m, "distrito"),
                tipoInfra == null ? Albergue.INFRAESTRUCTURA_NO_ESPECIFICADA : tipoInfra,
                entero(m, "capacidadMaxima"), decimalOpcional(m, "areaM2", 0.0));

        // Los damnificados se cargan con alojar(): así se valida capacidad y DNI repetido.
        for (Object o : lista(m, "damnificados")) {
            Damnificado d = mapaADamnificado(comoObjeto(o, "damnificados"));
            if (!a.alojar(d)) {
                throw new IllegalArgumentException("No se pudo alojar al DNI " + d.getDni()
                        + " en el albergue '" + a.getId() + "' (capacidad excedida o DNI repetido).");
            }
        }
        // La ocupación guardada debe coincidir con las personas guardadas (la ocupación ya no es un contador aparte)
        if (entero(m, "ocupacionActual") != a.getOcupacionActual()) {
            throw new IllegalArgumentException("La ocupación guardada del albergue '" + a.getId()
                    + "' no coincide con sus damnificados.");
        }
        for (Object o : lista(m, "recursos")) {
            a.registrarRecurso(mapaARecurso(comoObjeto(o, "recursos")));
        }
        // "estadoSemaforo" se ignora aquí: AlbergueService.reemplazarAlbergues lo recalcula.
        return a;
    }

    private static Damnificado mapaADamnificado(Map<String, Object> m) {
        String tipo = texto(m, "tipo");
        String dni = texto(m, "dni");
        String nombre = texto(m, "nombreCompleto");
        int edad = entero(m, "edad");

        // Los archivos antiguos solo guardaban atencionMedica en VULNERABLE: si falta, se asume false
        boolean atencion = booleanoOpcional(m, "atencionMedica", false);

        switch (tipo) {
            case "GENERAL":
                return new DamnificadoGeneral(dni, nombre, edad, atencion);
            case "MENOR":
                return new DamnificadoMenor(dni, nombre, edad, atencion,
                        textoOpcional(m, "dniTutor"), textoOpcional(m, "nombreTutor"),
                        textoOpcional(m, "parentesco"));
            case "VULNERABLE": {
                String condicion = textoOpcional(m, "condicion");
                DamnificadoVulnerable v = new DamnificadoVulnerable(dni, nombre, edad, atencion,
                        condicion == null ? "" : condicion,
                        booleano(m, "estadoGestacion"), booleano(m, "enfermedadCronica"));
                v.setAtendido(booleanoOpcional(m, "atendido", false));
                v.setEstable(booleanoOpcional(m, "estable", false));
                return v;
            }
            default:
                throw new IllegalArgumentException("Tipo de damnificado desconocido: " + tipo);
        }
    }

    private static Recurso mapaARecurso(Map<String, Object> m) {
        TipoRecurso tipo;
        try {
            tipo = TipoRecurso.valueOf(texto(m, "tipo"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de recurso desconocido: " + m.get("tipo"));
        }
        String vence = textoOpcional(m, "fechaVencimiento");
        return new Recurso(tipo, texto(m, "nombre"), texto(m, "unidad"),
                decimal(m, "cantidadDisponible"), vence == null ? null : LocalDate.parse(vence));
    }

    private static RegistroAsignacion mapaARegistro(Map<String, Object> m) {
        return new RegistroAsignacion(texto(m, "idAlbergue"), texto(m, "dni"), texto(m, "nombreDamnificado"),
                texto(m, "recurso"), texto(m, "unidad"), decimal(m, "cantidad"),
                LocalDateTime.parse(texto(m, "fechaHora")));
    }

    // =========================================================
    // LECTURA SEGURA DE CAMPOS
    // =========================================================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoObjeto(Object o, String donde) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException("Se esperaba un objeto en '" + donde + "'.");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> lista(Map<String, Object> m, String clave) {
        if (m.get(clave) instanceof List<?> l) {
            return (List<Object>) l;
        }
        throw new IllegalArgumentException("Falta la lista '" + clave + "'.");
    }

    private static String texto(Map<String, Object> m, String clave) {
        if (m.get(clave) instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException("Falta el campo de texto '" + clave + "'.");
    }

    private static String textoOpcional(Map<String, Object> m, String clave) {
        Object o = m.get(clave);
        if (o == null) {
            return null;
        }
        if (o instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException("El campo '" + clave + "' debe ser texto o null.");
    }

    private static int entero(Map<String, Object> m, String clave) {
        if (m.get(clave) instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        throw new IllegalArgumentException("El campo '" + clave + "' debe ser un entero.");
    }

    private static double decimal(Map<String, Object> m, String clave) {
        if (m.get(clave) instanceof Number n) {
            return n.doubleValue();
        }
        throw new IllegalArgumentException("El campo '" + clave + "' debe ser un número.");
    }

    private static boolean booleano(Map<String, Object> m, String clave) {
        if (m.get(clave) instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException("El campo '" + clave + "' debe ser true o false.");
    }

    private static double decimalOpcional(Map<String, Object> m, String clave, double porDefecto) {
        Object o = m.get(clave);
        if (o == null) {
            return porDefecto;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        throw new IllegalArgumentException("El campo '" + clave + "' debe ser un número.");
    }

    private static boolean booleanoOpcional(Map<String, Object> m, String clave, boolean porDefecto) {
        Object o = m.get(clave);
        if (o == null) {
            return porDefecto;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException("El campo '" + clave + "' debe ser true o false.");
    }
}
