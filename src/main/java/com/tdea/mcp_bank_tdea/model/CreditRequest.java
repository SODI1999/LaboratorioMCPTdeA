package com.tdea.mcp_bank_tdea.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad JPA que registra las solicitudes de crédito bancario.
 * Gestiona el monto solicitado, el plazo en meses, el propósito del crédito
 * y su estado operativo (APPROVED o PENDING).
 */
@Entity
@Table(name = "credit_requests")
public class CreditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "amount_requested", nullable = false)
    private double amountRequested;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public CreditRequest() {
    }

    /**
     * Constructor completo para registrar una solicitud de crédito.
     *
     * @param accountNumber   Número de cuenta bancaria del solicitante.
     * @param amountRequested Monto solicitado en pesos colombianos (COP).
     * @param termMonths      Plazo en meses para pagar el crédito.
     * @param purpose         Destino o justificación del crédito.
     * @param status          Estado inicial de la solicitud (APPROVED o PENDING).
     */
    public CreditRequest(String accountNumber, double amountRequested, int termMonths, String purpose, String status) {
        this.accountNumber = accountNumber;
        this.amountRequested = amountRequested;
        this.termMonths = termMonths;
        this.purpose = purpose;
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

    public double getAmountRequested() {
        return amountRequested;
    }

    public void setAmountRequested(double amountRequested) {
        this.amountRequested = amountRequested;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(int termMonths) {
        this.termMonths = termMonths;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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
