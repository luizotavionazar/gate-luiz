package br.com.luizotavionazar.authluiz.api.conta.controller;

import br.com.luizotavionazar.authluiz.api.conta.dto.*;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.DoisFatoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2FA")
@RestController
@RequestMapping("/auth/me/2fa")
@RequiredArgsConstructor
public class DoisFatoresController {

    private final DoisFatoresService doisFatoresService;

    @Operation(summary = "Obter status do 2FA",
            description = "Retorna se TOTP está ativo, quantidade de backup codes restantes e se verificação extra está habilitada.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Status do 2FA")
    @GetMapping("/status")
    public ResponseEntity<DoisFatoresStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(doisFatoresService.obterStatus(jwt.getSubject()));
    }

    @Operation(summary = "Iniciar configuração TOTP",
            description = """
                    Gera um segredo TOTP pendente e retorna a URI `otpauth://` para exibição do QR code. \
                    **Pré-condição:** verificação extra deve estar ativa (`PATCH /auth/me/2fa/verificacao-extra`).

                    **Próximos passos após o 200:**
                    1. Exiba o `otpauthUri` como QR code para o usuário escanear com o aplicativo autenticador (Google Authenticator, Authy, etc.).
                    2. Chame `POST /auth/me/2fa/totp/confirmar` com `{codigo}` (código de 6 dígitos gerado pelo app) para ativar o TOTP e receber os 8 backup codes.

                    O TOTP só é ativado após a confirmação bem-sucedida.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URI otpauth gerada — próximo passo: `POST /auth/me/2fa/totp/confirmar`"),
            @ApiResponse(responseCode = "403", description = "Verificação extra não está ativa ou e-mail não verificado", content = @Content)
    })
    @PostMapping("/totp/iniciar")
    public ResponseEntity<IniciarTotpResponse> iniciarTotp(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(doisFatoresService.iniciarTotp(jwt.getSubject()));
    }

    @Operation(summary = "Confirmar TOTP e ativar 2FA",
            description = "Valida o primeiro código TOTP, ativa o autenticador e gera 8 backup codes. " +
                    "Os backup codes são exibidos apenas neste momento.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TOTP ativado — retorna 8 backup codes"),
            @ApiResponse(responseCode = "401", description = "Código TOTP inválido", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.ATIVAR_2FA, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/totp/confirmar")
    public ResponseEntity<ConfirmarTotpResponse> confirmarTotp(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarTotpRequest request) {
        return ResponseEntity.ok(doisFatoresService.confirmarTotp(jwt.getSubject(), request.codigo()));
    }

    @Operation(summary = "Desativar TOTP",
            description = "Remove o autenticador TOTP e todos os backup codes. Exige confirmação por senha.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TOTP desativado — retorna status 2FA atualizado"),
            @ApiResponse(responseCode = "401", description = "Senha incorreta", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.DESATIVAR_2FA, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping
    public ResponseEntity<DoisFatoresStatusResponse> desativar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Desativar2faRequest request) {
        return ResponseEntity.ok(doisFatoresService.desativar(jwt.getSubject(), request.senha()));
    }

    @Operation(summary = "Regerar backup codes",
            description = "Gera 8 novos backup codes, invalidando os anteriores. Exige código TOTP atual para confirmar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novos 8 backup codes gerados"),
            @ApiResponse(responseCode = "401", description = "Código TOTP inválido", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.BACKUP_CODES_REGENERADOS, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/backup-codes/regerar")
    public ResponseEntity<ConfirmarTotpResponse> regerarBackupCodes(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegerarBackupCodesRequest request) {
        return ResponseEntity.ok(doisFatoresService.regerarBackupCodes(jwt.getSubject(), request.codigo()));
    }

    @Operation(summary = "Ativar ou desativar verificação extra",
            description = """
                    Liga ou desliga a exigência de 2FA em todos os logins de IPs desconhecidos.

                    **Para ativar (`ativo: true`):**
                    - A conta deve ter senha definida.
                    - O campo `senha` não é necessário.

                    **Para desativar (`ativo: false`):**
                    - O TOTP deve estar desativado primeiro.
                    - Se a conta tiver senha definida, o campo `senha` é obrigatório para confirmar a identidade.
                    - Contas sem senha (somente Google) podem desativar sem informar senha.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de verificação extra atualizado"),
            @ApiResponse(responseCode = "400", description = "Senha não informada ao desativar, ou TOTP ainda ativo", content = @Content),
            @ApiResponse(responseCode = "401", description = "Senha incorreta ao desativar", content = @Content),
            @ApiResponse(responseCode = "403", description = "Conta sem senha ao tentar ativar, ou e-mail não verificado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.ATIVAR_VERIFICACAO_EXTRA, categoria = CategoriaAuditoria.SEGURANCA)
    @PatchMapping("/verificacao-extra")
    public ResponseEntity<DoisFatoresStatusResponse> atualizarVerificacaoExtra(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AtualizarVerificacaoExtraRequest request) {
        return ResponseEntity.ok(
                doisFatoresService.atualizarVerificacaoExtra(jwt.getSubject(), request.ativo(), request.senha()));
    }
}
