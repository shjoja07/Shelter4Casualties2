# Diagrama de casos de uso

```mermaid
flowchart LR
    %% Actor
    Encargado["Encargado<br/>de Albergue"]

    %% Límite del sistema
    subgraph Sistema["Sistema Shelter 4 Casualties 2"]
        direction TB

        RF01(["RF-01 Registrar nuevo albergue"])
        RF03(["RF-03 Actualizar inventario de recursos"])
        RF06(["RF-06 Validar mínimos Sphere y alertas"])
        RF02(["RF-02 Registrar ingreso de damnificado"])
        RF04(["RF-04 Asignar recurso escaso por prioridad"])
        RF07(["RF-07 Filtrar albergues"])
        RF05(["RF-05 Consultar panel semáforo de la red"])
        RF08(["RF-08 Registrar salida o reubicación"])

        UC_CalcularPrioridad(["Calcular prioridad<br/>de asignación"])
        RF09(["RF-09 Consultar disponibilidad<br/>de otros albergues"])
    end

    %% Asociaciones del actor
    Encargado --- RF01
    Encargado --- RF03
    Encargado --- RF06
    Encargado --- RF02
    Encargado --- RF04
    Encargado --- RF07
    Encargado --- RF05
    Encargado --- RF08

    %% Relaciones entre casos de uso
    RF02 -.->|«include»| UC_CalcularPrioridad
    RF04 -.->|«include»| UC_CalcularPrioridad

    RF07 -.->|«extend»| RF05

    RF05 -.->|«include»| RF09
    RF08 -.->|«include»| RF09

    %% Nota del actor
    Nota["Actor único: personal designado<br/>(el sistema no es de uso público)."]
    Nota --- Encargado

    %% Estilos visuales
    classDef actor fill:#ffffff,stroke:#333333,color:#111111
    classDef casoUso fill:#eaf2f8,stroke:#2874a6,color:#17202a
    classDef nota fill:#fff4cc,stroke:#b7950b,color:#333333

    class Encargado actor
    class RF01,RF02,RF03,RF04,RF05,RF06,RF07,RF08,RF09,UC_CalcularPrioridad casoUso
    class Nota nota
```
