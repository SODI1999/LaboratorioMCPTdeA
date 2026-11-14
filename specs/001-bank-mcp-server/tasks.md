# [TASKS-001] Tareas de Implementación: Servidor MCP Bancario

Trazabilidad con [SPEC-001](spec.md) y [PLAN-001](plan.md).

---

- [x] **TASK-01** [setup / AC-08]: Configuración inicial del proyecto Maven (`pom.xml`), Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper`) con Spring Boot, Spring AI, Spring Data JPA y H2.
- [x] **TASK-02** [model / AC-07]: Creación de entidades JPA (`Account`, `Transaction`, `CreditRequest`) y sus interfaces de repositorio (`AccountRepository`, `TransactionRepository`, `CreditRequestRepository`).
- [x] **TASK-03** [service / AC-01, AC-02, AC-03, AC-04, AC-05, AC-06]: Implementación de `BankService` con lógica transaccional y anotaciones `@Tool` de Spring AI.
- [x] **TASK-04** [config / AC-07, AC-08]: Configuración de `application.properties` (H2 en archivo local, transporte Stdio, silenciamiento de consola) y `DataInitializer` para precarga de cuentas semilla.
- [x] **TASK-05** [app / AC-08]: Configuración de la clase principal `McpBankTdeaApplication` y registro de beans `ToolCallback`.
- [x] **TASK-06** [tests / AC-01 a AC-07]: Implementación de pruebas unitarias y de integración (`BankServiceTest`) para verificar todas las operaciones bancarias y validaciones de saldo.
- [x] **TASK-07** [package / AC-08]: Compilación y empaquetado del archivo JAR ejecutable con `mvnw clean package` y generación de la configuración para Claude Desktop.
