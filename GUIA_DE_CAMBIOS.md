# Guía de cambios — Shelter4Casualties2 (versión previa a Swing)

Esta guía acompaña al proyecto corregido. Explica qué se cambió en cada uno de los 7 pasos, qué decisiones
de diseño necesitan confirmación del grupo, cómo conectar la interfaz Swing y qué queda pendiente.

## 1. Verificación realizada

| Qué | Resultado |
|---|---|
| Compilación con `javac --release 17 -Xlint:all` | Sin errores ni advertencias |
| `PruebasShelter` (41 pruebas) | 41 exitosas, 0 fallidas |
| Prueba de humo de `Main` con entradas inválidas (letras, DNI corto, fecha mala, IDs inexistentes), albergue lleno con derivación, derivar, salida, inventario, reparto, Sphere, guardar y reinicio con carga automática | Sin caídas; el estado se recuperó al reiniciar |

Nota de honestidad: las pruebas se ejecutaron en un JDK 21 compilando con `--release 17`. Eso garantiza que
no se usa ninguna API posterior a Java 17, pero no se ejecutó en un runtime 17 real. No se probó Swing
(todavía no existe) ni NetBeans.

## 2. Cómo integrarlo al repositorio

**Reemplazar** (misma ruta):
`Main`, `model/Albergue`, `model/Damnificado`, `model/DamnificadoGeneral`, `model/DamnificadoMenor`,
`model/DamnificadoVulnerable`, `service/AlbergueService`, `service/AsignacionRecurso`,
`persistence/PersistenciaJson`, `Clases.md`, `CasosDeUso.md`, `README.md`.

**Agregar** (nuevos): `model/NivelPrioridad`, `model/NivelSemaforo`, `model/Alerta`,
`service/FiltroAlbergue`, `test/com/shelter/PruebasShelter`, este archivo.

**Sin cambios** (puedes dejar los tuyos): `model/Recurso`, `model/Distribuible`, `model/TipoRecurso`,
`model/RegistroAsignacion`, `config/SphereConstantes`, `persistence/Json`, `MinimosSphere.md`, `manifest.mf`.

**Eliminar del repo**: `SphereConstantes_1.java` y la copia de `SphereConstantes.java` que está fuera de
`src/com/shelter/config/`. Dos copias de la misma clase impiden compilar.

Si alguien del grupo tiene código propio que llame a las firmas antiguas, ver la tabla de la sección 5.

## 3. Qué se hizo en cada paso

### Paso 1 — Bugs y validaciones
- **Derivación (RF-08)**: ahora retira al damnificado del origen y lo aloja en el destino; ambas ocupaciones
  y semáforos quedan correctos. Recibe el DNI, no un objeto suelto. Casos de error distinguidos por enum.
- **Reparto (RF-04)**: la entrega es `min(cantidad por persona, restante, stock)`. Se acabó la excepción
  "Stock insuficiente" con stock fraccionario.
- **Entrada de datos**: toda la lectura valida y repite la pregunta. Sin `nextInt()`/`nextBoolean()` directos.
  Se corrigió la doble pregunta de capacidad y el `Enter` extra. Una fecha mal escrita se vuelve a pedir
  (antes quedaba como "no vence"). Los decimales aceptan coma o punto.
- **Integridad**: ID de albergue único (sin distinguir mayúsculas), capacidad > 0, edad 0–120, DNI y nombre
  no vacíos, menor = menos de 18 años, un mismo DNI no puede estar en dos lugares de la red.
- **Constructor de `DamnificadoMenor`**: ya guarda nombre del tutor y parentesco (antes quedaban `null`).

### Paso 2 — Prioridad unificada
- Una sola fuente de verdad: `Damnificado.getNivelPrioridad()` (enum `NivelPrioridad`: CRÍTICA, ALTA,
  MEDIA, BAJA). Cada subclase lo decide por polimorfismo; `AlbergueService` ya no usa `instanceof`,
  así que agregar un subtipo no obliga a tocar el servicio (RNF-03).
- `atencionMedica` pasó a la clase base. Antes se pedía a todos y solo el Vulnerable la guardaba.
- El vulnerable nunca es BAJA y el texto de `determinarNivelPrioridad()` se construye desde el nivel real,
  por lo que no puede contradecirlo.
- RF-04 considera el tiempo desde la última entrega: a igual nivel recibe antes quien lleva más tiempo sin
  recibir **ese recurso**. Se calcula desde el historial, que ya se guardaba.

### Paso 3 — `Albergue` encapsulado
- La ocupación se deriva de la lista de damnificados (se eliminó `setOcupacionActual`).
- Listas de solo lectura; se modifica con `alojar`, `retirar`, `registrarRecurso`.
- Nuevos: `tipoInfraestructura`, `areaM2`, `getPorcentajeOcupacion`, `buscarDamnificado`,
  `getCantidadTotal`, `tieneRecursoDisponible`.
- `registrarRecurso` suma al lote existente si es idéntico (mismo tipo, nombre, unidad y vencimiento).
- Se limpiaron los comentarios de trabajo ("Atte: Adrian", "Lo mismo", "parche").

### Paso 4 — Semáforo, Sphere y filtros
- `NivelSemaforo` (enum) calculado por `AlbergueService` y actualizado automáticamente en cada operación que
  lo afecta (ingreso, derivación, salida, inventario, reparto, carga).
- `validarMinimosSphere` devuelve `List<Alerta>` (gravedad + código + mensaje): agua, espacio vital y
  capacidad declarada frente al área. `evaluarRecursos` agrega recursos críticos sin stock y lotes vencidos.
- `filtrarAlbergues(FiltroAlbergue)`: cupos mínimos, distrito, nivel de alerta, recurso faltante (RF-07).
- `SphereConstantes` ahora se usa (agua, espacio vital y capacidad por área).

### Paso 5 — Menús completos y persistencia
- Completados: filtrar (RF-07), derivar y salida (RF-08), validar Sphere (RF-06), actualizar inventario
  (RF-03), consultar un albergue en detalle (RF-09), persistencia JSON.
- Si no hay cupo al registrar, se ofrece registrar en otro albergue con cupo (lo que prometía el avance 2).
- Al iniciar se carga `datos_shelter.json` si existe; al salir se guarda. Al salir con la consola cerrada
  también se guarda.
- `PersistenciaJson` guarda y lee los campos nuevos como **opcionales** (los archivos de la versión
  anterior siguen cargando), valida que la ocupación coincida con las personas y rechaza IDs o DNI repetidos
  sin tocar el estado actual.

### Paso 6 — Servicios sin `println`
- `AlbergueService` y `AsignacionRecurso` ya no imprimen. Devuelven booleanos, enums, listas u `Optional`, o
  lanzan `IllegalArgumentException` por datos inválidos. Los modelos tampoco imprimen
  (`confirmacionAtencion` y `estadoPostAtencion` devuelven texto y guardan estado).

### Paso 7 — Documentación y pruebas
- `Clases.md`, `CasosDeUso.md` y `README.md` actualizados al código real.
- `PruebasShelter`: 41 pruebas sin dependencias externas (corren con solo Java). Cubren validaciones,
  prioridad, ingreso/derivación/salida, reparto (incluido stock fraccionario y desempate), semáforo y Sphere,
  filtros, JSON (ida y vuelta, archivo dañado, formato antiguo, inconsistencias) y rendimiento con 2000
  ocupantes (RNF-02).

## 4. Decisiones que el grupo debe confirmar

Estas decisiones las tomé yo para que el sistema fuera coherente. Ninguna viene de una norma citada.

1. **Cuatro niveles de prioridad** (antes un 1/2). El avance 2 habla de "Prioridad 1 (Alta) / 2 (Normal)".
2. **Menor sin tutor = ALTA; menor acompañado = BAJA** (el avance 2 decía "baja" para todo menor). Si el
   grupo prefiere BAJA siempre, es una línea en `DamnificadoMenor.getNivelPrioridad()`.
3. **El semáforo se pone ROJO si hay gente alojada y falta stock vigente de ALIMENTO o MEDICINA** (el agua se
   evalúa aparte). Consecuencia visible: un albergue con una sola persona y sin medicina aparece ROJO. Es
   consistente con RF-05 ("recursos críticos"), pero si parece demasiado estricto se ajusta en
   `AlbergueService.evaluarRecursos` (por ejemplo, MEDICINA como AMARILLO).
4. **Umbrales**: ocupación amarillo desde 70 %, rojo desde 90 %; aviso amarillo cuando un mínimo Sphere está
   a menos de 15 % de incumplirse. Son valores propuestos, ajustables en las constantes de `AlbergueService`.
5. **Agua**: se divide el agua vigente almacenada entre los ocupantes y se compara con 15 L/persona/día
   (equivale a "un día de cobertura"); menos de 7.5 L/persona es ROJO. El agua debe registrarse en litros.
6. **DNI**: el formato de 8 dígitos se valida en la capa de entrada (`Damnificado.esDniValido`), no en el
   modelo, para no rechazar archivos viejos ni a personas con otro documento. Decidir si Swing exige 8 dígitos.
7. **Menor = menos de 18 años**, pero `CategoriaEdad.NINO` sigue siendo hasta 12 años y "niño pequeño" (prioridad
   alta) hasta 5. Los adolescentes de 13 a 17 se registran como Menor pero su categoría por edad es JOVEN.
8. **La medicina solo se reparte** a quien requiere atención médica, es gestante o tiene enfermedad crónica.
9. **Un mismo DNI no puede estar en dos albergues** (se rechaza el ingreso).
10. **`ultimaAsignacion` no se agregó a `Damnificado`**: se lee del historial por recurso (ya persistido). El
    diagrama de clases se actualizó en consecuencia.

## 5. Cambios de firma (por si alguien tiene código que los llama)

| Antes | Ahora |
|---|---|
| `registrarIngreso(String, Damnificado): boolean` | `ResultadoIngreso` (INGRESADO, ALBERGUE_NO_ENCONTRADO, SIN_CUPO, DNI_DUPLICADO) |
| `derivarDamnificado(String, String, Damnificado): boolean` | `derivarDamnificado(String, String, String dni): ResultadoDerivacion` |
| `registrarAlbergue(Albergue): void` | `boolean` (false si el ID ya existe) |
| `mostrarPanelSemaforo(): void` | `obtenerPanelSemaforo(): List<Albergue>` |
| `calcularPrioridadAsignacion(...)`: 1 o 2 | 1 a 4 (CRÍTICA=1 … BAJA=4) |
| `Albergue.setOcupacionActual(int)` | eliminado (la ocupación se deriva) |
| `Albergue.getEstadoSemaforo(): String` | `NivelSemaforo` |
| `Albergue.getDamnificados()/getRecursos()` mutables | solo lectura; usar `alojar`/`retirar`/`registrarRecurso` |
| `Damnificado.setDni` / `setNombreCompleto` | eliminados (identificación inmutable) |
| `DamnificadoVulnerable.confirmacionAtencion(boolean): void` | devuelve `String` y guarda el estado |
| `registrarRecurso(String, Recurso)` solo agregaba | además valida (no vencido, agua en litros) y puede lanzar `IllegalArgumentException` |

## 6. Cómo conectar Swing

| Pantalla / acción | Llamada |
|---|---|
| Tabla del panel semáforo | `servicio.obtenerPanelSemaforo()` → una fila por `Albergue` |
| Color de la fila | `albergue.getEstadoSemaforo()` (VERDE / AMARILLO / ROJO) |
| Barra de progreso | `albergue.getPorcentajeOcupacion()` (0–100) |
| Lista de alertas de un albergue | `servicio.obtenerAlertas(albergue)` → `List<Alerta>` |
| Filtros de la tabla (RF-07) | `servicio.filtrarAlbergues(new FiltroAlbergue(cupos, distrito, nivel, recursoFaltante))`; `null` = no filtrar |
| Registrar albergue | `new Albergue(id, nombre, distrito, tipo, capacidad, area)` + `servicio.registrarAlbergue(...)` |
| Registrar damnificado | crear `DamnificadoGeneral/Menor/Vulnerable` + `servicio.registrarIngreso(id, d)` |
| Derivar / salida | `servicio.derivarDamnificado(origen, destino, dni)` / `servicio.registrarSalida(id, dni)` |
| Inventario | `servicio.registrarRecurso(...)`, `servicio.actualizarRecurso(...)` |
| Repartir | `asignacion.asignar(albergue, recurso, total, porPersona)`; sugerido: `cantidadSugeridaPorPersona(recurso)` |
| Historial | `asignacion.getHistorial()` |
| Guardar / cargar | `PersistenciaJson.guardar(...)` / `cargar(...)` + `reemplazarAlbergues` + `reemplazarHistorial` |

Pautas:
- **Una sola instancia** de `AlbergueService` y de `AsignacionRecurso` para toda la aplicación (hoy están
  como campos estáticos de `Main`). Pasarlas a las ventanas por constructor.
- **Errores**: los constructores de modelo, `asignar` y `registrarRecurso` lanzan `IllegalArgumentException`
  con un mensaje listo para mostrar en un diálogo. Los enums de resultado se traducen a mensajes.
- **Refrescar la tabla** después de cada operación. El modelo todavía no notifica cambios (no hay
  listeners); si el grupo quiere refresco automático, es una mejora posterior.
- **No modificar el modelo directamente** desde la interfaz: pasar siempre por el servicio, que es quien
  recalcula el semáforo.
- **Hilos**: nada es seguro entre hilos. Usar todo desde el hilo de Swing (EDT); si guardar/cargar se lleva
  a un `SwingWorker`, no tocar los servicios mientras corre.
- **Validación de formularios**: reutilizar `Damnificado.esDniValido`, `Damnificado.EDAD_MAXIMA`,
  `Damnificado.EDAD_MAYORIA` y los mensajes de las excepciones.
- **Accesibilidad del semáforo (RNF-01)**: mostrar también el texto (VERDE/AMARILLO/ROJO) y no solo el color.
- La lógica de arranque (`Main.iniciarDatos`) y cierre (`guardarEn`) conviene extraerla a una clase pequeña
  cuando se haga la ventana principal, para que la usen la consola y Swing.

## 7. Limitaciones y pendientes (lo que NO se resolvió)

- **RNF-07 (sincronización con base de datos central)**: no está cubierto. Solo hay persistencia local en
  JSON. Hay que reformular el requisito como "persistencia local en archivo" o dejarlo como mejora futura.
- **Constantes Sphere sin uso**: grifos, bombas, pozos, inodoros, distancias, tiempos de cola, altura de techo y
  asentamiento no se evalúan porque el sistema no captura esos datos. Solo se evalúan agua y espacio vital
  (lo que exige RF-06).
- **Presentación del avance 2 (PDF)**: describe un menú de 4 opciones con "salir" en la 4, una prioridad de dos
  niveles y un modelo anterior. Conviene actualizar esas diapositivas con la sección 4.
- **RF-02 del avance 1** menciona subtipos `AdultoMayor`, `NinoNoAcompanado`, `PersonaConDiscapacidad`; el
  código implementa General, Menor y Vulnerable. Alinear el texto del requisito.
- **Pruebas**: no usan JUnit a propósito (sin dependencias). Migrarlas a JUnit es directo si el docente lo pide.
- **Decimales**: el stock usa `double`. Para cantidades enormes o muchas operaciones podrían acumularse
  errores diminutos (el reparto ya usa una tolerancia).
- **Sin autenticación ni concurrencia**: el sistema es de un solo usuario en un solo hilo.
- **Persistencia**: se guarda el estado completo en cada guardado (suficiente para el tamaño del proyecto).
  No hay guardado automático tras cada operación, solo al salir o desde el menú.
