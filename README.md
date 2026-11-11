# Shelter4Casualties2
Algorithms II Final Project - 4th Semester - Software Engineering at Universidad Nacional Mayor de San Marcos

Sistema en Java (POO) para centralizar la gestión de albergues temporales durante emergencias en el Perú:
controla la ocupación, el registro de damnificados y el inventario de recursos críticos, priorizando a las
poblaciones vulnerables y alertando con un semáforo cuando un albergue incumple los mínimos humanitarios
(Manual Sphere 2018). Hoy funciona por consola; la interfaz gráfica en Swing es el siguiente paso.

## Requisitos
- Java 17 o superior (no se usan librerías externas).

## Estructura
```
src/com/shelter/
  Main.java                  Menú de consola (solo presentación)
  model/                     Albergue, Damnificado (+ General, Menor, Vulnerable), Recurso, Distribuible,
                             TipoRecurso, RegistroAsignacion, NivelPrioridad, NivelSemaforo, Alerta
  service/                   AlbergueService (reglas de negocio), AsignacionRecurso (reparto), FiltroAlbergue
  config/SphereConstantes    Mínimos del Manual Sphere
  persistence/               Json (lector/escritor) y PersistenciaJson (guardar/cargar)
test/com/shelter/            PruebasShelter (pruebas de la lógica, sin dependencias)
Clases.md, CasosDeUso.md     Diagramas (Mermaid)
MinimosSphere.md             Tabla de valores Sphere usados
GUIA_DE_CAMBIOS.md           Qué se cambió, por qué y cómo usarlo desde Swing
```

## Compilar y ejecutar
Desde la carpeta del proyecto:
```
javac --release 17 -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out com.shelter.Main
```
En Windows (PowerShell):
```
$f = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName
javac --release 17 -encoding UTF-8 -d out $f
java -cp out com.shelter.Main
```
En NetBeans basta con abrir la carpeta `src` como fuentes y ejecutar `com.shelter.Main`
(configure la codificación del proyecto en UTF-8).

## Pruebas
```
javac --release 17 -encoding UTF-8 -d out $(find src test -name '*.java')
java -cp out com.shelter.PruebasShelter
```
Termina con código 1 si alguna prueba falla.

## Datos guardados
Al iniciar, si existe `datos_shelter.json` en la carpeta de ejecución se carga; si no, se crean dos albergues de
prueba (A1 y A2). Al salir (opción 0) se guarda automáticamente. También se puede guardar y cargar desde el
módulo 4 del menú. Un archivo dañado nunca borra los datos que están en memoria.

## Reglas principales
- **Prioridad** (1 más urgente a 4): atención médica = CRÍTICA; gestante o niño de 5 años o menos o adulto mayor
  (60+) = ALTA; vulnerable en el resto = MEDIA (nunca baja); menor sin tutor = ALTA; el resto = BAJA.
- **Semáforo** = la gravedad más alta entre: ocupación (amarillo desde 70 %, rojo desde 90 %), agua
  (menos de 15 L/persona amarillo; menos de 7.5 rojo), espacio vital (menos de 3.5 m²/persona rojo),
  recursos críticos sin stock con gente alojada (rojo) y lotes vencidos (amarillo).
- **Reparto**: primero por nivel de prioridad y, a igual nivel, quien lleva más tiempo sin recibir ese recurso.
  La medicina solo se entrega a quien la necesita. Al agua le corresponden 15 L por persona por defecto.
