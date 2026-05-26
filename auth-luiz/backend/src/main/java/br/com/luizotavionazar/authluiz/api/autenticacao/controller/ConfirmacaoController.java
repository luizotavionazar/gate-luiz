package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ConfirmarEmailRequest;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.ConfirmacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/verificacao")
@RequiredArgsConstructor
public class ConfirmacaoController {

    private final ConfirmacaoService confirmacaoService;

    // ── E-mail ────────────────────────────────────────────────────────────────

    @Auditavel(acao = AcaoAuditoria.EMAIL_CONFIRMADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/email/confirmar")
    public ResponseEntity<ContaResponse> confirmarEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarEmailRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(confirmacaoService.confirmarEmail(publicId, request.codigo()));
    }

    @PostMapping("/email/enviar")
    public ResponseEntity<MensagemResponse> enviarVerificacaoEmail(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        String ip = IpUtils.extrairIp(httpRequest);
        return ResponseEntity.ok(confirmacaoService.enviarVerificacaoEmail(publicId, ip));
    }

    // ── Telefone ──────────────────────────────────────────────────────────────

    @Auditavel(acao = AcaoAuditoria.TELEFONE_CONFIRMADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/telefone/confirmar")
    public ResponseEntity<ContaResponse> confirmarTelefone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarEmailRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(confirmacaoService.confirmarTelefone(publicId, request.codigo()));
    }

    @PostMapping("/telefone/enviar")
    public ResponseEntity<MensagemResponse> enviarVerificacaoTelefone(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        String ip = IpUtils.extrairIp(httpRequest);
        return ResponseEntity.ok(confirmacaoService.enviarVerificacaoTelefone(publicId, ip));
    }
}
