@startuml
skinparam classAttributeIconSize 0
class Main {
+ main(args: String[])
- opciones del menú 1 a 11
- leerTexto() / leerEntero() / leerDecimal() / leerSiNo()
}
class "SphereConstantes" <<utility>> {
+ 19 constantes (static final)
(mínimos Sphere 2018)
}
class AlbergueService {
- albergues: List<Albergue>
+ UMBRAL_AMARILLO, UMBRAL_ROJO: double
+ registrarAlbergue(Albergue): boolean <<RF-01>>
+ registrarIngreso(String, Damnificado): boolean <<RF-02>>
+ registrarRecurso(String, Recurso): boolean <<RF-03>>
+ mostrarPanelSemaforo(): void <<RF-05>>
+ actualizarSemaforo(Albergue): void
+ validarMinimosSphere(Albergue): List<String> <<RF-06>>
+ filtrarAlbergues(...): List<Albergue> <<RF-07>>
+ derivarDamnificado(String, String, Damnificado): boolean <<RF-08>>
+ registrarSalida(String, String): boolean <<RF-08>>
+ consultarDisponibilidad(): List<Albergue> <<RF-09>>
+ consultarAlbergue(String): void <<RF-09>>
+ calcularPrioridadAsignacion(Damnificado): int
+ buscarPorId(String): Optional<Albergue>
}
class AsignacionRecurso {
- albergueService: AlbergueService
- historial: List<RegistroAsignacion>
+ calcularOrdenPrioridad(Albergue): List<Damnificado>
+ asignar(Albergue, Recurso, double): List<RegistroAsignacion> <<RF-04>>
+ getHistorial(): List<RegistroAsignacion>
}
class RegistroAsignacion {
- idAlbergue, dni, nombreDamnificado: String
- recurso, unidad: String
- cantidad: double

- fechaHora: LocalDateTime
+ toString(): String
}
class Albergue {
- id, nombre, distrito: String
- tipoInfrastructura: String
- capacidadMaxima: int
- areaM2: double
- damnificados: List<Damnificado>
- recursos: List<Recurso>
- estadoSemaforo: String
+ tieneCupo(): boolean
+ getCuposDisponibles(): int
+ getOcupacionActual(): int
+ getPorcentajeOcupacion(): double
+ alojar(Damnificado): boolean
+ retirar(Damnificado): boolean
+ buscarDamnificado(String): Optional<Damnificado>
+ registrarRecurso(Recurso): void
+ getCantidadTotal(TipoRecurso): double
+ tieneRecursoDisponible(TipoRecurso): boolean
}
class Recurso {
- tipo: TipoRecurso
- nombre, unidad: String
- cantidadDisponible: double
- fechaVencimiento: LocalDate
+ descontar(double): void
+ actualizar(double, LocalDate): void
+ estaVencido(LocalDate): boolean
}
interface Distribuible <<interface>> {
+ getNombre(): String
+ getUnidad(): String
+ getCantidadDisponible(): double
+ getFechaVencimiento(): LocalDate
+ estaVencido(LocalDate): boolean
+ descontar(double): void
}
enum TipoRecurso <<enumeration>> {
AGUA, ALIMENTO, MEDICINA (críticos)
ABRIGO, HIGIENE
+ esCritico(): boolean
}

abstract class Damnificado <<abstract>> {
# dni, nombreCompleto: String
# edad: int
# categoria: CategoriaEdad
# ultimaAsignacion: LocalDateTime
+ asignarCategoriaPorEdad(int)
+ determinarNivelPrioridad(): String
+ getters / setters
}
enum CategoriaEdad <<enumeration>> {
NINO, JOVEN, ADULTO, ADULTO_MAYOR
}
class DamnificadoGeneral {
+ determinarNivelPrioridad()
}
class DamnificadoMener {
- dniTutor, nombreTutor: String
- parentesco: String
+ determinarNivelPrioridad()
}
class DamnificadoVulnerable {
- enfermedadCronica: boolean
- atencionMedica: boolean
- estadoGestacion: boolean
+ confirmacionAtencion(boolean)
+ estadoPostAtencion(boolean)
+ determinarNivelPrioridad()
}
' Relaciones
Main ..> AsignacionRecurso : usa
Main ..> AlbergueService : usa
SphereConstantes <.. AlbergueService : valores mínimos
AlbergueService "1" *-- "0..*" Albergue : gestiona
AlbergueService .> AsignacionRecurso : consulta prioridad
AsignacionRecurso ..> RegistroAsignacion : crea
Albergue "1" *-- "0..*" Recurso : inventario
Albergue "1" o-- "0..*" Damnificado : aloja
Recurso ..|> Distribuible

Recurso --> TipoRecurso
Damnificado <|-- DamnificadoGeneral
Damnificado <|-- DamnificadoMener
Damnificado <|-- DamnificadoVulnerable
Damnificado --> CategoriaEdad
@enduml