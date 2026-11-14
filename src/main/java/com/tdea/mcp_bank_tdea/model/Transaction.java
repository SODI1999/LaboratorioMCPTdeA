package com.tdea.mcp_bank_tdea.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un movimiento financiero en una cuenta bancaria.
 * Proporciona trazabilidad y registro inmutable para auditoría de depósitos,
 * transferencias y desembolsos de créditos.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "balance_after", nullable = false)
    private double balanceAfter;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public Transaction() {
    }

    /**
     * Constructor completo para registrar una nueva transacción financiera.
     *
     * @param accountNumber Número de cuenta afectada por la operación.
     * @param type          Tipo de movimiento (DEPOSIT, TRANSFER_IN, TRANSFER_OUT, CREDIT_DISBURSEMENT).
     * @param amount        Monto procesado en la transacción.
     * @param balanceAfter  Saldo de la cuenta resultante tras aplicar la transacción.
     * @param description   Descripción detallada o motivo de la operación.
     */
    public Transaction(String accountNumber, String type, double amount, double balanceAfter, String description) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
