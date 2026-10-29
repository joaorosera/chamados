package br.com.sistemas.chamados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Documentação da API. O springdoc lê os controllers, DTOs e validações e gera o
 * contrato OpenAPI em /v3/api-docs; a interface fica em /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chamadosOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Sistema de Chamados")
            .version("1.0")
            .description("API REST em arquitetura em camadas da disciplina DAS (UNIVILLE). "
                + "Erros seguem o formato ErroResposta: momento, status, erro e detalhes."));
    }
}
