package br.com.luizotavionazar.authluiz.api.conta.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginPendenteResponse;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarEmailRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarNomeRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarSenhaRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarTelefoneRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarUsernameRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.DeletarContaRequest;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.permluiz.PermLuizService;
import br.com.luizotavionazar.authluiz.domain.usuario.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Minha Conta")
@RestController
@RequestMapping("/auth/me")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;
    private final PermLuizService permLuizService;

    @Operation(summary = "Verificar se é admin no PermLuiz",
            description = "Consulta o PermLuiz e retorna se o usuário autenticado é o admin mestre.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Status de admin retornado")
    @GetMapping("/perm-admin")
    public ResponseEntity<Map<String, Object>> isPermAdmin(@AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = permLuizService.isAdmin(jwt.getTokenValue());
        return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
    }

    @Operation(summary = "Obter dados da conta",
            description = "Retorna o perfil completo do usuário autenticado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Perfil do usuário")
    @GetMapping
    public ResponseEntity<ContaResponse> minhaConta(@AuthenticationPrincipal Jwt jwt) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.obterMinhaConta(publicId));
    }

    @Operation(summary = "Atualizar username",
            description = "Altera o username público (4–30 chars). Usernames reservados são rejeitados.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta atualizada"),
            @ApiResponse(responseCode = "409", description = "Username já em uso", content = @Content),
            @ApiResponse(responseCode = "422", description = "Username inválido ou reservado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.ALTERAR_USERNAME)
    @PatchMapping("/username")
    public ResponseEntity<ContaResponse> atualizarUsername(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarUsernameRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.atualizarUsername(publicId, request));
    }

    @Operation(summary = "Atualizar nome",
            description = "Altera o nome de exibição (3–100 chars).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Conta atualizada")
    @Auditavel(acao = AcaoAuditoria.ALTERAR_NOME)
    @PatchMapping("/nome")
    public ResponseEntity<ContaResponse> atualizarNome(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarNomeRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.atualizarNome(publicId, request));
    }

    @Operation(summary = "Atualizar e-mail",
            description = """
                    Solicita alteração de e-mail. O novo endereço fica em `emailPendente` na resposta e só é aplicado após confirmação. \
                    Bloqueado para contas vinculadas ao Google.

                    **Próximos passos após o 200:**
                    1. Um código de 6 dígitos é enviado ao novo e-mail automaticamente.
                    2. Chame `POST /auth/verificacao/email/confirmar` com `{codigo}` para confirmar e aplicar a mudança.
                    3. Se precisar reenviar o código, use `POST /auth/verificacao/email/enviar` (cooldown de 2 min).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código enviado ao novo e-mail — próximo passo: `POST /auth/verificacao/email/confirmar`"),
            @ApiResponse(responseCode = "400", description = "E-mail igual ao atual ou e-mail não verificado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Novo e-mail já cadastrado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.ALTERAR_EMAIL_SOLICITADO)
    @PatchMapping("/email")
    public ResponseEntity<ContaResponse> atualizarEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.atualizarEmail(publicId, request, IpUtils.extrairIp(httpRequest)));
    }

    @Operation(summary = "Atualizar senha",
            description = "Altera ou define a senha. Exige `senhaAtual` se já houver senha definida. " +
                    "Remove todos os IPs confiáveis ao alterar. Requer e-mail verificado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta atualizada"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou e-mail não verificado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Nova senha não atende a política", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.ALTERAR_SENHA, categoria = CategoriaAuditoria.SEGURANCA)
    @PatchMapping("/senha")
    public ResponseEntity<ContaResponse> atualizarSenha(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarSenhaRequest request,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.atualizarSenha(publicId, request, IpUtils.extrairIp(httpRequest)));
    }

    @Operation(summary = "Atualizar telefone",
            description = """
                    Altera ou remove o telefone (formato E.164). Requer e-mail verificado.

                    **Se `telefone` for `null`:** remove o telefone imediatamente, sem verificação.

                    **Se informar um novo número:** o número fica em `telefonePendente` na resposta e só é aplicado após confirmação. \
                    Próximos passos:
                    1. Um código de 6 dígitos é enviado ao novo número via WhatsApp/SMS automaticamente.
                    2. Chame `POST /auth/verificacao/telefone/confirmar` com `{codigo}` para confirmar e aplicar a mudança.
                    3. Se precisar reenviar o código, use `POST /auth/verificacao/telefone/enviar` (cooldown de 2 min).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telefone removido (se null) ou código enviado — próximo passo: `POST /auth/verificacao/telefone/confirmar`"),
            @ApiResponse(responseCode = "409", description = "Telefone já cadastrado por outro usuário", content = @Content),
            @ApiResponse(responseCode = "503", description = "Twilio não configurado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.ALTERAR_TELEFONE)
    @PatchMapping("/telefone")
    public ResponseEntity<ContaResponse> atualizarTelefone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarTelefoneRequest request,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.atualizarTelefone(publicId, request, IpUtils.extrairIp(httpRequest)));
    }

    @Operation(summary = "Enviar código para exclusão de conta",
            description = """
                    Envia um código OTP por e-mail para confirmar a exclusão. \
                    Usado apenas quando `verificacaoExtraAtiva=true` e TOTP não está ativo.

                    **Próximo passo após o 200:** chame `DELETE /auth/me` com o body \
                    `{tokenPendente, codigo}` usando o `tokenPendente` da resposta e o código de 6 dígitos recebido por e-mail.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Código enviado — próximo passo: `DELETE /auth/me` com `{tokenPendente, codigo}`")
    @PostMapping("/exclusao/codigo")
    public ResponseEntity<LoginPendenteResponse> enviarCodigoExclusaoConta(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(contaService.enviarCodigoExclusaoConta(publicId, IpUtils.extrairIp(httpRequest)));
    }

    @Operation(summary = "Excluir conta",
            description = "Remove permanentemente a conta. " +
                    "Se `verificacaoExtraAtiva=true`: TOTP ativo → body `{codigo}` com código TOTP; " +
                    "sem TOTP → body `{tokenPendente, codigo}` obtido via `POST /auth/me/exclusao/codigo`.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta excluída"),
            @ApiResponse(responseCode = "401", description = "Código de verificação inválido", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.CONTA_DELETADA, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping
    public ResponseEntity<Void> deletarConta(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) DeletarContaRequest request
    ) {
        String publicId = jwt.getSubject();
        permLuizService.notificarDelecaoUsuario(jwt.getTokenValue());
        contaService.deletarConta(publicId, request);
        return ResponseEntity.noContent().build();
    }
}
