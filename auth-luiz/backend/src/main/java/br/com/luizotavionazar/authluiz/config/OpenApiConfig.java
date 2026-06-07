package br.com.luizotavionazar.authluiz.config;

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
                        .title("AuthLuiz API")
                        .version("1.0")
                        .description("""
                                API de autenticação e identidade. Emite JWTs RS256, gerencia contas, \
                                OAuth via Google, verificação em dois fatores (TOTP, e-mail, SMS/WhatsApp) \
                                e recuperação de senha.

                                **Como autenticar:**
                                1. Chame `POST /auth/login` e copie o `token` da resposta.
                                2. Clique em **Authorize** e cole o token no campo `bearerAuth`.
                                3. Todos os endpoints protegidos passarão o header `Authorization: Bearer <token>`.

                                **Endpoints de serviço interno** (`/auth/interno/**`) não usam JWT. \
                                Informe o valor de `AUTH_LUIZ_SERVICE_KEY` diretamente no campo `X-Service-Key` \
                                exibido no "Try it out" de cada endpoint.
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
                                        .description("JWT emitido por `POST /auth/login`. Cole apenas o token, sem o prefixo 'Bearer'.")
                        )
                )
                .tags(List.of(
                        new Tag().name("Setup").description("Configuração inicial da aplicação — requer master key no header `X-Master-Key`"),
                        new Tag().name("Interno").description("Endpoints server-to-server protegidos por `X-Service-Key`"),
                        new Tag().name("Autenticação").description("Cadastro, login local, logout e recuperação de senha"),
                        new Tag().name("Login Pendente (2FA)").description("Verificação do segundo fator quando o login exige confirmação adicional"),
                        new Tag().name("Minha Conta").description("Perfil do usuário autenticado e atualização de dados"),
                        new Tag().name("OAuth").description("Login e vínculo via Google"),
                        new Tag().name("Verificação").description("Confirmação de e-mail e telefone via código de 6 dígitos"),
                        new Tag().name("2FA").description("Configuração de autenticação de dois fatores: TOTP, backup codes e verificação extra"),
                        new Tag().name("IPs Confiáveis").description("Gerenciamento de IPs confiáveis para login sem segundo fator"),
                        new Tag().name("JWKS").description("Chave pública RSA para verificação externa de JWTs")
                ));
    }
}
