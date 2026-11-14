package com.tdea.mcp_bank_tdea.service;

import com.tdea.mcp_bank_tdea.model.Account;
import com.tdea.mcp_bank_tdea.model.CreditRequest;
import com.tdea.mcp_bank_tdea.model.Transaction;
import com.tdea.mcp_bank_tdea.repository.AccountRepository;
import com.tdea.mcp_bank_tdea.repository.CreditRequestRepository;
import com.tdea.mcp_bank_tdea.repository.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servicio bancario central que expone operaciones financieras como herramientas MCP (Model Context Protocol).
 * Gestiona consultas de saldo, consignaciones, transferencias entre cuentas,
 * radicación de solicitudes de crédito y consulta de historial de transacciones,
 * garantizando persistencia en base de datos H2 e integridad transaccional ACID.
 */
@Service
public class BankService {

    private static final double AUTO_APPROVAL_CREDIT_LIMIT = 10000000.0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CreditRequestRepository creditRequestRepository;

    /**
     * Inyección de dependencias para los repositorios de persistencia.
     *
     * @param accountRepository       Repositorio JPA para cuentas bancarias.
     * @param transactionRepository   Repositorio JPA para movimientos financieros.
     * @param creditRequestRepository Repositorio JPA para solicitudes de crédito.
     */
    public BankService(AccountRepository accountRepository,
                       TransactionRepository transactionRepository,
                       CreditRequestRepository creditRequestRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.creditRequestRepository = creditRequestRepository;
    }

    /**
     * Consulta el saldo actual, titular y estado de una cuenta bancaria.
     *
     * @param numeroCuenta Número único de cuenta bancaria a consultar.
     * @return Mensaje descriptivo con el estado, titular y saldo disponible de la cuenta, o mensaje de error si no existe.
     */
    @Tool(
            name = "consultarSaldo",
            description = "Consulta el saldo disponible, titular y estado de una cuenta bancaria en el sistema a partir de su número de cuenta."
    )
    public String consultarSaldo(
            @ToolParam(description = "Número único de la cuenta bancaria (ej. 1001, 1002).") String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
            return "Error: Debe proporcionar un número de cuenta válido.";
        }

        Optional<Account> accountOpt = accountRepository.findByAccountNumber(numeroCuenta.trim());
        if (accountOpt.isEmpty()) {
            return "Error: La cuenta número '" + numeroCuenta.trim() + "' no se encuentra registrada en el sistema.";
        }

        Account account = accountOpt.get();
        return String.format(Locale.US, "Consulta de Cuenta: Cuenta No: %s | Titular: %s | Estado: %s | Saldo Disponible: $%,.2f COP",
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getStatus(),
                account.getBalance());
    }

    /**
     * Realiza una consignación (depósito de dinero) a una cuenta bancaria registrada.
     *
     * @param numeroCuenta Número de cuenta destino donde se consignará el dinero.
     * @param monto        Monto en pesos colombianos (COP) a depositar. Debe ser estrictamente mayor a cero.
     * @return Mensaje de confirmación con el nuevo saldo, o mensaje de error en caso de fallo de validación.
     */
    @Tool(
            name = "consignarDinero",
            description = "Realiza un depósito de fondos en una cuenta bancaria específica y registra el movimiento de auditoría."
    )
    @Transactional
    public String consignarDinero(
            @ToolParam(description = "Número de la cuenta a la que se le abonará el dinero.") String numeroCuenta,
            @ToolParam(description = "Monto en pesos colombianos (COP) a consignar. Debe ser mayor a 0.") double monto) {
        if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
            return "Error: Debe proporcionar un número de cuenta válido.";
        }
        if (monto <= 0) {
            return "Error: El monto a consignar debe ser mayor a cero.";
        }

        Optional<Account> accountOpt = accountRepository.findByAccountNumber(numeroCuenta.trim());
        if (accountOpt.isEmpty()) {
            return "Error: La cuenta número '" + numeroCuenta.trim() + "' no existe.";
        }

        Account account = accountOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return "Error: La cuenta '" + numeroCuenta.trim() + "' se encuentra en estado inactivo o bloqueado.";
        }

        double nuevoSaldo = account.getBalance() + monto;
        account.setBalance(nuevoSaldo);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                account.getAccountNumber(),
                "DEPOSIT",
                monto,
                nuevoSaldo,
                "Consignación en efectivo/depósito a cuenta"
        );
        transactionRepository.save(transaction);

        return String.format(Locale.US, "Consignación Exitosa: Se han acreditado $%,.2f COP a la cuenta %s. Nuevo saldo: $%,.2f COP.",
                monto, account.getAccountNumber(), nuevoSaldo);
    }

    /**
     * Transfiere fondos de forma atómica y segura entre dos cuentas bancarias activas.
     *
     * @param cuentaOrigen  Número de cuenta de donde se debitarán los fondos.
     * @param cuentaDestino Número de cuenta a donde se acreditarán los fondos.
     * @param monto         Monto a transferir en pesos colombianos (COP).
     * @return Mensaje de confirmación detallando la transferencia y saldo final, o causa del rechazo.
     */
    @Tool(
            name = "transferirDinero",
            description = "Transfiere dinero de forma atómica entre dos cuentas bancarias distintas, verificando fondos suficientes."
    )
    @Transactional
    public String transferirDinero(
            @ToolParam(description = "Número de la cuenta bancaria de origen (debitada).") String cuentaOrigen,
            @ToolParam(description = "Número de la cuenta bancaria de destino (acreditada).") String cuentaDestino,
            @ToolParam(description = "Monto a transferir en pesos (COP). Debe ser mayor a 0.") double monto) {
        if (cuentaOrigen == null || cuentaOrigen.trim().isEmpty() ||
                cuentaDestino == null || cuentaDestino.trim().isEmpty()) {
            return "Error: Debe especificar tanto la cuenta de origen como la cuenta de destino.";
        }
        if (cuentaOrigen.trim().equalsIgnoreCase(cuentaDestino.trim())) {
            return "Error: La cuenta de origen y la cuenta de destino no pueden ser la misma.";
        }
        if (monto <= 0) {
            return "Error: El monto a transferir debe ser mayor a cero.";
        }

        Optional<Account> origenOpt = accountRepository.findByAccountNumber(cuentaOrigen.trim());
        if (origenOpt.isEmpty()) {
            return "Error: La cuenta de origen '" + cuentaOrigen.trim() + "' no existe.";
        }

        Optional<Account> destinoOpt = accountRepository.findByAccountNumber(cuentaDestino.trim());
        if (destinoOpt.isEmpty()) {
            return "Error: La cuenta de destino '" + cuentaDestino.trim() + "' no existe.";
        }

        Account origen = origenOpt.get();
        Account destino = destinoOpt.get();

        if (!"ACTIVE".equalsIgnoreCase(origen.getStatus())) {
            return "Error: La cuenta de origen '" + cuentaOrigen.trim() + "' no está activa.";
        }
        if (!"ACTIVE".equalsIgnoreCase(destino.getStatus())) {
            return "Error: La cuenta de destino '" + cuentaDestino.trim() + "' no está activa.";
        }

        if (origen.getBalance() < monto) {
            return String.format(Locale.US, "Error: Fondos insuficientes en la cuenta origen %s. Saldo actual: $%,.2f COP, monto requerido: $%,.2f COP.",
                    origen.getAccountNumber(), origen.getBalance(), monto);
        }

        // Débito origen
        double saldoFinalOrigen = origen.getBalance() - monto;
        origen.setBalance(saldoFinalOrigen);
        accountRepository.save(origen);

        // Crédito destino
        double saldoFinalDestino = destino.getBalance() + monto;
        destino.setBalance(saldoFinalDestino);
        accountRepository.save(destino);

        // Registrar auditoría de origen
        Transaction txOrigen = new Transaction(
                origen.getAccountNumber(),
                "TRANSFER_OUT",
                monto,
                saldoFinalOrigen,
                "Transferencia enviada a la cuenta " + destino.getAccountNumber()
        );
        transactionRepository.save(txOrigen);

        // Registrar auditoría de destino
        Transaction txDestino = new Transaction(
                destino.getAccountNumber(),
                "TRANSFER_IN",
                monto,
                saldoFinalDestino,
                "Transferencia recibida desde la cuenta " + origen.getAccountNumber()
        );
        transactionRepository.save(txDestino);

        return String.format(Locale.US, "Transferencia Exitosa: Se transfirieron $%,.2f COP de la cuenta %s (%s) a la cuenta %s (%s). Saldo disponible en origen: $%,.2f COP.",
                monto,
                origen.getAccountNumber(), origen.getOwnerName(),
                destino.getAccountNumber(), destino.getOwnerName(),
                saldoFinalOrigen);
    }

    /**
     * Radica y procesa una solicitud de crédito bancario para un titular de cuenta.
     * Si el monto es menor o igual a $10.000.000 COP, se aprueba y desembolsa inmediatamente al saldo;
     * si supera dicho tope, se radica en estado PENDING para estudio crediticio posterior.
     *
     * @param numeroCuenta Número de cuenta a la que se asociará el crédito.
     * @param monto        Monto solicitado en pesos colombianos (COP).
     * @param plazoMeses   Plazo solicitado en meses para el crédito (mínimo 1).
     * @param motivo       Motivo, destinación o justificación del crédito.
     * @return Mensaje de resolución de la solicitud de crédito con el estado y saldo resultante.
     */
    @Tool(
            name = "solicitarCredito",
            description = "Radica una solicitud de crédito bancario. Si el monto es menor o igual a 10 millones COP, se aprueba y desembolsa automáticamente en la cuenta; si es mayor, queda radicada en estudio."
    )
    @Transactional
    public String solicitarCredito(
            @ToolParam(description = "Número de cuenta bancaria que solicita el crédito.") String numeroCuenta,
            @ToolParam(description = "Monto del crédito solicitado en pesos COP (ej. 5000000).") double monto,
            @ToolParam(description = "Plazo de amortización en meses (ej. 12, 24, 36).") int plazoMeses,
            @ToolParam(description = "Destino o motivo del crédito (ej. Libre inversión, educación).") String motivo) {
        if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
            return "Error: Debe especificar un número de cuenta válido.";
        }
        if (monto <= 0) {
            return "Error: El monto del crédito debe ser mayor a cero.";
        }
        if (plazoMeses <= 0) {
            return "Error: El plazo en meses debe ser al menos de 1 mes.";
        }
        String motivoLimpio = (motivo == null || motivo.trim().isEmpty()) ? "Libre inversión" : motivo.trim();

        Optional<Account> accountOpt = accountRepository.findByAccountNumber(numeroCuenta.trim());
        if (accountOpt.isEmpty()) {
            return "Error: La cuenta '" + numeroCuenta.trim() + "' no existe en el sistema.";
        }

        Account account = accountOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return "Error: La cuenta '" + numeroCuenta.trim() + "' no se encuentra activa para solicitar créditos.";
        }

        if (monto <= AUTO_APPROVAL_CREDIT_LIMIT) {
            // Aprobación automática y desembolso inmediato
            CreditRequest credit = new CreditRequest(
                    account.getAccountNumber(),
                    monto,
                    plazoMeses,
                    motivoLimpio,
                    "APPROVED"
            );
            creditRequestRepository.save(credit);

            double nuevoSaldo = account.getBalance() + monto;
            account.setBalance(nuevoSaldo);
            accountRepository.save(account);

            Transaction tx = new Transaction(
                    account.getAccountNumber(),
                    "CREDIT_DISBURSEMENT",
                    monto,
                    nuevoSaldo,
                    String.format("Desembolso crédito aprobado #%d (%s - %d meses)", credit.getId(), motivoLimpio, plazoMeses)
            );
            transactionRepository.save(tx);

            return String.format(Locale.US, "¡Crédito APROBADO y DESEMBOLSADO!: Se radicó y aprobó automáticamente el crédito #%d por $%,.2f COP a %d meses (%s). El dinero ya está disponible en la cuenta %s. Nuevo saldo: $%,.2f COP.",
                    credit.getId(), monto, plazoMeses, motivoLimpio, account.getAccountNumber(), nuevoSaldo);
        } else {
            // Requiere validación de analista
            CreditRequest credit = new CreditRequest(
                    account.getAccountNumber(),
                    monto,
                    plazoMeses,
                    motivoLimpio,
                    "PENDING"
            );
            creditRequestRepository.save(credit);

            return String.format(Locale.US, "Solicitud de Crédito RADICADA EN ESTUDIO: La solicitud #%d por $%,.2f COP a %d meses para la cuenta %s supera el umbral de aprobación automática ($10.000.000 COP). Ha quedado registrada en estado PENDIENTE para revisión manual.",
                    credit.getId(), monto, plazoMeses, account.getAccountNumber());
        }
    }

    /**
     * Lista el historial cronológico de movimientos y transacciones registradas en una cuenta bancaria.
     *
     * @param numeroCuenta Número de cuenta a consultar.
     * @return Reporte en texto plano con el extracto de movimientos registrados o indicación de cuenta sin movimientos.
     */
    @Tool(
            name = "listarMovimientos",
            description = "Obtiene el historial cronológico de transacciones registradas de una cuenta bancaria (depósitos, débitos, transferencias y créditos)."
    )
    public String listarMovimientos(
            @ToolParam(description = "Número de la cuenta bancaria de la cual listar transacciones.") String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
            return "Error: Debe proporcionar un número de cuenta válido.";
        }

        Optional<Account> accountOpt = accountRepository.findByAccountNumber(numeroCuenta.trim());
        if (accountOpt.isEmpty()) {
            return "Error: La cuenta número '" + numeroCuenta.trim() + "' no existe.";
        }

        List<Transaction> transacciones = transactionRepository.findByAccountNumberOrderByTimestampDesc(numeroCuenta.trim());
        if (transacciones.isEmpty()) {
            return "La cuenta " + numeroCuenta.trim() + " no registra movimientos financieros en el historial.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== HISTORIAL DE MOVIMIENTOS CUENTA %s ===\n", numeroCuenta.trim()));
        for (Transaction tx : transacciones) {
            sb.append(String.format(Locale.US, "- [%s] %s: $%,.2f COP | Saldo posterior: $%,.2f COP | Detalle: %s\n",
                    tx.getTimestamp().format(DATE_FORMATTER),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getBalanceAfter(),
                    tx.getDescription()));
        }
        return sb.toString();
    }
}
