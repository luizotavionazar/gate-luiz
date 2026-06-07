package br.com.luizotavionazar.permluiz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PermLuiz API")
                        .version("1.0")
                        .description("""
                                API de controle de acesso baseado em roles e permissões. \
                                Valida JWTs emitidos pelo AuthLuiz via JWKS — não emite tokens próprios.

                                **Como autenticar:**
                                1. Faça login no AuthLuiz (`POST /auth/login`) e copie o `token`.
                                2. Clique em **Authorize** e cole o token no campo `bearerAuth`.

                                **Admin mestre:** o primeiro usuário autenticado a chamar qualquer endpoint \
                                `/admin/**` é automaticamente promovido a admin. \
                                Endpoints `/admin/**` retornam 403 para usuários que não sejam o admin mestre.
                                """)
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT emitido pelo AuthLuiz via `POST /auth/login`. Cole apenas o token, sem o prefixo 'Bearer'.")
                        )
                )
                .tags(List.of(
                        new Tag().name("Setup").description("Verificação de configuração inicial — sem autenticação"),
                        new Tag().name("Minha Conta").description("Roles do usuário autenticado e verificação de admin"),
                        new Tag().name("Admin - Usuários").description("Listagem de usuários e atribuição de roles (requer admin mestre)"),
                        new Tag().name("Admin - Roles").description("CRUD de roles e associação de permissões (requer admin mestre)"),
                        new Tag().name("Admin - Permissões").description("CRUD de permissões (requer admin mestre)")
                ));
    }
}
