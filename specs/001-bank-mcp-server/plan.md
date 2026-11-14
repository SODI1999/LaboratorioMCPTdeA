# [PLAN-001] Plan Técnico: Servidor MCP Bancario con Spring Boot y H2

Fecha: 2026-11-01  
Spec: [SPEC-001](spec.md)  

---

## 1. Evidencia y Arquitectura Existente

- **Entorno verificado:** Java OpenJDK 25.0.4.1 LTS (`java -version`), compilando con target Java 21 en [pom.xml](../../pom.xml).
- **Stack real en `pom.xml`:** Spring Boot `4.1.2`, Spring AI `2.0.1` (`spring-ai-starter-mcp-server`), H2 `2.4.240`, Spring Data JPA.
- **Hallazgo crítico de compatibilidad (Spring AI 2.0.1 vs Guía PDF):** En Spring AI `2.0.1` (verificado con `javap`), `ToolCallbacks` se encuentra en el paquete `org.springframework.ai.support.ToolCallbacks` (en la guía con Spring AI 1.0.0 aparecía en `org.springframework.ai.tool.ToolCallbacks`). `@Tool` reside en `org.springframework.ai.tool.annotation.Tool` y `ToolCallback` en `org.springframework.ai.tool.ToolCallback`.

---

## 2. Diseño Propuesto y Componentes

### 2.1 Estructura del Proyecto
```
pom.xml
mvnw / mvnw.cmd
src/
├── main/
│   ├── java/com/tdea/mcp_bank_tdea/
│   │   ├── McpBankTdeaApplication.java
│   │   ├── config/
│   │   │   └── DataInitializer.java
│   │   ├── model/
│   │   │   ├── Account.java
│   │   │   ├── Transaction.java
│   │   │   └── CreditRequest.java
│   │   ├── repository/
│   │   │   ├── AccountRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   └── CreditRequestRepository.java
│   │   └── service/
│   │       └── BankService.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/tdea/mcp_bank_tdea/
        ├── McpBankTdeaApplicationTests.java
        └── BankServiceTest.java
```

### 2.2 Capa de Persistencia (H2 + Spring Data JPA)
- **Account:** Identificador, número de cuenta único (`accountNumber`), nombre del titular (`ownerName`), saldo (`balance`), estado (`ACTIVE`/`BLOCKED`).
- **Transaction:** Identificador, número de cuenta (`accountNumber`), tipo (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`, `CREDIT_DISBURSEMENT`), monto (`amount`), fecha y hora (`timestamp`), descripción (`description`).
- **CreditRequest:** Identificador, número de cuenta (`accountNumber`), monto solicitado (`amountRequested`), plazo en meses (`termMonths`), motivo (`purpose`), estado (`PENDING`, `APPROVED`, `REJECTED`), fecha de solicitud (`createdAt`).

### 2.3 Capa de Servicio y Tools MCP (`BankService`)
Métodos expuestos con `@Tool`:
1. `consultarSaldo(String numeroCuenta)`: Verifica existencia de cuenta y retorna titular, estado y saldo actual.
2. `consignarDinero(String numeroCuenta, double monto)`: Valida monto > 0, incrementa el saldo y registra la transacción en `Transaction`.
3. `transferirDinero(String cuentaOrigen, String cuentaDestino, double monto)`: Valida existencia de ambas cuentas, que sean distintas y que la cuenta origen tenga fondos suficientes; actualiza ambos saldos de manera atómica (`@Transactional`) y registra ambos movimientos (`TRANSFER_OUT` y `TRANSFER_IN`).
4. `solicitarCredito(String numeroCuenta, double monto, int plazoMeses, String motivo)`: Valida cuenta y parámetros, registra la solicitud en `CreditRequest` y aplica la regla de aprobación definida.
5. `listarMovimientos(String numeroCuenta)`: Consulta las transacciones registradas de la cuenta ordenadas por fecha descendente.

### 2.4 Configuración de Transporte MCP Stdio
- Propiedades en `src/main/resources/application.properties`:
  - `spring.ai.mcp.server.name=bank-mcp-server`
  - `spring.ai.mcp.server.version=1.0.0`
  - `spring.ai.mcp.server.type=SYNC`
  - `spring.ai.mcp.server.stdio=true`
  - `spring.main.web-application-type=none`
  - `spring.main.banner-mode=off`
  - `logging.pattern.console=` (crítico para no contaminar `stdout` con logs que rompan JSON-RPC)
  - `logging.file.name=./target/mcp-bank-tdea.log`
- Registro de bean `List<ToolCallback>` en `McpBankTdeaApplication` mediante `List.of(ToolCallbacks.from(bankService))`.

---

## 3. Contratos y Compatibilidad

- **Protocolo de comunicación:** Model Context Protocol (MCP) JSON-RPC 2.0 sobre `stdio`.
- **Compatibilidad con Claude Desktop:**
  Configuración en `%APPDATA%\Claude\claude_desktop_config.json`:
  ```json
  {
    "mcpServers": {
      "bank-mcp-server": {
        "command": "java",
        "args": [
          "-jar",
          "C:\\dev\\LaboratorioMCPTdeA\\target\\mcp-bank-tdea-0.0.1-SNAPSHOT.jar"
        ]
      }
    }
  }
  ```

---

## 4. Observabilidad y Auditoría

- Cada operación financiera genera un registro en la tabla `transactions` de H2.
- Los logs de Spring Boot e Hibernate se envían exclusivamente a `./target/mcp-bank-tdea.log`.

---

## 5. Decisiones de Tecnología y Estándares

- **Java:** Target 21 sobre JDK 25.0.4.1 LTS.
- **Framework:** Spring Boot 4.1.2 con Spring AI 2.0.1.
- **Documentación:** Javadoc estricto y completo en todas las clases, entidades, interfaces de repositorio y métodos `@Tool` (propósito, `@param`, `@return`, excepciones e invariantes de negocio). Se prohíben comentarios vacíos o que solo narren sintaxis.
- **Convención de Commits:** Conventional Commits 1.0.0 en español.

---

## 6. UI & Interaction Design

- **N/A:** El proyecto es un servidor MCP headless que se comunica por `stdio` con Claude Desktop; no posee interfaz gráfica web propia.

---

## 7. Verificación

| Criterio | Tipo de Check | Comando / Prueba |
| :--- | :--- | :--- |
| `AC-01` a `AC-06` | Automatizado (`test`) | `BankServiceTest.java` ejecutado vía `.\mvnw.cmd test` |
| `AC-07` | Automatizado (`test`) | Verificación de `DataInitializer` en test de contexto |
| `AC-08` | Automatizado (`package`) | `.\mvnw.cmd clean package` generando `target/mcp-bank-tdea-0.0.1-SNAPSHOT.jar` |

---

## 8. Rollback y Recuperación

- La base de datos H2 se almacena en `./data/bankdb.mv.db`. Eliminar dicho archivo restablece el estado inicial con las cuentas semilla en el siguiente arranque.
