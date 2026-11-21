# Shelter4Casualties2
Algorithms II Final Project - 4th Semester - Software Engineering at Universidad Nacional Mayor de San Marcos

Sistema en Java (POO) para centralizar la gestión de albergues temporales durante emergencias en el Perú:
controla la ocupación, el registro de damnificados y el inventario de recursos críticos, priorizando a las
poblaciones vulnerables y alertando con un semáforo cuando un albergue incumple los mínimos humanitarios
(Manual Sphere 2018). Tiene una interfaz gráfica Swing tipo "Centro de Comando" (por defecto) y un menú de
consola.

## Requisitos
- Java 17 o superior (no se usan librerías externas: solo Swing/Java2D del JDK).

## Estructura (MVC)
```
src/com/shelter/
  Main.java                  Punto de entrada: sin argumentos abre la GUI; con --consola, el menú de consola
  model/                     Albergue, Damnificado (+ General, Menor, Vulnerable), Recurso, Distribuible,
                             TipoRecurso, RegistroAsignacion, NivelPrioridad, NivelSemaforo, Alerta
  service/                   AlbergueService (reglas de negocio), AsignacionRecurso (reparto), FiltroAlbergue
  controller/                ShelterController: único punto por el que la GUI modifica datos; avisa a las vistas
  config/SphereConstantes    Mínimos del Manual Sphere
  persistence/               Json (lector/escritor) y PersistenciaJson (guardar/cargar)
  ui/VentanaPrincipal        Ventana: encabezado, navegación lateral y las 4 vistas
  ui/vistas/                 PanelTablero, PanelDamnificados, PanelRecursos, PanelHistorial, Encabezado
  ui/componentes/            SemaforoComponente (Graphics2D animado), Tarjeta, BotonModerno, BarraNivel,
                             TarjetaRecurso, Iconos (vectoriales), Renderizadores, Formularios
  ui/tema/Tema               Paleta Dark Slate y colores por recurso / semáforo / prioridad
test/com/shelter/            PruebasShelter (pruebas de la lógica, sin dependencias)
Clases.md, CasosDeUso.md     Diagramas (Mermaid)
MinimosSphere.md             Tabla de valores Sphere usados
GUIA_DE_CAMBIOS.md           Qué se cambió y por qué
```

## Compilar y ejecutar
Desde la carpeta del proyecto:
```
javac --release 17 -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out com.shelter.Main              # interfaz gráfica
java -cp out com.shelter.Main --consola    # menú de consola
```
En Windows (PowerShell):
```
$f = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName
javac --release 17 -encoding UTF-8 -d out $f
java -cp out com.shelter.Main
```
En NetBeans basta con abrir la carpeta `src` como fuentes y ejecutar `com.shelter.Main`
(configure la codificación del proyecto en UTF-8). La GUI también se puede abrir directamente con
`com.shelter.ui.VentanaPrincipal`.

## Pruebas
```
javac --release 17 -encoding UTF-8 -d out $(find src test -name '*.java')
java -cp out com.shelter.PruebasShelter
```
Termina con código 1 si alguna prueba falla.

## Datos guardados
Al iniciar, si existe `datos_shelter.json` en la carpeta de ejecución se carga; si no, se crean dos albergues de
prueba (A1 y A2). Al cerrar la ventana (o salir con la opción 0 en consola) se guarda automáticamente; si el
guardado falla, la ventana avisa y pregunta antes de cerrar. Un archivo dañado nunca borra los datos en memoria.

## Interfaz gráfica
- **Tablero**: semáforo animado del albergue seleccionado, tabla de la red con filtros en vivo, mosaicos de
  recursos por tipo y detalle de alertas, damnificados e inventario.
- **Damnificados**: ingreso con vista previa de la prioridad, traslado y salida eligiendo a la persona de una
  lista (sin escribir códigos a mano) y ubicación por DNI con verificación post-atención.
- **Recursos**: registro y modificación de lotes, inventario y reparto priorizado con la cola de prioridad.
- **Historial**: entregas con búsqueda, guardar / exportar / cargar JSON.
- **Colores por recurso**: Agua azul/cian, Alimentos esmeralda/naranja, Medicina coral/blanco clínico,
  Abrigo ámbar/amarillo, Higiene lavanda.

## Reglas principales
- **Prioridad** (1 más urgente a 4): atención médica = CRÍTICA; gestante o niño de 5 años o menos o adulto mayor
  (60+) = ALTA; vulnerable en el resto = MEDIA (nunca baja); menor sin tutor = ALTA; el resto = BAJA.
- **Identidad**: cada damnificado recibe al ingresar un código alfanumérico ordenado y dependiente del refugio:
  `SANMI-00001`, `SANMI-00002`, etc. El DNI y el nombre completo se conservan como datos secundarios para
  detectar duplicados y validar registros. Al trasladarse, el código cambia al prefijo del nuevo refugio.
  Dos refugios no pueden compartir prefijo y un código nunca se reutiliza (el contador se guarda en el JSON).
- **Semáforo** = la gravedad más alta entre: ocupación (amarillo desde 70 %, rojo desde 90 %), agua
  (menos de 15 L/persona amarillo; menos de 7.5 rojo), espacio vital (menos de 3.5 m²/persona rojo),
  recursos críticos sin stock con gente alojada (rojo) y lotes vencidos (amarillo).
- **Reparto**: primero por nivel de prioridad y, a igual nivel, quien lleva más tiempo sin recibir ese recurso.
  La medicina solo se entrega a quien la necesita. Al agua le corresponden 15 L por persona por defecto.
