# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop (Java 17 + Swing, sin librerías externas). No es web, iOS ni Android.

## Users
Encargado de albergue: personal que trabaja dentro de un albergue temporal durante una emergencia en el Perú.
Registra ingresos y salidas de damnificados, deriva personas a otros albergues cuando no hay cupo, registra
lotes de recursos y reparte recursos escasos según prioridad. Trabaja bajo presión y necesita saber de un
vistazo si su albergue incumple algún mínimo humanitario.

## Product Purpose
Centralizar la gestión de albergues temporales: ocupación, registro de damnificados e inventario de recursos
críticos, priorizando a las poblaciones vulnerables y alertando con un semáforo (verde, amarillo, rojo) cuando un
albergue incumple los mínimos del Manual Sphere 2018. El éxito, ahora mismo, es una entrega final convincente del
curso Algoritmos II (UNMSM, 4.º ciclo de Ingeniería de Software), evaluada por docentes y presentada en clase.

## Positioning
Semáforo calculado con los mínimos Sphere reales (agua en L/persona/día, espacio vital en m²/persona, recursos
críticos y lotes vencidos) y reparto de recursos priorizado por vulnerabilidad y por el tiempo desde la última entrega.

## Operating Context
- Proyecto de curso: lo evalúan docentes y se presenta en clase; se trabaja en grupo con GitHub (ramas y Pull Requests).
- Se ejecuta desde VS Code o NetBeans (`com.shelter.Main`); los datos viven en `datos_shelter.json`.
- Requerimientos con códigos RF-01 a RF-09 y RNF (ver CasosDeUso.md y Clases.md).

## Capabilities and Constraints
- Albergues (RF-01), ingreso de damnificados con prioridad (RF-02), inventario (RF-03), reparto priorizado (RF-04),
  panel semáforo (RF-05), validación Sphere (RF-06), filtros (RF-07), derivación y salida (RF-08), consulta de la red (RF-09).
- Prioridades: CRÍTICA, ALTA, MEDIA, BAJA. Tipos de damnificado: General, Menor, Vulnerable.
  Tipos de recurso: AGUA, ALIMENTO, MEDICINA (críticos), ABRIGO, HIGIENE.
- Códigos de damnificado por refugio (p. ej. SANMI-00001). El DNI y el nombre son datos secundarios.
- Restricción técnica: Java 17, Swing/Java2D puro, sin dependencias externas. También existe una interfaz de consola.
- RNF-01: el semáforo debe mostrar color y texto (no depender solo del color).

## Brand Commitments
Nombre: "Shelter 4 Casualties". Idioma de la interfaz: español (Perú).

## Evidence on Hand
- Datos de ejemplo en `datos_shelter.json`. Mínimos Sphere en `MinimosSphere.md` y `config/SphereConstantes.java`.
- No hay usuarios reales, instituciones aliadas ni testimonios: no se deben inventar.

## Product Principles
1. El estado del albergue se entiende en segundos: lo urgente primero.
2. Las reglas humanitarias (Sphere, prioridad) son la fuente de verdad; la interfaz nunca las contradice.
3. Evitar errores de captura bajo presión: elegir de listas antes que escribir códigos a mano.
4. Ser una entrega académica sólida y honesta: no se muestran datos ni capacidades inventados.

## Accessibility & Inclusion
RNF-01: el color nunca es la única señal; cada estado del semáforo va acompañado de su etiqueta en texto.
