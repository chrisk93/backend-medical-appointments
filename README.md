# MediSalud — API de Agendamiento de Citas Médicas

API REST para registrar médicos y pacientes, consultar disponibilidad, reservar y cancelar citas médicas aplicando reglas de horario, no duplicidad y penalización por cancelación tardía.

Proyecto construido con **Spec-Driven Development**: la especificación, el diseño y el plan de tareas están en [`docs/`](docs/) y el código se implementó tarea por tarea a partir de ellos.

---

## Tabla de contenido

1. [Stack tecnológico](#stack-tecnológico)
2. [Requisitos](#requisitos)
3. [Ejecución local](#ejecución-local)
4. [Pruebas y cobertura](#pruebas-y-cobertura)
5. [API](#api)
6. [Reglas de negocio](#reglas-de-negocio)
7. [Manejo de errores](#manejo-de-errores)
8. [Arquitectura](#arquitectura)
9. [Estructura del proyecto](#estructura-del-proyecto)
10. [Modelo de datos](#modelo-de-datos)
11. [Seguridad](#seguridad)
12. [Decisiones y supuestos](#decisiones-y-supuestos)
13. [Documentación](#documentación)

---

## Stack tecnológico

| Elemento | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Bean Validation) |
| Persistencia | Hibernate 7 + H2 en memoria |
| Build | Maven (Maven Wrapper incluido) |
| Utilidades | Lombok |
| Pruebas | JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest` |
| Cobertura | JaCoCo 0.8.15 |

---

## Requisitos

- **Java 17** o superior (`java -version`).
- No se necesita instalar Maven (se usa `./mvnw`) ni una base de datos (H2 corre en memoria dentro de la app).

---

## Ejecución local

### Opción 1 — Modo desarrollo

```bash
./mvnw spring-boot:run
```

En Windows: `mvnw.cmd spring-boot:run`

### Opción 2 — Como en producción (jar)

```bash
./mvnw clean verify                         # compila, ejecuta las pruebas, verifica cobertura y genera el jar
java -jar target/prueba-0.0.1-SNAPSHOT.jar
```

Para generar el jar sin ejecutar pruebas: `./mvnw clean package -DskipTests`.

La API queda disponible en **`http://localhost:8080`** cuando el log muestra:

```
Tomcat started on port 8080 (http) with context path '/'
Started PruebaApplication in X seconds
```

Detener con `Ctrl + C`.

### Notas

- **Base de datos en memoria:** los datos se pierden al reiniciar. Al arrancar siempre se cargan 3 médicos (IDs 1, 2 y 3).
- **Otro puerto:** `java -jar target/prueba-0.0.1-SNAPSHOT.jar --server.port=8081`
- **Usar siempre `clean`** (`./mvnw clean verify`): si el IDE (p. ej. VS Code con la extensión Java) compila en `target/classes` al mismo tiempo que Maven, pueden quedar clases desactualizadas.

---

## Pruebas y cobertura

```bash
./mvnw clean test      # solo pruebas
./mvnw clean verify    # pruebas + reporte + verificación de cobertura mínima
```

- **239 pruebas** (unitarias, de controlador, de persistencia y de integración).
- Cobertura actual: **100 % de líneas** y **98,6 % de ramas**.
- El build **falla** si la cobertura de líneas baja de **80 %** global o de **90 %** en `service.impl` y `domain.policy`.
- Reporte HTML: `target/site/jacoco/index.html`.

| Nivel | Herramienta | Qué valida |
|---|---|---|
| Reglas puras | JUnit + AssertJ | Horario, franjas y penalizaciones, sin Spring |
| Servicios | Mockito + reloj fijo | Cada regla de negocio y su excepción |
| Controladores | `@WebMvcTest` + MockMvc | Rutas, validación, códigos HTTP y formato de error |
| Persistencia | `@DataJpaTest` (H2) | Consultas, filtros y restricciones de la BD |
| Integración | `@SpringBootTest` + MockMvc | Flujos completos contra la BD real |

---

## API

Base URL: `http://localhost:8080/api`. Todas las fechas usan **ISO 8601**: `yyyy-MM-dd` y `yyyy-MM-ddTHH:mm:ss`.

Los ejemplos usan **curl** y se pueden copiar y ejecutar en una terminal con la app corriendo (macOS, Linux o Git Bash en Windows). En PowerShell usa `curl.exe` y escribe cada comando en una sola línea, sin las `\` del final. También se pueden importar en Postman: *Import → Raw text* y pegar el comando.

| RF | Método | Ruta | Éxito | Errores |
|---|---|---|---|---|
| RF-01 Registrar médico | `POST` | `/api/doctors` | 201 | 400 |
| RF-02 Registrar paciente | `POST` | `/api/patients` | 201 | 400, 409 |
| RF-03 Reservar cita | `POST` | `/api/appointments` | 201 | 400, 404, 409 |
| RF-04 Disponibilidad | `GET` | `/api/doctors/{doctorId}/availability?startDate=&endDate=` | 200 | 400, 404 |
| RF-05 Cancelar cita | `POST` | `/api/appointments/{id}/cancellation` | 200 | 400, 404, 409 |
| RF-06 Listar citas | `GET` | `/api/appointments?doctorId=&patientId=&status=&startDate=&endDate=` | 200 | 400 |

### RF-01 — Registrar médico

```bash
curl -X POST http://localhost:8080/api/doctors \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Dr. Luis Pérez", "specialty": "Neurología", "phone": "555-1004", "email": "luis.perez@medisalud.com"}'
```

`fullName` (3-100) y `specialty` obligatorios; `phone` (≥ 7 dígitos) y `email` opcionales.

```json
201 Created
{ "id": 4, "fullName": "Dr. Luis Pérez", "specialty": "Neurología", "phone": "555-1004", "email": "luis.perez@medisalud.com" }
```

### RF-02 — Registrar paciente

```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Ana Torres", "documentNumber": "1020304050", "phone": "3001234567", "email": "ana.torres@mail.com", "birthDate": "1990-05-20"}'
```

```json
201 Created
{ "id": 1, "fullName": "Ana Torres", "documentNumber": "1020304050", "phone": "3001234567", "email": "ana.torres@mail.com", "birthDate": "1990-05-20" }
```

Todos obligatorios excepto `birthDate` (opcional, no futura). `documentNumber` es único (≥ 7 caracteres): un duplicado responde **409**.

### RF-03 — Reservar cita

```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientId": 1, "doctorId": 1, "dateTime": "2026-10-26T10:00:00"}'
```

```json
201 Created
{ "id": 1, "patientId": 1, "doctorId": 1, "dateTime": "2026-10-26T10:00:00", "status": "SCHEDULED", "cancelledAt": null }
```

### RF-04 — Consultar disponibilidad

```bash
curl "http://localhost:8080/api/doctors/1/availability?startDate=2026-10-26&endDate=2026-10-31"
```

```json
200 OK
[ { "start": "2026-10-26T08:00:00", "end": "2026-10-26T08:30:00" }, ... ]
```

Ambas fechas son obligatorias e inclusivas. Excluye franjas ocupadas y franjas que ya comenzaron.

### RF-05 — Cancelar cita

```bash
curl -X POST http://localhost:8080/api/appointments/1/cancellation
```

```json
200 OK
{ "id": 1, "patientId": 1, "doctorId": 1, "dateTime": "2026-10-26T10:00:00", "status": "CANCELLED", "cancelledAt": "2026-10-25T09:12:44" }
```

### RF-06 — Listar citas

```bash
curl "http://localhost:8080/api/appointments?doctorId=1&status=SCHEDULED&startDate=2026-10-26&endDate=2026-10-31"
```

```json
200 OK
[ { "id": 1, "patientId": 1, "doctorId": 1, "dateTime": "2026-10-26T10:00:00", "status": "SCHEDULED", "cancelledAt": null } ]
```

Todos los filtros son opcionales y combinables. Resultado ordenado por fecha/hora ascendente. `status`: `SCHEDULED` o `CANCELLED`.

### Datos iniciales

| ID | Nombre | Especialidad | Teléfono | Email |
|---|---|---|---|---|
| 1 | Dra. María González | Cardiología | 555-1001 | maria.gonzalez@medisalud.com |
| 2 | Dr. Carlos Ruiz | Pediatría | 555-1002 | carlos.ruiz@medisalud.com |
| 3 | Dra. Ana López | Dermatología | 555-1003 | ana.lopez@medisalud.com |

---

## Reglas de negocio

| Regla | Comportamiento | HTTP |
|---|---|---|
| **RN-01** Horario | Lun-Vie 08:00-18:00 (20 franjas), Sáb 08:00-13:00 (10 franjas), Dom sin atención. Franjas de 30 min. | 400 |
| **RN-02** No duplicidad | Un médico no puede tener dos citas programadas en la misma franja. | 409 |
| **RN-03** Edad mínima | La fecha de nacimiento no puede ser futura; si no existe se asume edad 0 (válido). | 400 |
| **RN-04** Conflicto de paciente | Un paciente no puede tener dos citas programadas a la misma hora, con ningún médico. | 409 |
| **RN-05** Penalización | Cancelar con **menos** de 2 h de antelación genera una penalización. Con ≥ 3 penalizaciones en los últimos 30 días el paciente no puede agendar. | 409 |
| **RN-06** Validaciones | No se agenda en el pasado ni fuera del inicio de una franja (400). Solo se cancelan citas `SCHEDULED` (409). | 400 / 409 |

Orden de validación al reservar: existencia de paciente/médico (404) → fecha, franja y fecha de nacimiento (400) → penalizaciones, franja del médico y del paciente (409).

---

## Manejo de errores

Todas las respuestas de error usan el formato estándar **`ProblemDetail` (RFC 9457)**, con `Content-Type: application/problem+json`:

```json
{ "type": "about:blank", "title": "Conflict", "status": 409, "detail": "The doctor already has an appointment in this time slot." }
```

Errores de validación incluyen la lista de campos:

```json
{
  "title": "Bad Request", "status": 400, "detail": "Invalid request content.",
  "errors": [ { "field": "fullName", "message": "size must be between 3 and 100" } ]
}
```

| Causa | HTTP |
|---|---|
| Body inválido, JSON mal formado, fecha no ISO 8601, parámetro con tipo inválido o faltante, regla de negocio de formato | 400 |
| Recurso inexistente (médico, paciente, cita) | 404 |
| Conflicto (franja ocupada, documento duplicado, paciente bloqueado, cita no programada) | 409 |
| Error inesperado | 500 (mensaje genérico; el detalle solo va al log) |

Las respuestas de error **no repiten** los valores enviados por el cliente ni exponen detalles internos (nombres de clases, mensajes SQL, stack traces).

---

## Arquitectura

Arquitectura **por capas** con principios **SOLID**:

```
 Cliente HTTP
     │  JSON
     ▼
┌──────────────┐   valida formato (@Valid), traduce HTTP ⇄ DTO
│  Controller  │
└──────┬───────┘
       │  DTO
       ▼
┌──────────────┐   casos de uso + reglas de negocio, @Transactional
│   Service    │──────────► Policies (reglas puras: horario, penalización)
└──────┬───────┘──────────► Clock (fuente única de "ahora")
       │  Entidades
       ▼
┌──────────────┐   Spring Data JPA (consultas parametrizadas)
│  Repository  │
└──────┬───────┘
       ▼
     H2 (en memoria)

 GlobalExceptionHandler: excepciones ──► ProblemDetail (400/404/409/500)
```

| Capa | Responsabilidad |
|---|---|
| `controller` | Recibe HTTP, valida el formato y devuelve el código HTTP. Sin lógica de negocio. |
| `service` | Orquesta cada caso de uso y aplica las reglas dentro de una transacción. Interfaces + `impl`. |
| `domain/policy` | Reglas de negocio puras y sin estado (`WorkingHoursPolicy`, `PenaltyPolicy`). Se prueban sin Spring. |
| `domain/model` | Entidades JPA. `Appointment.cancel()` protege su propio cambio de estado. |
| `repository` | Acceso a datos con Spring Data JPA; filtros dinámicos con `Specification`. |
| `dto` / `mapper` | Contrato de entrada/salida de la API; las entidades nunca se exponen. |
| `exception` | Excepciones propias y traducción centralizada a `ProblemDetail`. |
| `config` | Bean `Clock`, que permite fijar la hora en las pruebas. |

---

## Estructura del proyecto

```
.
├── docs/
│   ├── specs/SPEC.md              # Especificación (requerimientos y reglas)
│   ├── design/DESIGN.md           # Diseño (arquitectura, modelo, API, testing)
│   ├── tasks/TASKS.md             # Plan de implementación (25 tareas)
│   └── GUIA_IMPLEMENTACION.md     # Explicación detallada de la implementación
├── HELP.md                        # Introducción al proyecto para juniors en backend
├── src/main/java/com/ceiba/prueba/
│   ├── PruebaApplication.java
│   ├── config/                    # ClockConfig
│   ├── controller/                # DoctorController, PatientController, AppointmentController
│   ├── domain/
│   │   ├── model/                 # Doctor, Patient, Appointment, Penalty, AppointmentStatus
│   │   └── policy/                # WorkingHoursPolicy, PenaltyPolicy
│   ├── dto/
│   │   ├── request/               # DoctorRequest, PatientRequest, AppointmentRequest, AppointmentFilter
│   │   └── response/              # DoctorResponse, PatientResponse, AppointmentResponse, TimeSlotResponse
│   ├── exception/                 # Excepciones, GlobalExceptionHandler, ProblemDetailInstanceAdvice
│   ├── mapper/                    # DoctorMapper, PatientMapper, AppointmentMapper
│   ├── repository/                # Repositorios JPA
│   │   └── specification/         # AppointmentSpecifications (filtros RF-06)
│   └── service/                   # Interfaces de servicio
│       └── impl/                  # Implementaciones
├── src/main/resources/
│   ├── application.properties
│   └── data.sql                   # Carga inicial de los 3 médicos
├── src/test/java/...              # Pruebas (espejo de main)
└── pom.xml
```

---

## Modelo de datos

```
DOCTOR      (id PK, full_name, specialty, phone?, email?)
PATIENT     (id PK, full_name, document_number UNIQUE, phone, email, birth_date?)
APPOINTMENT (id PK, patient_id FK, doctor_id FK, date_time, status, cancelled_at?)
PENALTY     (id PK, patient_id FK, appointment_id FK UNIQUE, created_at)
```

- Índices en `appointment(doctor_id, date_time)` y `appointment(patient_id, date_time)`.
- `status` se guarda como texto (`SCHEDULED`, `CANCELLED`).
- No hay `UNIQUE(doctor_id, date_time)`: una cita cancelada conserva su franja y esta debe poder reservarse de nuevo; RN-02/RN-04 se validan en el servicio.

---

## Seguridad

| Requisito | Medida |
|---|---|
| Validación de inputs | Bean Validation en los DTOs (`@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@PastOrPresent`, `@NotNull`). |
| Protección contra inyección | Solo Spring Data JPA: consultas derivadas, JPQL con parámetros y `Specification`; nunca SQL concatenado. |
| Datos sensibles | Entidades nunca expuestas; errores sin valores del cliente ni detalles internos; documento no incluido en mensajes ni logs; consola H2 deshabilitada. |

Autenticación/autorización está **fuera de alcance** según la especificación.

---

## Decisiones y supuestos

| # | Decisión |
|---|---|
| D-1 | `startDate`/`endDate` (RF-04, RF-06) en `yyyy-MM-dd`, ambos extremos inclusivos. |
| D-2 | Una cita programada cuya hora ya pasó se puede cancelar y genera penalización. |
| D-3 | Cancelar con exactamente 2 h de antelación **no** genera penalización. |
| D-4 | IDs autoincrementales; los médicos iniciales se insertan sin ID explícito. |
| D-5 | Todos los errores responden con `ProblemDetail`. |
| — | Zona horaria: la del servidor (`LocalDateTime`, sin offset). |
| — | Código y API en inglés; documentación en español. |
| — | Mensajes de validación siempre en inglés (`spring.web.locale=en`). |
| — | No se consideran festivos (excluidos del alcance). |

---

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/specs/SPEC.md`](docs/specs/SPEC.md) | Requerimientos funcionales, reglas de negocio y requerimientos técnicos. |
| [`docs/design/DESIGN.md`](docs/design/DESIGN.md) | Arquitectura, modelo de datos, API, manejo de errores, seguridad y estrategia de pruebas. |
| [`docs/tasks/TASKS.md`](docs/tasks/TASKS.md) | Las 25 tareas de implementación con criterios de aceptación y evidencia. |