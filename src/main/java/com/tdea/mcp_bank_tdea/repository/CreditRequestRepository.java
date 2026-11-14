package com.tdea.mcp_bank_tdea.repository;

import com.tdea.mcp_bank_tdea.model.CreditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la administración de solicitudes de crédito.
 */
@Repository
public interface CreditRequestRepository extends JpaRepository<CreditRequest, Long> {

    /**
     * Recupera todas las solicitudes de crédito asociadas a un número de cuenta ordenadas por fecha.
     *
     * @param accountNumber Número de cuenta del solicitante.
     * @return Lista de solicitudes de crédito radicadas.
     */
    List<CreditRequest> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
