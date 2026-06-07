package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.*;

import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.AutenticacaoService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LogoutService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final LogoutService logoutService;

    @Operation(summary = "Cadastrar novo usuário",
            description = "Cria uma conta com e-mail, senha, username e nome. Envia e-mail de boas-vindas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
            @ApiResponse(responseCode = "409", description = "E-mail, username ou telefone já cadastrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Dados inválidos (validação)", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.CADASTRO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/cadastro")
    public ResponseEntity<ContaResponse> cadastrar(
            @Valid @RequestBody CadastroRequest request,
            HttpServletRequest httpRequest
    ) {
        ContaResponse response = autenticacaoService.cadastrar(request, extrairIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login local",
            description = """
                    Autentica com e-mail, telefone ou username + senha.

                    **Se retornar 200:** login concluído — use o `token` da resposta nas próximas requisições.

                    **Se retornar 202 (`LoginPendenteResponse`):** verificação adicional necessária. \
                    O campo `tipo` indica o próximo passo:

                    - **`TOTP`** — chame `POST /auth/login/verificar` com `{tokenPendente, codigo}` \
                    onde `codigo` é o código de 6 dígitos do aplicativo autenticador.
                    - **`EMAIL` / `SMS` / `WHATSAPP`** — o código já foi enviado ao destino indicado em `destinoMascarado`. \
                    Chame `POST /auth/login/verificar` com `{tokenPendente, codigo}`. \
                    Para reenviar ou trocar de canal use `POST /auth/login/reenviar`.
                    - **`AGUARDANDO_CANAL`** — o usuário tem telefone verificado e precisa escolher o canal. \
                    Primeiro chame `POST /auth/login/reenviar` com `{tokenPendente, canal}` \
                    (canal = `EMAIL`, `WHATSAPP` ou `SMS`) para receber o código; \
                    depois chame `POST /auth/login/verificar` com `{tokenPendente, codigo}`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado — retorna JWT",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "202", description = "Verificação adicional necessária — siga o campo `tipo` para o próximo passo",
                    content = @Content(schema = @Schema(implementation = LoginPendenteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content),
            @ApiResponse(responseCode = "503", description = "Setup não concluído", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.LOGIN_SUCESSO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        String ip = IpUtils.extrairIp(httpRequest);
        Object resultado = autenticacaoService.login(request, ip);
        if (resultado instanceof LoginPendenteResponse lp) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(lp);
        }
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Iniciar recuperação de senha",
            description = """
                    Envia um código de 6 dígitos para o e-mail ou telefone informado.

                    **Próximos passos após o 200:**
                    1. Chame `POST /auth/recuperacao/validar` com `{email|telefone, codigo}` para confirmar o código.
                    2. Em seguida chame `POST /auth/recuperacao/redefinir` com `{email|telefone, codigo, novaSenha}` para definir a nova senha.

                    O código expira em 5 minutos e bloqueia após 5 tentativas erradas.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código enviado — próximo passo: `POST /auth/recuperacao/validar`"),
            @ApiResponse(responseCode = "429", description = "Limite de envios por IP atingido", content = @Content),
            @ApiResponse(responseCode = "503", description = "Canal de SMS não configurado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.RECUPERACAO_SENHA_INICIADA, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/recuperacao/iniciar")
    public ResponseEntity<RecuperacaoIniciarResponse> iniciarRecuperacaoSenha(
            @Valid @RequestBody RecuperacaoSenhaRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                autenticacaoService.iniciarRecuperacaoSenha(request, extrairIp(httpRequest))
        );
    }

    @Operation(summary = "Validar código de recuperação",
            description = """
                    Confirma que o código de 6 dígitos está correto sem alterar a senha. \
                    Use este endpoint para validar o código antes de exibir o formulário de nova senha.

                    **Próximo passo após o 200:** chame `POST /auth/recuperacao/redefinir` \
                    com `{email|telefone, codigo, novaSenha}` usando o mesmo código validado aqui.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código válido — próximo passo: `POST /auth/recuperacao/redefinir`"),
            @ApiResponse(responseCode = "401", description = "Código inválido ou expirado", content = @Content),
            @ApiResponse(responseCode = "410", description = "Código bloqueado após 5 tentativas erradas", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.RECUPERACAO_SENHA_CODIGO_VALIDADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/recuperacao/validar")
    public ResponseEntity<MensagemResponse> validarCodigoRecuperacao(
            @Valid @RequestBody ValidarCodigoRecuperacaoRequest request
    ) {
        return ResponseEntity.ok(autenticacaoService.validarCodigoRecuperacao(request));
    }

    @Operation(summary = "Redefinir senha",
            description = "Define nova senha usando o código válido. Invalida todos os IPs confiáveis após redefinição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Código inválido ou expirado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Nova senha não atende a política de senhas", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.RECUPERACAO_SENHA_REDEFINIDA, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/recuperacao/redefinir")
    public ResponseEntity<MensagemResponse> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(autenticacaoService.redefinirSenha(request, extrairIp(httpRequest)));
    }

    @Operation(summary = "Logout",
            description = "Invalida o JWT atual inserindo seu `jti` na blacklist. O token é rejeitado em todas as requisições subsequentes até expirar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout realizado"),
            @ApiResponse(responseCode = "401", description = "Token inválido ou expirado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.LOGOUT, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/logout")
    public ResponseEntity<MensagemResponse> logout(@AuthenticationPrincipal Jwt jwt) {
        logoutService.invalidar(jwt);
        return ResponseEntity.ok(new MensagemResponse("Logout realizado com sucesso."));
    }

    private String extrairIp(HttpServletRequest request) {
        return IpUtils.extrairIp(request);
    }
}
