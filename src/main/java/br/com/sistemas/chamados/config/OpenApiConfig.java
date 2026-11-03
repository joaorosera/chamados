package br.com.sistemas.chamados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentação da API. O springdoc lê os controllers, DTOs e validações e gera o
 * contrato OpenAPI em /v3/api-docs; a interface fica em /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC = "basic";

    @Bean
    public OpenAPI chamadosOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Sistema de Chamados")
                .version("1.0")
                .description("API REST em arquitetura em camadas da disciplina DAS (UNIVILLE). "
                    + "Erros seguem o formato ErroResposta: momento, status, erro e detalhes. "
                    + "Use o botão Authorize com um dos usuários de demonstração "
                    + "(usuario, tecnico ou admin)."))
            // Habilita o botão "Authorize" do Swagger UI para enviar HTTP Basic.
            .components(new Components().addSecuritySchemes(BASIC,
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
            .addSecurityItem(new SecurityRequirement().addList(BASIC));
    }
}
