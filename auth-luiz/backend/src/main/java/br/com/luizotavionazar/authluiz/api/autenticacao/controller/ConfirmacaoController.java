package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ConfirmarEmailRequest;
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
    public ResponseEntity<MensagemResponse> confirmarEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarEmailRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        confirmacaoService.confirmarEmail(idUsuario, request.codigo());
        return ResponseEntity.ok(new MensagemResponse("E-mail confirmado com sucesso!"));
    }

    @PostMapping("/email/enviar")
    public ResponseEntity<MensagemResponse> enviarVerificacaoEmail(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(confirmacaoService.enviarVerificacaoEmail(idUsuario, ip));
    }

    // ── Telefone ──────────────────────────────────────────────────────────────

    @Auditavel(acao = AcaoAuditoria.TELEFONE_CONFIRMADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/telefone/confirmar")
    public ResponseEntity<MensagemResponse> confirmarTelefone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarEmailRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        confirmacaoService.confirmarTelefone(idUsuario, request.codigo());
        return ResponseEntity.ok(new MensagemResponse("Telefone confirmado com sucesso!"));
    }

    @PostMapping("/telefone/enviar")
    public ResponseEntity<MensagemResponse> enviarVerificacaoTelefone(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(confirmacaoService.enviarVerificacaoTelefone(idUsuario, ip));
    }
}
