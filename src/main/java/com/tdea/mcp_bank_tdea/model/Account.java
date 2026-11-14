package com.tdea.mcp_bank_tdea.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una cuenta bancaria en el sistema.
 * Almacena el número único de cuenta, el titular, el balance disponible
 * y el estado operativo de la cuenta.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "balance", nullable = false)
    private double balance;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public Account() {
    }

    /**
     * Constructor parametrizado para instanciar una nueva cuenta bancaria.
     *
     * @param accountNumber Número único de cuenta bancaria.
     * @param ownerName     Nombre completo del titular de la cuenta.
     * @param balance       Saldo inicial en pesos colombianos (COP).
     * @param status        Estado operativo de la cuenta (ej. ACTIVE, BLOCKED).
     */
    public Account(String accountNumber, String ownerName, double balance, String status) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.status = status;
        this.createdAt = LocalDateTime.now();
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
