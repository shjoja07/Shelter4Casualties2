package com.shelter;

import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoMenor;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.NivelPrioridad;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;
import com.shelter.persistence.PersistenciaJson;
import com.shelter.service.AlbergueService;
import com.shelter.service.AlbergueService.ResultadoDerivacion;
import com.shelter.service.AlbergueService.ResultadoIngreso;
import com.shelter.service.AsignacionRecurso;
import com.shelter.service.FiltroAlbergue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas de la lógica de negocio. Corren en cualquier
 * equipo con Java 17 sin descargar librerías. Además, cada prueba es una lambda; en caso salga
 * una excepción o una verificación falla, se cuenta como fallo.
 *
 * Ejecución:  java -cp out com.shelter.PruebasShelter   (el programa termina con código 1 si algo falla)
 */
public class PruebasShelter {

    private static int exitosas = 0;
    private static int fallidas = 0;

    // ---------- mini-marco de pruebas ----------
    private interface Prueba { void ejecutar() throws Exception; }

    private static void prueba(String nombre, Prueba p) {
        try {
            p.ejecutar();
            exitosas++;
            System.out.println("  OK    " + nombre);
        } catch (Throwable t) {
            fallidas++;
            System.out.println("  FALLA " + nombre + "  ->  " + t);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    private static void esperarExcepcion(Prueba p, String mensaje) {
        try {
            p.ejecutar();
        } catch (IllegalArgumentException e) {
            return;
        } catch (Exception e) {
            throw new AssertionError(mensaje + " (se lanzó otra excepción: " + e + ")");
        }
        throw new AssertionError(mensaje + " (no lanzó IllegalArgumentException)");
    }

    // ---------- datos de ayuda ----------
    private static int contadorDni = 10000000;

    private static String dni() {
        return String.valueOf(contadorDni++);
    }

    private static Damnificado general(int edad) {
        return new DamnificadoGeneral(dni(), "Persona " + contadorDni, edad, false);
    }

    private static AlbergueService servicioConDosAlbergues() {
        AlbergueService s = new AlbergueService();
        s.registrarAlbergue(new Albergue("A1", "Central", "Lima Centro", "Coliseo", 2, 14));
        s.registrarAlbergue(new Albergue("A2", "Norte", "Los Olivos", "Colegio", 5, 40));
        return s;
    }

    private static Recurso agua(double litros) {
        return new Recurso(TipoRecurso.AGUA, "Agua", "litros", litros, null);
    }

    public static void main(String[] args) {
        System.out.println("=== PRUEBAS SHELTER 4 CASUALTIES 2 ===");

        // ------------------------------------------------------------------
        System.out.println("\n[Albergue y validaciones]");
        prueba("la ocupación se deriva de la lista de damnificados", () -> {
            Albergue a = new Albergue("X", "X", "d", 3);
            verificar(a.getOcupacionActual() == 0, "debe iniciar en 0");
            a.alojar(general(30));
            a.alojar(general(31));
            verificar(a.getOcupacionActual() == 2 && a.getCuposDisponibles() == 1, "ocupación 2, cupos 1");
        });
        prueba("no se puede modificar la lista de damnificados desde fuera", () -> {
            Albergue a = new Albergue("X", "X", "d", 3);
            boolean bloqueado = false;
            try {
                a.getDamnificados().add(general(20));
            } catch (UnsupportedOperationException e) {
                bloqueado = true;
            }
            verificar(bloqueado, "la lista debe ser de solo lectura");
        });
        prueba("alojar rechaza al llegar a la capacidad máxima", () -> {
            Albergue a = new Albergue("X", "X", "d", 1);
            verificar(a.alojar(general(20)), "el primero entra");
            verificar(!a.alojar(general(21)), "el segundo no cabe");
        });
        prueba("capacidad <= 0 y datos vacíos lanzan excepción", () -> {
            esperarExcepcion(() -> new Albergue("X", "X", "d", 0), "capacidad 0");
            esperarExcepcion(() -> new Albergue("X", "X", "d", -3), "capacidad negativa");
            esperarExcepcion(() -> new Albergue(" ", "X", "d", 3), "id vacío");
            esperarExcepcion(() -> new Albergue("X", "X", "d", "t", 3, -5), "área negativa");
        });
        prueba("ID de albergue duplicado se rechaza (sin distinguir mayúsculas)", () -> {
            AlbergueService s = servicioConDosAlbergues();
            verificar(!s.registrarAlbergue(new Albergue("a1", "Otro", "d", 5)), "a1 ya existe como A1");
            verificar(s.getAlbergues().size() == 2, "no se agregó");
        });
        prueba("edad inválida y menor de 18 estricto", () -> {
            esperarExcepcion(() -> new DamnificadoGeneral(dni(), "X", -1, false), "edad negativa");
            esperarExcepcion(() -> new DamnificadoGeneral(dni(), "X", 200, false), "edad > 120");
            esperarExcepcion(() -> new DamnificadoMenor(dni(), "X", 18, false, null), "un menor debe tener < 18");
            esperarExcepcion(() -> new DamnificadoGeneral(" ", "X", 30, false), "DNI vacío");
        });
        prueba("validación de formato de DNI", () -> {
            verificar(Damnificado.esDniValido("12345678"), "8 dígitos es válido");
            verificar(!Damnificado.esDniValido("1234567"), "7 dígitos no");
            verificar(!Damnificado.esDniValido("1234567a"), "letras no");
            verificar(!Damnificado.esDniValido(null), "null no");
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Prioridad unificada]");
        prueba("general adulto = BAJA; adulto mayor y niño de 5 años = ALTA", () -> {
            verificar(general(30).getNivelPrioridad() == NivelPrioridad.BAJA, "30 años BAJA");
            verificar(general(60).getNivelPrioridad() == NivelPrioridad.ALTA, "60 años ALTA");
            verificar(general(5).getNivelPrioridad() == NivelPrioridad.ALTA, "5 años ALTA");
            verificar(general(6).getNivelPrioridad() == NivelPrioridad.BAJA, "6 años BAJA");
        });
        prueba("atención médica en un General ya cuenta (antes se perdía) = CRITICA", () -> {
            Damnificado d = new DamnificadoGeneral(dni(), "Herido", 30, true);
            verificar(d.getNivelPrioridad() == NivelPrioridad.CRITICA, "debe ser CRITICA");
        });
        prueba("el vulnerable NUNCA es BAJA", () -> {
            Damnificado cronico = new DamnificadoVulnerable(dni(), "C", 30, false, "", false, true);
            Damnificado basico = new DamnificadoVulnerable(dni(), "B", 30, false, "Discapacitado", false, false);
            Damnificado gestante = new DamnificadoVulnerable(dni(), "G", 28, false, "", true, false);
            Damnificado urgente = new DamnificadoVulnerable(dni(), "U", 28, true, "", false, false);
            verificar(cronico.getNivelPrioridad() == NivelPrioridad.MEDIA, "crónico MEDIA");
            verificar(basico.getNivelPrioridad() == NivelPrioridad.MEDIA, "básico MEDIA");
            verificar(gestante.getNivelPrioridad() == NivelPrioridad.ALTA, "gestante ALTA");
            verificar(urgente.getNivelPrioridad() == NivelPrioridad.CRITICA, "atención médica CRITICA");
        });
        prueba("el número de prioridad y el texto no se contradicen", () -> {
            Damnificado gestante = new DamnificadoVulnerable(dni(), "G", 28, false, "", true, false);
            verificar(gestante.determinarNivelPrioridad().startsWith("ALTA"), "texto ALTA");
            verificar(new AlbergueService().calcularPrioridadAsignacion(gestante) == NivelPrioridad.ALTA.getOrden(),
                    "número consistente con el nivel");
        });
        prueba("menor sin tutor sube a ALTA; menor acompañado de 10 años es BAJA", () -> {
            Damnificado solo = new DamnificadoMenor(dni(), "Solo", 10, false, null);
            Damnificado acompanado = new DamnificadoMenor(dni(), "Acomp", 10, false, "11111111", "Madre", "madre");
            verificar(solo.getNivelPrioridad() == NivelPrioridad.ALTA, "no acompañado ALTA");
            verificar(acompanado.getNivelPrioridad() == NivelPrioridad.BAJA, "acompañado BAJA");
        });
        prueba("el constructor del menor guarda nombre del tutor y parentesco (bug corregido)", () -> {
            DamnificadoMenor m = new DamnificadoMenor(dni(), "Niña", 9, false, "22222222", "Rosa Pérez", "tía");
            verificar("Rosa Pérez".equals(m.getNombreTutor()), "nombre del tutor");
            verificar("tía".equals(m.getParentesco()), "parentesco");
            verificar(m.tieneTutor(), "tiene tutor");
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Ingreso, derivación y salida (RF-02, RF-08)]");
        prueba("registrarIngreso distingue cada resultado", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado d1 = general(30);
            verificar(s.registrarIngreso("A1", d1) == ResultadoIngreso.INGRESADO, "ingresa");
            verificar(s.registrarIngreso("ZZ", general(30)) == ResultadoIngreso.ALBERGUE_NO_ENCONTRADO, "no existe");
            verificar(s.registrarIngreso("A2", d1) == ResultadoIngreso.DNI_DUPLICADO, "mismo DNI en otro albergue");
            s.registrarIngreso("A1", general(31));
            verificar(s.registrarIngreso("A1", general(32)) == ResultadoIngreso.SIN_CUPO, "A1 (cap. 2) lleno");
        });
        prueba("derivar MUEVE a la persona: origen -1, destino +1 y listas correctas", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado d = general(40);
            s.registrarIngreso("A1", d);
            verificar(s.derivarDamnificado("A1", "A2", d.getDni()) == ResultadoDerivacion.DERIVADO, "derivado");
            Albergue a1 = s.buscarPorId("A1").get();
            Albergue a2 = s.buscarPorId("A2").get();
            verificar(a1.getOcupacionActual() == 0, "origen quedó en 0");
            verificar(a2.getOcupacionActual() == 1, "destino quedó en 1");
            verificar(a1.buscarDamnificado(d.getDni()).isEmpty(), "ya no está en el origen");
            verificar(a2.buscarDamnificado(d.getDni()).isPresent(), "ya está en el destino");
        });
        prueba("derivar: casos de error no cambian nada", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado d = general(40);
            s.registrarIngreso("A1", d);
            verificar(s.derivarDamnificado("A1", "A1", d.getDni()) == ResultadoDerivacion.MISMO_ALBERGUE, "mismo albergue");
            verificar(s.derivarDamnificado("A1", "ZZ", d.getDni()) == ResultadoDerivacion.DESTINO_NO_ENCONTRADO, "destino");
            verificar(s.derivarDamnificado("ZZ", "A2", d.getDni()) == ResultadoDerivacion.ORIGEN_NO_ENCONTRADO, "origen");
            verificar(s.derivarDamnificado("A1", "A2", "99999999") == ResultadoDerivacion.DAMNIFICADO_NO_ENCONTRADO, "persona");
            verificar(s.buscarPorId("A1").get().getOcupacionActual() == 1, "A1 sigue con 1");
        });
        prueba("derivar a un albergue lleno no mueve a nadie", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("A", "A", "d", 1));
            s.registrarAlbergue(new Albergue("B", "B", "d", 1));
            Damnificado x = general(30);
            Damnificado y = general(31);
            s.registrarIngreso("A", x);
            s.registrarIngreso("B", y);
            verificar(s.derivarDamnificado("A", "B", x.getDni()) == ResultadoDerivacion.DESTINO_SIN_CUPO, "sin cupo");
            verificar(s.buscarPorId("A").get().buscarDamnificado(x.getDni()).isPresent(), "x sigue en A");
        });
        prueba("registrarSalida libera el cupo", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado d = general(40);
            s.registrarIngreso("A1", d);
            verificar(s.registrarSalida("A1", d.getDni()), "sale");
            verificar(s.buscarPorId("A1").get().getOcupacionActual() == 0, "cupo liberado");
            verificar(!s.registrarSalida("A1", d.getDni()), "no puede salir dos veces");
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Reparto de recursos (RF-04)]");
        prueba("stock fraccionario: no lanza excepción y no deja negativos (bug crítico corregido)", () -> {
            AlbergueService s = servicioConDosAlbergues();
            s.registrarIngreso("A2", general(30));
            s.registrarIngreso("A2", general(31));
            s.registrarIngreso("A2", general(32));
            Albergue a2 = s.buscarPorId("A2").get();
            Recurso arroz = new Recurso(TipoRecurso.ALIMENTO, "Arroz", "kg", 2.5, null);
            s.registrarRecurso("A2", arroz);
            AsignacionRecurso asig = new AsignacionRecurso(s);
            List<RegistroAsignacion> r = asig.asignar(a2, arroz, 10);
            double entregado = r.stream().mapToDouble(RegistroAsignacion::getCantidad).sum();
            verificar(Math.abs(entregado - 2.5) < 1e-9, "se entrega exactamente el stock: " + entregado);
            verificar(arroz.getCantidadDisponible() >= 0 && arroz.getCantidadDisponible() < 1e-9, "stock en 0, no negativo");
        });
        prueba("con poco stock, recibe primero quien tiene mayor prioridad", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado comun = general(30);
            Damnificado critico = new DamnificadoGeneral(dni(), "Herido", 30, true);
            Damnificado mayor = general(70);
            s.registrarIngreso("A2", comun);
            s.registrarIngreso("A2", mayor);
            s.registrarIngreso("A2", critico);
            Albergue a2 = s.buscarPorId("A2").get();
            Recurso pan = new Recurso(TipoRecurso.ALIMENTO, "Pan", "kg", 2, null);
            s.registrarRecurso("A2", pan);
            List<RegistroAsignacion> r = new AsignacionRecurso(s).asignar(a2, pan, 5);
            verificar(r.size() == 2, "solo alcanza para 2");
            verificar(r.get(0).getDni().equals(critico.getDni()), "primero el crítico");
            verificar(r.get(1).getDni().equals(mayor.getDni()), "segundo el adulto mayor");
        });
        prueba("desempate por tiempo: en la 2.ª ronda recibe quien no recibió antes", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado p1 = general(30);
            Damnificado p2 = general(31);
            s.registrarIngreso("A2", p1);
            s.registrarIngreso("A2", p2);
            Albergue a2 = s.buscarPorId("A2").get();
            Recurso pan = new Recurso(TipoRecurso.ALIMENTO, "Pan", "kg", 10, null);
            s.registrarRecurso("A2", pan);
            AsignacionRecurso asig = new AsignacionRecurso(s);
            asig.asignar(a2, pan, 1);                       // 1.ª ronda: solo uno recibe
            String recibio = asig.getHistorial().get(0).getDni();
            List<RegistroAsignacion> segunda = asig.asignar(a2, pan, 1); // 2.ª ronda
            verificar(!segunda.get(0).getDni().equals(recibio), "debe recibir el otro (no repetir al mismo)");
        });
        prueba("la MEDICINA solo se reparte a quien la necesita", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado sano = general(30);
            Damnificado cronico = new DamnificadoVulnerable(dni(), "Crónico", 50, false, "", false, true);
            s.registrarIngreso("A2", sano);
            s.registrarIngreso("A2", cronico);
            Albergue a2 = s.buscarPorId("A2").get();
            Recurso med = new Recurso(TipoRecurso.MEDICINA, "Insulina", "dosis", 10, null);
            s.registrarRecurso("A2", med);
            List<RegistroAsignacion> r = new AsignacionRecurso(s).asignar(a2, med, 10);
            verificar(r.size() == 1 && r.get(0).getDni().equals(cronico.getDni()), "solo el crónico recibe");
        });
        prueba("el agua usa 15 L por persona por defecto (mínimo Sphere)", () -> {
            AlbergueService s = servicioConDosAlbergues();
            s.registrarIngreso("A2", general(30));
            Albergue a2 = s.buscarPorId("A2").get();
            Recurso agua = agua(100);
            s.registrarRecurso("A2", agua);
            List<RegistroAsignacion> r = new AsignacionRecurso(s).asignar(a2, agua, 100);
            verificar(r.size() == 1 && Math.abs(r.get(0).getCantidad() - 15.0) < 1e-9, "15 L a la persona");
        });
        prueba("validaciones del reparto: vencido, ajeno y cantidades inválidas", () -> {
            AlbergueService s = servicioConDosAlbergues();
            s.registrarIngreso("A2", general(30));
            Albergue a2 = s.buscarPorId("A2").get();
            AsignacionRecurso asig = new AsignacionRecurso(s);
            Recurso vencido = new Recurso(TipoRecurso.ALIMENTO, "Lata", "u", 5, LocalDate.now().minusDays(1));
            a2.registrarRecurso(vencido); // se agrega directo al modelo para simular un lote que venció
            esperarExcepcion(() -> asig.asignar(a2, vencido, 1), "recurso vencido");
            Recurso ajeno = new Recurso(TipoRecurso.ALIMENTO, "Ajeno", "u", 5, null);
            esperarExcepcion(() -> asig.asignar(a2, ajeno, 1), "recurso que no es del albergue");
            Recurso ok = new Recurso(TipoRecurso.ALIMENTO, "Ok", "u", 5, null);
            a2.registrarRecurso(ok);
            esperarExcepcion(() -> asig.asignar(a2, ok, 0), "cantidad 0");
            esperarExcepcion(() -> asig.asignar(a2, ok, Double.NaN), "NaN");
            esperarExcepcion(() -> asig.asignar(a2, ok, 5, -1), "por persona negativa");
        });
        prueba("registrarRecurso: suma lotes iguales, rechaza vencidos y agua que no esté en litros", () -> {
            AlbergueService s = servicioConDosAlbergues();
            s.registrarRecurso("A1", new Recurso(TipoRecurso.ALIMENTO, "Arroz", "kg", 5, null));
            s.registrarRecurso("A1", new Recurso(TipoRecurso.ALIMENTO, "arroz", "KG", 3, null));
            verificar(s.buscarPorId("A1").get().getRecursos().size() == 1, "un solo lote");
            verificar(Math.abs(s.buscarPorId("A1").get().getRecursos().get(0).getCantidadDisponible() - 8) < 1e-9, "suma 8");
            esperarExcepcion(() -> s.registrarRecurso("A1",
                    new Recurso(TipoRecurso.ALIMENTO, "Viejo", "kg", 1, LocalDate.now().minusDays(2))), "vencido");
            esperarExcepcion(() -> s.registrarRecurso("A1",
                    new Recurso(TipoRecurso.AGUA, "Agua", "botellas", 10, null)), "agua en botellas");
            verificar(!s.registrarRecurso("ZZ", agua(10)), "albergue inexistente devuelve false");
        });
        prueba("actualizarRecurso cambia cantidad y fecha del inventario", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Recurso r = new Recurso(TipoRecurso.ALIMENTO, "Arroz", "kg", 5, null);
            s.registrarRecurso("A1", r);
            verificar(s.actualizarRecurso("A1", r, 20, LocalDate.now().plusDays(30)), "actualiza");
            verificar(Math.abs(r.getCantidadDisponible() - 20) < 1e-9, "cantidad 20");
            esperarExcepcion(() -> s.actualizarRecurso("A1", r, -1, null), "cantidad negativa");
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Semáforo y mínimos Sphere (RF-05, RF-06)]");
        prueba("ocupación: vacío VERDE, 70% AMARILLO, 90% o más ROJO", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("O", "O", "d", 10)); // sin área: no se evalúa espacio
            Albergue o = s.buscarPorId("O").get();
            verificar(s.calcularSemaforo(o) == NivelSemaforo.VERDE, "vacío VERDE");
            for (int i = 0; i < 7; i++) {
                s.registrarIngreso("O", general(30));
                if (i < 6) {
                    // mantener agua y alimentos para aislar el efecto de la ocupación
                    s.registrarRecurso("O", new Recurso(TipoRecurso.ALIMENTO, "A" + i, "kg", 1, null));
                }
            }
            s.registrarRecurso("O", agua(1000));
            s.registrarRecurso("O", new Recurso(TipoRecurso.MEDICINA, "M", "u", 5, null));
            verificar(s.buscarPorId("O").get().getEstadoSemaforo() == NivelSemaforo.AMARILLO, "7/10 = 70% AMARILLO");
            for (int i = 0; i < 2; i++) {
                s.registrarIngreso("O", general(30));
            }
            verificar(s.calcularSemaforo(o) == NivelSemaforo.ROJO, "9/10 = 90% ROJO");
        });
        prueba("el semáforo se actualiza solo al ingresar gente", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Albergue a1 = s.buscarPorId("A1").get();
            verificar(a1.getEstadoSemaforo() == NivelSemaforo.VERDE, "nace VERDE");
            s.registrarIngreso("A1", general(30));
            s.registrarIngreso("A1", general(31)); // 2/2 = 100%
            verificar(a1.getEstadoSemaforo() == NivelSemaforo.ROJO, "lleno => ROJO sin llamar a nadie más");
        });
        prueba("agua: por debajo de 15 L/persona AMARILLO; por debajo de 7.5 ROJO; suficiente = sin alerta", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("W", "W", "d", 100));
            Albergue w = s.buscarPorId("W").get();
            s.registrarIngreso("W", general(30));
            s.registrarIngreso("W", general(31));
            Recurso a = agua(20);                       // 10 L/persona
            s.registrarRecurso("W", a);
            verificar(tieneAlerta(s.validarMinimosSphere(w), "AGUA", NivelSemaforo.AMARILLO), "10 L/p AMARILLO");
            s.actualizarRecurso("W", a, 10, null);      // 5 L/persona
            verificar(tieneAlerta(s.validarMinimosSphere(w), "AGUA", NivelSemaforo.ROJO), "5 L/p ROJO");
            s.actualizarRecurso("W", a, 40, null);      // 20 L/persona
            verificar(s.validarMinimosSphere(w).stream().noneMatch(x -> x.codigo().equals("AGUA")), "20 L/p sin alerta");
        });
        prueba("espacio: menos de 3.5 m²/persona = ROJO (hacinamiento)", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("E", "E", "d", "Carpa", 4, 6)); // 6 m²
            s.registrarIngreso("E", general(30));
            s.registrarIngreso("E", general(31)); // 3 m²/persona < 3.5
            verificar(tieneAlerta(s.validarMinimosSphere(s.buscarPorId("E").get()), "ESPACIO", NivelSemaforo.ROJO), "hacinamiento");
        });
        prueba("capacidad declarada mayor a la que permite el área = alerta", () -> {
            AlbergueService s = new AlbergueService();
            Albergue a = new Albergue("C", "C", "d", "Carpa", 10, 14); // 14/3.5 = 4 personas máx.
            s.registrarAlbergue(a);
            verificar(s.capacidadSegunEspacio(a) == 4, "máximo 4 personas");
            verificar(tieneAlerta(s.validarMinimosSphere(a), "CAPACIDAD", NivelSemaforo.AMARILLO), "alerta de capacidad");
        });
        prueba("recurso crítico sin stock con ocupantes = ROJO; lote vencido = AMARILLO", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("R", "R", "d", 100));
            Albergue r = s.buscarPorId("R").get();
            s.registrarIngreso("R", general(30));
            verificar(tieneAlerta(s.evaluarRecursos(r), "RECURSO_FALTANTE", NivelSemaforo.ROJO), "falta ALIMENTO/MEDICINA");
            r.registrarRecurso(new Recurso(TipoRecurso.ABRIGO, "Manta vieja", "u", 3, LocalDate.now().minusDays(1)));
            verificar(tieneAlerta(s.evaluarRecursos(r), "RECURSO_VENCIDO", NivelSemaforo.AMARILLO), "lote vencido");
        });
        prueba("un albergue completo y abastecido queda VERDE", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("V", "V", "d", "Colegio", 10, 100));
            s.registrarIngreso("V", general(30));
            s.registrarRecurso("V", agua(100));
            s.registrarRecurso("V", new Recurso(TipoRecurso.ALIMENTO, "Arroz", "kg", 10, null));
            s.registrarRecurso("V", new Recurso(TipoRecurso.MEDICINA, "Paracetamol", "u", 10, null));
            verificar(s.buscarPorId("V").get().getEstadoSemaforo() == NivelSemaforo.VERDE,
                    "esperado VERDE, alertas: " + s.obtenerAlertas(s.buscarPorId("V").get()));
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Filtros (RF-07) y consultas (RF-09)]");
        prueba("filtrar por cupo, distrito, nivel y recurso faltante", () -> {
            AlbergueService s = servicioConDosAlbergues();
            s.registrarIngreso("A1", general(30));
            s.registrarIngreso("A1", general(31)); // A1 lleno => ROJO
            verificar(s.filtrarAlbergues(FiltroAlbergue.sinFiltros()).size() == 2, "sin filtros: 2");
            verificar(s.filtrarAlbergues(new FiltroAlbergue(1, null, null, null)).size() == 1, "con cupo: solo A2");
            verificar(s.filtrarAlbergues(new FiltroAlbergue(null, "olivos", null, null)).get(0).getId().equals("A2"), "distrito");
            verificar(s.filtrarAlbergues(new FiltroAlbergue(null, null, NivelSemaforo.ROJO, null)).get(0).getId().equals("A1"), "ROJO");
            verificar(s.filtrarAlbergues(new FiltroAlbergue(null, null, null, TipoRecurso.AGUA)).size() == 2, "ninguno tiene agua");
            s.registrarRecurso("A2", agua(50));
            verificar(s.filtrarAlbergues(new FiltroAlbergue(null, null, null, TipoRecurso.AGUA)).size() == 1, "solo A1 sin agua");
        });
        prueba("consultarDisponibilidad excluye el albergue indicado", () -> {
            AlbergueService s = servicioConDosAlbergues();
            verificar(s.consultarDisponibilidad("A1").size() == 1, "solo A2");
            verificar(s.consultarAlbergue("a2").isPresent(), "consultar sin distinguir mayúsculas");
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Persistencia JSON]");
        prueba("guardar y cargar conserva TODO (incluida atención médica de General y datos de Menor)", () -> {
            AlbergueService s = servicioConDosAlbergues();
            Damnificado herido = new DamnificadoGeneral("30000001", "Herido Gómez", 33, true);
            DamnificadoMenor menor = new DamnificadoMenor("30000002", "Ana Ruiz", 8, false, "30000003", "Luz Ruiz", "madre");
            DamnificadoVulnerable vul = new DamnificadoVulnerable("30000004", "Mario Díaz", 71, false, "Discapacitado", false, true);
            vul.setAtendido(true);
            s.registrarIngreso("A2", herido);
            s.registrarIngreso("A2", menor);
            s.registrarIngreso("A2", vul);
            s.registrarRecurso("A2", agua(75.5));
            s.registrarRecurso("A2", new Recurso(TipoRecurso.ALIMENTO, "Arroz", "kg", 12, LocalDate.now().plusDays(90)));
            AsignacionRecurso asig = new AsignacionRecurso(s);
            asig.asignar(s.buscarPorId("A2").get(), s.buscarPorId("A2").get().getRecursos().get(0), 30);

            Path tmp = Files.createTempFile("shelter", ".json");
            try {
                PersistenciaJson.guardar(tmp, s.getAlbergues(), asig.getHistorial());
                PersistenciaJson.Datos datos = PersistenciaJson.cargar(tmp);

                AlbergueService s2 = new AlbergueService();
                s2.reemplazarAlbergues(datos.albergues());
                AsignacionRecurso asig2 = new AsignacionRecurso(s2);
                asig2.reemplazarHistorial(datos.historial());

                Albergue a2 = s2.buscarPorId("A2").get();
                verificar(a2.getOcupacionActual() == 3, "3 alojados");
                verificar(a2.getAreaM2() == 40 && a2.getTipoInfraestructura().equals("Colegio"), "área e infraestructura");
                Damnificado h = a2.buscarDamnificado("30000001").get();
                verificar(h instanceof DamnificadoGeneral && h.isAtencionMedica(), "la atención médica del General se conservó");
                DamnificadoMenor m = (DamnificadoMenor) a2.buscarDamnificado("30000002").get();
                verificar("Luz Ruiz".equals(m.getNombreTutor()) && "madre".equals(m.getParentesco()), "tutor y parentesco");
                DamnificadoVulnerable v = (DamnificadoVulnerable) a2.buscarDamnificado("30000004").get();
                verificar("Discapacitado".equals(v.getCondicion()) && v.isEnfermedadCronica() && v.isAtendido(), "datos del vulnerable");
                verificar(a2.getRecursos().size() == 2, "2 recursos");
                verificar(asig2.getHistorial().size() == asig.getHistorial().size() && !asig2.getHistorial().isEmpty(), "historial");
                verificar(asig2.getHistorial().get(0).getFechaHora().equals(asig.getHistorial().get(0).getFechaHora()), "fecha del historial");
                verificar(s2.buscarPorId("A2").get().getEstadoSemaforo() == s.buscarPorId("A2").get().getEstadoSemaforo(),
                        "el semáforo se recalcula igual tras cargar");
            } finally {
                Files.deleteIfExists(tmp);
            }
        });
        prueba("cargar un archivo dañado lanza IOException y no toca el sistema", () -> {
            Path tmp = Files.createTempFile("shelter", ".json");
            try {
                Files.writeString(tmp, "{ esto no es json");
                boolean fallo = false;
                try {
                    PersistenciaJson.cargar(tmp);
                } catch (IOException e) {
                    fallo = true;
                }
                verificar(fallo, "debe fallar con IOException");
            } finally {
                Files.deleteIfExists(tmp);
            }
        });
        prueba("un JSON con ocupación que no coincide se rechaza", () -> {
            Path tmp = Files.createTempFile("shelter", ".json");
            try {
                Files.writeString(tmp, "{\"version\":1,\"albergues\":[{\"id\":\"A1\",\"nombre\":\"N\",\"distrito\":\"D\","
                        + "\"capacidadMaxima\":5,\"ocupacionActual\":3,\"estadoSemaforo\":\"VERDE\","
                        + "\"damnificados\":[],\"recursos\":[]}],\"historial\":[]}");
                boolean rechazado = false;
                try {
                    PersistenciaJson.cargar(tmp);
                } catch (IOException e) {
                    rechazado = true;
                }
                verificar(rechazado, "ocupación 3 con 0 personas debe rechazarse");
            } finally {
                Files.deleteIfExists(tmp);
            }
        });
        prueba("un JSON del formato anterior (sin campos nuevos) sigue cargando", () -> {
            Path tmp = Files.createTempFile("shelter", ".json");
            try {
                Files.writeString(tmp, "{\"version\":1,\"albergues\":[{\"id\":\"A1\",\"nombre\":\"N\",\"distrito\":\"D\","
                        + "\"capacidadMaxima\":5,\"ocupacionActual\":1,\"estadoSemaforo\":\"VERDE\","
                        + "\"damnificados\":[{\"tipo\":\"GENERAL\",\"dni\":\"40000001\",\"nombreCompleto\":\"Pedro\",\"edad\":30}],"
                        + "\"recursos\":[]}],\"historial\":[]}");
                PersistenciaJson.Datos d = PersistenciaJson.cargar(tmp);
                verificar(d.albergues().get(0).getOcupacionActual() == 1, "cargó 1 persona");
            } finally {
                Files.deleteIfExists(tmp);
            }
        });
        prueba("reemplazarAlbergues con IDs o DNI repetidos falla sin alterar el estado", () -> {
            AlbergueService s = servicioConDosAlbergues();
            List<Albergue> malos = new ArrayList<>();
            malos.add(new Albergue("Z", "Z1", "d", 3));
            malos.add(new Albergue("z", "Z2", "d", 3));
            esperarExcepcion(() -> s.reemplazarAlbergues(malos), "ID repetido");
            verificar(s.getAlbergues().size() == 2 && s.buscarPorId("A1").isPresent(), "el estado original sigue intacto");

            Albergue p = new Albergue("P", "P", "d", 3);
            Albergue q = new Albergue("Q", "Q", "d", 3);
            p.alojar(new DamnificadoGeneral("50000001", "Uno", 30, false));
            q.alojar(new DamnificadoGeneral("50000001", "Uno de nuevo", 30, false));
            esperarExcepcion(() -> s.reemplazarAlbergues(List.of(p, q)), "DNI repetido en la red");
            verificar(s.getAlbergues().size() == 2, "sigue intacto");
        });

        // ------------------------------------------------------------------
        System.out.println("\n[Rendimiento (RNF-02)]");
        prueba("calcular prioridad y repartir con 2000 ocupantes tarda menos de 5 s", () -> {
            AlbergueService s = new AlbergueService();
            s.registrarAlbergue(new Albergue("G", "Grande", "d", "Estadio", 2000, 10000));
            for (int i = 0; i < 2000; i++) {
                int edad = (i % 3 == 0) ? 70 : (i % 3 == 1 ? 4 : 30);
                s.registrarIngreso("G", new DamnificadoGeneral(String.format("%08d", 60000000 + i), "P" + i, edad, i % 50 == 0));
            }
            Albergue g = s.buscarPorId("G").get();
            Recurso pan = new Recurso(TipoRecurso.ALIMENTO, "Pan", "u", 5000, null);
            s.registrarRecurso("G", pan);
            AsignacionRecurso asig = new AsignacionRecurso(s);
            long t0 = System.nanoTime();
            asig.asignar(g, pan, 1500);
            asig.asignar(g, pan, 1500); // la 2.ª ronda además consulta el historial
            double segundos = (System.nanoTime() - t0) / 1e9;
            verificar(segundos < 5.0, "tardó " + segundos + " s");
        });

        System.out.println("\n=== RESULTADO: " + exitosas + " exitosas, " + fallidas + " fallidas ===");
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static boolean tieneAlerta(List<Alerta> alertas, String codigo, NivelSemaforo gravedad) {
        return alertas.stream().anyMatch(a -> a.codigo().equals(codigo) && a.gravedad() == gravedad);
    }
}
