package br.com.luizotavionazar.authluiz.api.oauth.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginPendenteResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginResponse;
import br.com.luizotavionazar.authluiz.api.oauth.dto.DesvincularGoogleRequest;
import br.com.luizotavionazar.authluiz.api.oauth.dto.GoogleLoginRequest;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OAuth")
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final GoogleAuthService googleAuthService;

    @Operation(summary = "Login ou cadastro via Google",
            description = """
                    Autentica com Google ID token. Se a conta não existir, cria automaticamente.

                    **Se retornar 200:** login concluído — use o `token` da resposta.

                    **Se retornar 202 (`LoginPendenteResponse`):** verificação adicional necessária \
                    (conta com verificação extra ativa e IP desconhecido). \
                    O campo `tipo` indica o próximo passo:

                    - **`TOTP`** — chame `POST /auth/login/verificar` com `{tokenPendente, codigo}` \
                    onde `codigo` é o código do aplicativo autenticador.
                    - **`EMAIL` / `SMS` / `WHATSAPP`** — código enviado ao `destinoMascarado`. \
                    Chame `POST /auth/login/verificar` com `{tokenPendente, codigo}`.
                    - **`AGUARDANDO_CANAL`** — escolha o canal com `POST /auth/login/reenviar` \
                    passando `{tokenPendente, canal}`, depois conclua com `POST /auth/login/verificar`.

                    **Se retornar 409:** o e-mail já está cadastrado com senha local. \
                    Para vincular o Google a essa conta, faça login com senha e use `POST /auth/oauth/google/vincular`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado — retorna JWT",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "202", description = "Verificação adicional necessária — siga o campo `tipo`",
                    content = @Content(schema = @Schema(implementation = LoginPendenteResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado com senha — use `POST /auth/oauth/google/vincular`", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.LOGIN_GOOGLE, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/google")
    public ResponseEntity<?> autenticarComGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Object resultado = googleAuthService.autenticar(request, IpUtils.extrairIp(httpRequest));
        if (resultado instanceof LoginPendenteResponse lp) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(lp);
        }
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Vincular Google à conta",
            description = "Associa uma conta Google à conta autenticada. O e-mail do Google deve ser igual ao da conta.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Google vinculado — retorna conta atualizada"),
            @ApiResponse(responseCode = "400", description = "E-mail do Google diferente do e-mail da conta", content = @Content),
            @ApiResponse(responseCode = "409", description = "Google já vinculado a esta ou outra conta", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.VINCULAR_GOOGLE)
    @PostMapping("/google/vincular")
    public ResponseEntity<ContaResponse> vincularGoogle(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(googleAuthService.vincular(publicId, request, IpUtils.extrairIp(httpRequest)));
    }

    @Operation(summary = "Desvincular Google da conta",
            description = "Remove o vínculo com Google. Exige senha definida para não perder o acesso. " +
                    "Não disponível para contas criadas originalmente via Google (`providerOrigem=GOOGLE`).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Google desvinculado — retorna conta atualizada"),
            @ApiResponse(responseCode = "400", description = "Conta criada via Google não pode ser desvinculada, ou senha não definida", content = @Content),
            @ApiResponse(responseCode = "401", description = "Senha incorreta", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.DESVINCULAR_GOOGLE)
    @DeleteMapping("/google/vincular")
    public ResponseEntity<ContaResponse> desvincularGoogle(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) DesvincularGoogleRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(googleAuthService.desvincular(publicId, request));
    }
}
