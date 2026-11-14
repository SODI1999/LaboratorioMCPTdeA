package com.tdea.mcp_bank_tdea.config;

import com.tdea.mcp_bank_tdea.model.Account;
import com.tdea.mcp_bank_tdea.model.Transaction;
import com.tdea.mcp_bank_tdea.repository.AccountRepository;
import com.tdea.mcp_bank_tdea.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Componente de inicialización de datos semilla para la base de datos H2.
 * Si la base de datos se encuentra vacía al arrancar la aplicación, precarga
 * cuentas bancarias de prueba con saldos iniciales y registros de apertura.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Inyección de dependencias de los repositorios de cuentas y transacciones.
     *
     * @param accountRepository     Repositorio JPA de cuentas bancarias.
     * @param transactionRepository Repositorio JPA de transacciones.
     */
    public DataInitializer(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Ejecuta la rutina de carga inicial de datos al finalizar el levantamiento del contexto de Spring Boot.
     *
     * @param args Argumentos de línea de comandos.
     */
    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            seedAccount("1001", "Carlos Pérez", 2500000.0);
            seedAccount("1002", "María Gómez", 1200000.0);
            seedAccount("1003", "Juan Rodríguez", 500000.0);
        }
    }

    /**
     * Método auxiliar privado para persistir una cuenta y su movimiento de apertura.
     *
     * @param accountNumber Número único de cuenta bancaria.
     * @param ownerName     Nombre completo del titular.
     * @param initialBalance Saldo inicial asignado a la cuenta.
     */
    private void seedAccount(String accountNumber, String ownerName, double initialBalance) {
        Account account = new Account(accountNumber, ownerName, initialBalance, "ACTIVE");
        accountRepository.save(account);

        Transaction openingTx = new Transaction(
                accountNumber,
                "DEPOSIT",
                initialBalance,
                initialBalance,
                "Apertura de cuenta con depósito inicial"
        );
        transactionRepository.save(openingTx);
    }
}
