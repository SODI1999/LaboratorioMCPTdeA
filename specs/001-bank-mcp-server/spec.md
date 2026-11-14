# [SPEC-001] Servidor MCP Bancario con Spring Boot y H2

Fecha: 2026-11-01  
Tipo: Feature  
Estado: Especificado  

---

## 0. Clarification and Assumptions (Fase de Clarificación)

- **Requisitos de Origen:** Página 17 de la [Guía de Laboratorio TdeA (1)](../../Guia-LaboratorioMCPTDEA.pdf) (Prof. Diego José Luis Botia).
- **Decisiones Confirmadas:**
  - Runtime: Java 21 target sobre JDK 25.0.4.1 LTS.
  - Framework: Spring Boot 4.1.2 con Spring AI 2.0.1 (`spring-ai-starter-mcp-server`).
  - Base de Datos: H2 Database 2.4.240 embebida en archivo local con Spring Data JPA.
  - Paquete Base: `com.tdea.mcp_bank_tdea` (clase principal `McpBankTdeaApplication`).
  - Transporte MCP: `stdio` (entrada/salida estándar) para compatibilidad con Claude Desktop.
  - Estándar de Documentación: Javadoc estricto en todas las clases, interfaces, repositorios y métodos de herramientas `@Tool` (propósito, `@param`, `@return`, efectos secundarios e invariantes de negocio, exigencia docente obligatoria).
  - Convención de Commits: Conventional Commits 1.0.0 en español.

---

## 1. Necesidad y Alcance

### 1.1 Necesidad
El usuario y el docente requieren un servidor MCP (Model Context Protocol) capaz de exponer operaciones bancarias reales hacia un cliente de IA (Claude Desktop), permitiendo al usuario interactuar en lenguaje natural para realizar transacciones persistidas en base de datos.

### 1.2 Alcance
- Modelo de datos relacional para Cuentas (`Account`), Transacciones (`Transaction`) y Solicitudes de Crédito (`CreditRequest`).
- Inicialización de datos semilla (cuentas de prueba preconfiguradas con saldo inicial).
- Servicio bancario con métodos expuestos como `@Tool`:
  1. `consultarSaldo`: Consulta de saldo y titular.
  2. `consignarDinero`: Depósito de dinero a una cuenta.
  3. `transferirDinero`: Transferencia atómica entre dos cuentas con validación de saldo suficiente.
  4. `solicitarCredito`: Creación y registro de una solicitud de crédito asociada a una cuenta.
  5. `listarMovimientos`: Historial de transacciones de una cuenta.
- Configuración del servidor MCP síncrono mediante `stdio`.
- Instrucciones y configuración JSON para Claude Desktop (`claude_desktop_config.json`).

### 1.3 Criterios de No-Alcance (Non-Goals)
- No se implementará interfaz gráfica web o frontend móvil (la interfaz es Claude Desktop mediante MCP).
- No se conectará a pasarelas bancarias externas de producción (PSE, redes interbancarias reales).
- No se utilizarán motores de base de datos externos pesados (Oracle, SQL Server remoto); se utilizará H2 embebido.

---

## 2. Criterios de Aceptación (EARS) y No-Objetivos

- **AC-01 [functional]:** El sistema SHALL proveer la herramienta `consultarSaldo` que retorne el número de cuenta, titular, estado y saldo actual para una cuenta dada.
- **AC-02 [functional]:** WHEN se proporcione un número de cuenta válido y un monto positivo a `consignarDinero`, el sistema SHALL incrementar el saldo de la cuenta, registrar la transacción en la base de datos y retornar un mensaje de confirmación con el nuevo saldo.
- **AC-03 [functional]:** WHEN se solicite una transferencia mediante `transferirDinero` entre dos cuentas existentes y la cuenta origen posea fondos suficientes, el sistema SHALL debitar atómicamente el monto de la cuenta origen, acreditarlo en la cuenta destino, registrar ambos movimientos en la BD y retornar la confirmación.
- **AC-04 [functional]:** IF la cuenta origen no posee fondos suficientes, alguna cuenta no existe, o el monto es menor o igual a cero, THEN el sistema SHALL rechazar la operación con un mensaje explicativo y mantener los saldos inalterados.
- **AC-05 [functional]:** WHEN se invoque `solicitarCredito` con número de cuenta válido, monto positivo, plazo en meses y motivo, el sistema SHALL aprobar (`APPROVED`) y desembolsar inmediatamente el monto al saldo de la cuenta si es menor o igual a `10000000` COP (registrando movimiento de desembolso), o radicar la solicitud en estado `PENDING` sin modificar el saldo si supera dicho tope.
- **AC-06 [functional]:** El sistema SHALL proveer la herramienta `listarMovimientos` para consultar el historial cronológico de transacciones de una cuenta.
- **AC-07 [functional]:** El sistema SHALL inicializar automáticamente la base de datos H2 con al menos dos cuentas de prueba con saldo inicial al iniciar la aplicación.
- **AC-08 [functional]:** El sistema SHALL ejecutarse como servidor MCP síncrono sobre `stdio` mediante el ejecutable `.jar` generado.

---

## 3. Aplicabilidad de Dominio

| Control de Seguridad & Cumplimiento | ¿Requerido? | Aplicación Técnica |
| :--- | :---: | :--- |
| **¿Maneja datos personales sensibles (PII)?** | Sí | Nombres de titulares y números de cuenta. Al ser entorno de laboratorio académico, se utilizan identidades y cuentas ficticias de prueba. |
| **¿Expone datos multi-inquilino (Cross-tenant)?** | N/A | Aplicación mono-inquilino orientada a usuario local. |
| **¿Requiere registro de auditoría (Audit Logging)?** | Sí | Cada débito, crédito o transferencia queda registrado en la tabla `Transaction` con marca de tiempo, tipo y monto. |
| **¿Integridad transaccional de datos?** | Sí | Las operaciones de transferencia están anotadas con `@Transactional` en Spring para garantizar atomicidad (ACID). |

---

## 4. Experiencia y Límites

- **Interfaz de Usuario:** N/A directa en el backend; la interfaz es el host MCP (Claude Desktop).
- **Manejo de Errores hacia la IA:** Todas las tools retornan mensajes descriptivos en texto plano estructurado para que el modelo LLM entienda el motivo exacto de cualquier rechazo (ej. "Fondos insuficientes", "Cuenta destino inexistente").

---

## 5. Trazabilidad

| Criterio | Componente / Archivo | Método / Verificación |
| :--- | :--- | :--- |
| `AC-01` | `BankService.java` | Test unitario de consulta de saldo |
| `AC-02` | `BankService.java` | Test de consignación exitosa e historial |
| `AC-03` | `BankService.java` | Test transaccional de transferencia exitosa |
| `AC-04` | `BankService.java` | Test de rechazo por saldo insuficiente |
| `AC-05` | `BankService.java` | Test de radicación de solicitud de crédito |
| `AC-06` | `BankService.java` | Test de listado de movimientos |
| `AC-07` | `DataInitializer.java` | Verificación de cuentas semilla en H2 |
| `AC-08` | `McpBankTdeaApplication.java` | Verificación de empaquetado JAR y arranque stdio |
