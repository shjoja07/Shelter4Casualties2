@startuml
left to right direction
skinparam packageStyle rectangle
actor "Encargado\nde Albergue" as Encargado
rectangle "Sistema Shelter 4 Casualties 2" {
usecase "RF-01 Registrar nuevo albergue" as RF01
usecase "RF-03 Actualizar inventario de recursos" as RF03
usecase "RF-06 Validar mínimos Sphere y alertas" as RF06
usecase "RF-02 Registrar ingreso de damnificado" as RF02
usecase "RF-04 Asignar recurso escaso por prioridad" as RF04
usecase "RF-07 Filtrar albergues" as RF07
usecase "RF-05 Consultar panel semáforo de la red" as RF05
usecase "RF-08 Registrar salida o reubicación" as RF08
usecase "Calcular prioridad\nde asignación" as UC_CalcularPrioridad
usecase "RF-09 Consultar disponibilidad\nde otros albergues" as RF09
}
Encargado --> RF01
Encargado --> RF03
Encargado --> RF06
Encargado --> RF02
Encargado --> RF04
Encargado --> RF07
Encargado --> RF05
Encargado --> RF08
RF02 .> UC_CalcularPrioridad : «include»
RF04 .> UC_CalcularPrioridad : «include»
RF07 .> RF05 : «extend»
RF05 .> RF09 : «include»
RF08 .> RF09 : «include»
note bottom of Encargado
Actor único: personal designado
(el sistema no es de uso público).
end note
@enduml