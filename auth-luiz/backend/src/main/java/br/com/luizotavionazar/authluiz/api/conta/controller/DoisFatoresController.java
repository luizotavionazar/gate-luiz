package br.com.luizotavionazar.authluiz.api.conta.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.conta.dto.*;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.DoisFatoresService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/me/2fa")
@RequiredArgsConstructor
public class DoisFatoresController {

    private final DoisFatoresService doisFatoresService;

    @GetMapping("/status")
    public ResponseEntity<DoisFatoresStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(doisFatoresService.obterStatus(jwt.getSubject()));
    }

    @PostMapping("/totp/iniciar")
    public ResponseEntity<IniciarTotpResponse> iniciarTotp(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(doisFatoresService.iniciarTotp(jwt.getSubject()));
    }

    @Auditavel(acao = AcaoAuditoria.ATIVAR_2FA, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/totp/confirmar")
    public ResponseEntity<ConfirmarTotpResponse> confirmarTotp(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarTotpRequest request) {
        return ResponseEntity.ok(doisFatoresService.confirmarTotp(jwt.getSubject(), request.codigo()));
    }

    @Auditavel(acao = AcaoAuditoria.DESATIVAR_2FA, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping
    public ResponseEntity<MensagemResponse> desativar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Desativar2faRequest request) {
        doisFatoresService.desativar(jwt.getSubject(), request.senha());
        return ResponseEntity.ok(new MensagemResponse("Autenticação de dois fatores desativada."));
    }

    @Auditavel(acao = AcaoAuditoria.BACKUP_CODES_REGENERADOS, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/backup-codes/regerar")
    public ResponseEntity<ConfirmarTotpResponse> regerarBackupCodes(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegerarBackupCodesRequest request) {
        return ResponseEntity.ok(doisFatoresService.regerarBackupCodes(jwt.getSubject(), request.codigo()));
    }

    @Auditavel(acao = AcaoAuditoria.ATIVAR_VERIFICACAO_EXTRA, categoria = CategoriaAuditoria.SEGURANCA)
    @PatchMapping("/verificacao-extra")
    public ResponseEntity<MensagemResponse> atualizarVerificacaoExtra(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AtualizarVerificacaoExtraRequest request) {
        doisFatoresService.atualizarVerificacaoExtra(jwt.getSubject(), request.ativo(), request.senha());
        String msg = request.ativo()
                ? "Verificação extra ativada."
                : "Verificação extra desativada.";
        return ResponseEntity.ok(new MensagemResponse(msg));
    }
}
