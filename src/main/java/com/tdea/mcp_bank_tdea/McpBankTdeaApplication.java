package com.tdea.mcp_bank_tdea;

import com.tdea.mcp_bank_tdea.service.BankService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Clase principal de arranque del servidor MCP bancario con Spring Boot.
 * Expone y registra las herramientas financieras de {@link BankService}
 * para el protocolo Model Context Protocol (MCP) mediante {@link ToolCallback}.
 */
@SpringBootApplication
public class McpBankTdeaApplication {

    /**
     * Punto de entrada principal de la aplicación Java.
     *
     * @param args Argumentos de arranque de la aplicación.
     */
    public static void main(String[] args) {
        SpringApplication.run(McpBankTdeaApplication.class, args);
    }

    /**
     * Registra las herramientas bancarias de {@link BankService} como callbacks MCP
     * consumibles por el servidor MCP de Spring AI.
     *
     * @param bankService Servicio con las operaciones bancarias anotadas con {@code @Tool}.
     * @return Lista de {@link ToolCallback} registradas en el contenedor de Spring.
     */
    @Bean
    public List<ToolCallback> bankTools(BankService bankService) {
        return List.of(ToolCallbacks.from(bankService));
    }
}
