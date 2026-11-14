# [VERIFICATION-001] Verificación y Mapeo de Criterios

Fecha: 2026-11-01  
Spec: [SPEC-001](spec.md)  
Estado: Verificado  

---

## 1. Mapeo de Criterios a Evidencia Ejecutada

| Criterio | Tipo | Método | Check ID | Evidencia Ejecutada | Resultado |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `AC-01` | funcional | automated | `test` | `BankServiceTest.testConsultarSaldo_CuentaExistente`: Consulta saldo format $%,.2f COP, titular y estado. | PASSED |
| `AC-02` | funcional | automated | `test` | `BankServiceTest.testConsignarDinero_Exitoso`: Acredita saldo y persiste transacción tipo DEPOSIT. | PASSED |
| `AC-03` | funcional | automated | `test` | `BankServiceTest.testTransferirDinero_Exitoso`: Débito origen, crédito destino y registros TRANSFER_OUT / TRANSFER_IN. | PASSED |
| `AC-04` | funcional | automated | `test` | `BankServiceTest.testTransferirDinero_SaldoInsuficiente` y casos de error: Rechaza y preserva saldos. | PASSED |
| `AC-05` | funcional | automated | `test` | `BankServiceTest.testSolicitarCredito_AprobacionAutomatica` y `RadicacionPendiente`: Regla <= 10.000.000 COP aplicada. | PASSED |
| `AC-06` | funcional | automated | `test` | `BankServiceTest.testListarMovimientos`: Reporte ordenado descendente de movimientos. | PASSED |
| `AC-07` | funcional | automated | `test` | `DataInitializer` y `McpBankTdeaApplicationTests.contextLoads`: Base de datos H2 inicializada con cuentas semilla. | PASSED |
| `AC-08` | funcional | automated | `package` | `mvnw clean package`: Artefacto ejecutable autónomo `target/mcp-bank-tdea-0.0.1-SNAPSHOT.jar` (54.8 MB) generado con transporte Stdio. | PASSED |

---

## 2. Configuración para Claude Desktop

El archivo de configuración para Claude Desktop ([claude_desktop_config.example.json](../../claude_desktop_config.example.json)) contiene:
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

## 3. Limitaciones y Alcance

- Las pruebas se ejecutaron sobre JDK 25 LTS (target Java 21) y base de datos H2 en memoria/archivo local.
- Para la conexión en vivo con Claude Desktop, se debe asegurar que Java esté en el PATH de Windows y pegar el bloque JSON en `%APPDATA%\Claude\claude_desktop_config.json`.
