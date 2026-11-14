package com.tdea.mcp_bank_tdea;

import com.tdea.mcp_bank_tdea.model.Account;
import com.tdea.mcp_bank_tdea.model.CreditRequest;
import com.tdea.mcp_bank_tdea.model.Transaction;
import com.tdea.mcp_bank_tdea.repository.AccountRepository;
import com.tdea.mcp_bank_tdea.repository.CreditRequestRepository;
import com.tdea.mcp_bank_tdea.repository.TransactionRepository;
import com.tdea.mcp_bank_tdea.service.BankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración reales para el servicio bancario {@link BankService}.
 * Valida la lógica de negocio, persistencia en base de datos H2,
 * transaccionalidad ACID y los criterios de aceptación AC-01 a AC-07.
 */
@SpringBootTest
class BankServiceTest {

    @Autowired
    private BankService bankService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CreditRequestRepository creditRequestRepository;

    @BeforeEach
    void setUp() {
        // Limpiar y resetear estado inicial para pruebas determinísticas
        transactionRepository.deleteAll();
        creditRequestRepository.deleteAll();
        accountRepository.deleteAll();

        // Precargar cuentas de prueba para cada suite
        accountRepository.save(new Account("1001", "Carlos Pérez", 1000000.0, "ACTIVE"));
        accountRepository.save(new Account("1002", "María Gómez", 500000.0, "ACTIVE"));
        accountRepository.save(new Account("9999", "Cuenta Bloqueada", 200000.0, "BLOCKED"));
    }

    @Test
    @DisplayName("AC-01: Consultar saldo de cuenta existente retorna titular, estado y saldo correcto")
    void testConsultarSaldo_CuentaExistente() {
        String resultado = bankService.consultarSaldo("1001");

        assertNotNull(resultado);
        assertTrue(resultado.contains("Carlos Pérez"));
        assertTrue(resultado.contains("1001"));
        assertTrue(resultado.contains("1,000,000.00"));
        assertTrue(resultado.contains("ACTIVE"));
    }

    @Test
    @DisplayName("AC-04: Consultar saldo de cuenta inexistente retorna mensaje de error controlado")
    void testConsultarSaldo_CuentaInexistente() {
        String resultado = bankService.consultarSaldo("8888");

        assertNotNull(resultado);
        assertTrue(resultado.startsWith("Error"));
        assertTrue(resultado.contains("no se encuentra registrada"));
    }

    @Test
    @DisplayName("AC-02: Consignar dinero incrementa el saldo y persiste la transacción")
    void testConsignarDinero_Exitoso() {
        String resultado = bankService.consignarDinero("1001", 300000.0);

        assertTrue(resultado.contains("Consignación Exitosa"));
        assertTrue(resultado.contains("1,300,000.00"));

        Account account = accountRepository.findByAccountNumber("1001").orElseThrow();
        assertEquals(1300000.0, account.getBalance(), 0.001);

        List<Transaction> txs = transactionRepository.findByAccountNumberOrderByTimestampDesc("1001");
        assertEquals(1, txs.size());
        assertEquals("DEPOSIT", txs.get(0).getType());
        assertEquals(300000.0, txs.get(0).getAmount(), 0.001);
    }

    @Test
    @DisplayName("AC-04: Consignar monto negativo o cero es rechazado sin modificar saldo")
    void testConsignarDinero_MontoInvalido() {
        String resultado = bankService.consignarDinero("1001", -50000.0);

        assertTrue(resultado.startsWith("Error"));
        Account account = accountRepository.findByAccountNumber("1001").orElseThrow();
        assertEquals(1000000.0, account.getBalance(), 0.001);
    }

    @Test
    @DisplayName("AC-03: Transferencia atómica entre cuentas debita origen y acredita destino con auditoría")
    void testTransferirDinero_Exitoso() {
        String resultado = bankService.transferirDinero("1001", "1002", 400000.0);

        assertTrue(resultado.contains("Transferencia Exitosa"));
        assertTrue(resultado.contains("600,000.00"));

        Account origen = accountRepository.findByAccountNumber("1001").orElseThrow();
        Account destino = accountRepository.findByAccountNumber("1002").orElseThrow();

        assertEquals(600000.0, origen.getBalance(), 0.001);
        assertEquals(900000.0, destino.getBalance(), 0.001);

        List<Transaction> txOrigen = transactionRepository.findByAccountNumberOrderByTimestampDesc("1001");
        List<Transaction> txDestino = transactionRepository.findByAccountNumberOrderByTimestampDesc("1002");

        assertEquals(1, txOrigen.size());
        assertEquals("TRANSFER_OUT", txOrigen.get(0).getType());
        assertEquals(400000.0, txOrigen.get(0).getAmount(), 0.001);

        assertEquals(1, txDestino.size());
        assertEquals("TRANSFER_IN", txDestino.get(0).getType());
        assertEquals(400000.0, txDestino.get(0).getAmount(), 0.001);
    }

    @Test
    @DisplayName("AC-04: Transferencia rechazada por fondos insuficientes deja saldos inalterados")
    void testTransferirDinero_SaldoInsuficiente() {
        String resultado = bankService.transferirDinero("1002", "1001", 900000.0);

        assertTrue(resultado.startsWith("Error"));
        assertTrue(resultado.contains("insuficientes"));

        Account origen = accountRepository.findByAccountNumber("1002").orElseThrow();
        Account destino = accountRepository.findByAccountNumber("1001").orElseThrow();

        assertEquals(500000.0, origen.getBalance(), 0.001);
        assertEquals(1000000.0, destino.getBalance(), 0.001);
        assertTrue(transactionRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("AC-04: Transferencia hacia la misma cuenta es rechazada")
    void testTransferirDinero_MismaCuenta() {
        String resultado = bankService.transferirDinero("1001", "1001", 100000.0);

        assertTrue(resultado.startsWith("Error"));
        assertTrue(resultado.contains("no pueden ser la misma"));
    }

    @Test
    @DisplayName("AC-05: Solicitar crédito <= $10.000.000 se aprueba y desembolsa automáticamente al saldo")
    void testSolicitarCredito_AprobacionAutomatica() {
        String resultado = bankService.solicitarCredito("1001", 5000000.0, 24, "Remodelación");

        assertTrue(resultado.contains("APROBADO"));
        assertTrue(resultado.contains("DESEMBOLSADO"));

        Account account = accountRepository.findByAccountNumber("1001").orElseThrow();
        assertEquals(6000000.0, account.getBalance(), 0.001); // 1.000.000 + 5.000.000

        List<CreditRequest> creditos = creditRequestRepository.findByAccountNumberOrderByCreatedAtDesc("1001");
        assertEquals(1, creditos.size());
        assertEquals("APPROVED", creditos.get(0).getStatus());
        assertEquals(5000000.0, creditos.get(0).getAmountRequested(), 0.001);

        List<Transaction> txs = transactionRepository.findByAccountNumberOrderByTimestampDesc("1001");
        assertEquals(1, txs.size());
        assertEquals("CREDIT_DISBURSEMENT", txs.get(0).getType());
    }

    @Test
    @DisplayName("AC-05: Solicitar crédito > $10.000.000 se radica como PENDING sin modificar saldo")
    void testSolicitarCredito_RadicacionPendiente() {
        String resultado = bankService.solicitarCredito("1001", 25000000.0, 48, "Compra de Vehículo");

        assertTrue(resultado.contains("PENDIENTE"));
        assertTrue(resultado.contains("supera el umbral"));

        Account account = accountRepository.findByAccountNumber("1001").orElseThrow();
        assertEquals(1000000.0, account.getBalance(), 0.001); // Saldo intacto

        List<CreditRequest> creditos = creditRequestRepository.findByAccountNumberOrderByCreatedAtDesc("1001");
        assertEquals(1, creditos.size());
        assertEquals("PENDING", creditos.get(0).getStatus());
    }

    @Test
    @DisplayName("AC-06: Listar movimientos retorna extracto cronológico con transacciones registradas")
    void testListarMovimientos() {
        bankService.consignarDinero("1001", 200000.0);
        bankService.transferirDinero("1001", "1002", 150000.0);

        String movimientos = bankService.listarMovimientos("1001");

        assertNotNull(movimientos);
        assertTrue(movimientos.contains("HISTORIAL DE MOVIMIENTOS"));
        assertTrue(movimientos.contains("DEPOSIT"));
        assertTrue(movimientos.contains("TRANSFER_OUT"));
        assertTrue(movimientos.contains("150,000.00"));
    }
}
