package com.tdea.mcp_bank_tdea.repository;

import com.tdea.mcp_bank_tdea.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión y persistencia de cuentas bancarias.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Busca una cuenta bancaria a partir de su número de cuenta único.
     *
     * @param accountNumber Número único de cuenta.
     * @return Contenedor {@link Optional} con la cuenta si existe, o vacío en caso contrario.
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Verifica la existencia de una cuenta bancaria por su número.
     *
     * @param accountNumber Número único de cuenta.
     * @return {@code true} si la cuenta está registrada en la base de datos, {@code false} de lo contrario.
     */
    boolean existsByAccountNumber(String accountNumber);
}
