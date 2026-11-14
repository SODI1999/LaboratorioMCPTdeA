# Project context

User-owned context.

## 1. Onboarding state and scope
- State: CONTEXT_RECORDED
- Application validation: APPLICATION_VALIDATED
- Inventory date: 2026-11-01T08:39:00.000Z
- Coverage: Proyecto Maven en raíz ([pom.xml](../pom.xml)), código fuente en `src/`, suite de pruebas en `src/test/`, guía de laboratorio TdeA ([Guia-LaboratorioMCPTDEA.pdf](../Guia-LaboratorioMCPTDEA.pdf)) y acuerdo de ingeniería ([CONSTITUTION.md](../CONSTITUTION.md)).
- Se ejecutó `.\mvnw.cmd compile`, `.\mvnw.cmd test` (11 tests exitosos) y `.\mvnw.cmd clean package` generando el JAR ejecutable autónomo.

## 2. Evidence map
- Observed project type: Aplicación Java / Spring Boot generada mediante Spring Initializr (`com.tdea:mcp-bank-tdea:0.0.1-SNAPSHOT`).
- Declared & observed stack in [pom.xml](../pom.xml):
  - `spring-boot-starter-parent`: `4.1.2` (Spring Framework `7.0.10`, Hibernate `7.4.13.Final`)
  - `java.version`: `21` (ejecutando sobre JDK `25.0.4.1` LTS Temurin instalado)
  - `spring-ai-bom`: `2.0.1` (`spring-ai-starter-mcp-server:2.0.1`, `io.modelcontextprotocol.sdk:mcp:2.0.0`)
  - Persistencia: `spring-boot-starter-data-jpa`, `com.h2database:h2:2.4.240`, `spring-boot-h2console:4.1.2`
  - Testing: `spring-boot-starter-data-jpa-test` (JUnit Jupiter `6.0.3`, Mockito `5.23.0`)
- Entry point observado: [McpBankTdeaApplication.java](../src/main/java/com/tdea/mcp_bank_tdea/McpBankTdeaApplication.java) en paquete `com.tdea.mcp_bank_tdea`.
- Config observada: [application.properties](../src/main/resources/application.properties).

## 3. Goals and decisions
- Purpose: Desarrollar un servidor MCP con Spring Boot que se conecte a una BD H2 y permita generar transacciones para servicios bancarios (transferencias, consignaciones, solicitudes de créditos y consultas de saldo/movimientos) conectado a Claude Desktop.
- Intended users: Estudiante y docente evaluador (Prof. Diego José Luis Botia, TdeA 2026).
- Confirmed decisions:
  - Decisión `stack-backend-mcp` (revisión 2): Java 21 (sobre JDK 25 LTS), Spring Boot 4.1.2, Spring AI 2.0.1, H2 Database 2.4.240.
  - Documentación: Javadoc en clases y herramientas `@Tool`.
  - Convención de commits: Conventional Commits 1.0.0 en español.
  - Transporte MCP: Stdio mediante archivo `.jar` ejecutable (`target/mcp-bank-tdea-0.0.1-SNAPSHOT.jar`) para Claude Desktop.
- Constraints: Fecha de entrega declarada en guía: 15 de Noviembre de 2026.

## 4. Command matrix
| Command | Working directory | Source / basis | Prerequisites | Gate selection / reason | Execution |
| --- | --- | --- | --- | --- | --- |
| `.\mvnw.cmd compile` | `.` | [pom.xml](../pom.xml) / Maven Wrapper 3.10.0 | JDK 21+ (JDK 25 instalado) | Verificación de compilación | EXECUTED (2026-11-01T03:00:20-05:00, exit 0, BUILD SUCCESS) |
| `.\mvnw.cmd test` | `.` | [pom.xml](../pom.xml) / Maven Wrapper 3.10.0 | JDK 21+ | Gate principal de pruebas unitarias e integración | EXECUTED (2026-11-01T03:37:51-05:00, exit 0, 11 tests passed) |
| `.\mvnw.cmd clean package` | `.` | [pom.xml](../pom.xml) / Maven Wrapper 3.10.0 | JDK 21+ | Gate de empaquetado de JAR ejecutable para Claude Desktop | EXECUTED (2026-11-01T03:38:35-05:00, exit 0, JAR 54.8 MB generado) |

## 5. Quality baseline and requirements
- Execution evidence:
  - `.\mvnw.cmd -v` en `.` (2026-11-01T03:00:03-05:00, exit 0): Apache Maven 3.10.0 sobre Java 25.0.4.1 Temurin.
  - `.\mvnw.cmd compile` en `.` (2026-11-01T03:00:20-05:00, exit 0): Compilación limpia de `McpBankTdeaApplication.java`.
  - `.\mvnw.cmd dependency:tree` en `.` (2026-11-01T03:24:14-05:00, exit 0): Árbol de dependencias verificado.
  - `.\mvnw.cmd test` en `.` (2026-11-01T03:37:51-05:00, exit 0): 11 pruebas automatizadas pasando (cobertura funcional parcial; contextLoads no afirma las cuentas semilla ni se inyectan fallos para demostrar todo rollback).
  - `.\mvnw.cmd clean package` en `.` (2026-11-01T03:38:35-05:00, exit 0): Artefacto `target/mcp-bank-tdea-0.0.1-SNAPSHOT.jar` generado exitosamente.
- Application health: APPLICATION_VALIDATED.
- Invariantes de dominio: No permitir transferencias con saldo insuficiente ni montos menores o iguales a cero; garantizar atomicidad transaccional; aislar logs de consola para no contaminar el canal `stdio` de JSON-RPC.

## 6. Existing work, risks and pending validation
- Existing-work baseline: Esqueleto generado por Spring Initializr en la raíz del proyecto; sin repositorio Git inicializado (`.git` ausente).
### Source conflicts
<!-- omni:source-conflicts -->
| Tema / Módulo | Fuente A (Guía PDF) | Fuente B (`pom.xml` / Bytecode real) | Consecuencia | Estado / Resolución |
| :--- | :--- | :--- | :--- | :--- |
| Versiones de Spring Boot y Spring AI | Guía declara Spring Boot `3.5.3` y Spring AI `1.0.0` | `start.spring.io` generó [pom.xml](../pom.xml) con Spring Boot `4.1.2` y Spring AI `2.0.1` | En Spring AI `2.0.1`, la clase `ToolCallbacks` se ubica en `org.springframework.ai.support.ToolCallbacks` en lugar de `org.springframework.ai.tool.ToolCallbacks` (Apéndice E del PDF). | Resuelto: usar el import real verificado por `javap` (`org.springframework.ai.support.ToolCallbacks`). |
| Dependencia `spring-boot-h2console` | No incluida en el ejemplo sin BD de la guía | Incluida en [pom.xml](../pom.xml) por Spring Initializr al elegir H2 | En modo `stdio` (`spring.main.web-application-type=none`), no se levanta servidor web. | Resuelto: mantener H2 en archivo local (`./data/bankdb`) sin requerir servidor web activo para MCP Stdio. |

## 7. Next action
Revisar el informe y el paquete de evidencias de la entrega 002. La implementación 001 ya estaba completada; no corresponde volver a aclarar ni reimplementar sus reglas.

## 8. Reconciliación de entrega del 14 de noviembre de 2026
- Autorización del usuario: operar Claude Desktop y Postman, capturar pruebas reales y preparar informe, cuidando la cuota gratuita. Conservar versiones si funcionan. Sin commits, publicación ni despliegue.
- Implementación existente: servidor bancario Stdio; extensión 002: perfil HTTP optativo, JAR HTTP con nombre independiente, configuración de pruebas H2 en memoria y README de reproducción. No se cambió la lógica bancaria.
- Runtime observado: JDK 25.0.4.1; el target de compilación continúa siendo Java 21. No confundir runtime instalado con nivel de bytecode.
- Claude Desktop instalado utiliza Local/Claude-Data/claude_pzs8sxrjxfjjc. El archivo Roaming revisado previamente no era la configuración efectiva. Se respaldó el archivo efectivo antes de añadir mcpServers; las preferencias privadas se conservaron y no se incluyeron en la entrega.
- Ejecutado en Claude: cinco herramientas; consignación 10000, transferencia 5000, rechazo de 999999999, crédito aprobado 100000 y pendiente 10000001. Dos mensajes y permisos atendidos por el usuario. Saldos finales 1001=1155000 y 1002=655000 COP.
- Postman: conexión, catálogo y consultas por HTTP. Primero base bankdb-http separada; después, con Claude cerrado, base bankdb en modo ACCESS_MODE_DATA=r y ddl-auto=validate. Se observaron saldo e historial persistidos. No se ejecutaron nuevas escrituras desde Postman.
- Prueba HTTP de solo lectura: [script](../scripts/verify_mcp_http.py), siete comprobaciones pasando y [respuestas JSON](../output/evidencias/http-verificacion.json).
- Suite final aislada ejecutada 2026-11-14 00:44:59 America/Bogota: 11 tests, sin fallos ni errores; persisten warnings de autoacoplamiento de Mockito y escáner de anotaciones Spring AI.
- Fuente oficial consultada 2026-11-14: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html (página vigente 2.0.2). La compatibilidad del proyecto 2.0.1 se comprobó por build y ejecución, sin actualizar versiones.
- Límites: sin pruebas de carga/concurrencia ni Inspector en esta sesión. Importes double y Javadoc de accesores no auditado exhaustivamente. Las evidencias no equivalen a certificación bancaria.
- Artefactos: [informe PDF](../output/pdf/Informe-Proyecto-MCP-Bancario.pdf), capturas/transcripciones en output/evidencias y colección de consultas en output/postman. Revisión visual del PDF: siete páginas sin recortes de texto ni desbordamientos.

## 9. Revisión documental solicitada por el autor
- Autorización: reemplazar las capturas borrosas por las tomadas manualmente por el usuario, incluir su nombre **Brahian Ospina** al inicio y diagramar la arquitectura. Sin nuevas consultas a Claude ni escrituras bancarias.
- Fuentes nuevas: siete PNG originales 21 a 27 en output/evidencias, recibidos en el chat. El PDF utiliza recortes de los paneles, embebidos sin compresión JPEG; el ZIP conserva los PNG completos.
- Informe actualizado a ocho páginas: diagrama vectorial de componentes y diagrama de flujo de transferencia, ambos contrastados con BankService y los perfiles de transporte. Autor incluido también en los metadatos.
- Verificación de esta revisión: build_delivery.py y check_delivery_artifacts.py ejecutados correctamente; ocho páginas renderizadas e inspeccionadas visualmente, sin desbordamientos ni texto cortado. La comprobación documental no sustituye ni repite las pruebas funcionales registradas en la entrega 002.
- Próxima acción: revisión y entrega del PDF y ZIP por Brahian Ospina. No se publicaron archivos ni se hicieron commits.
