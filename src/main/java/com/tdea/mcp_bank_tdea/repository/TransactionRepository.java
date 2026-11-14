package com.tdea.mcp_bank_tdea.repository;

import com.tdea.mcp_bank_tdea.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio Spring Data JPA para el registro y consulta de movimientos financieros.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Obtiene el historial de movimientos de una cuenta bancaria ordenados de forma descendente por fecha.
     *
     * @param accountNumber Número de cuenta a consultar.
     * @return Lista cronológica inversa con las transacciones registradas.
     */
    List<Transaction> findByAccountNumberOrderByTimestampDesc(String accountNumber);
}
