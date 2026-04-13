package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.ConfirmacaoService;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/confirmar")
    public ResponseEntity<MensagemResponse> confirmarEmail(@RequestParam String token) {
        confirmacaoService.confirmarEmail(token);
        return ResponseEntity.ok(new MensagemResponse("E-mail confirmado com sucesso!"));
    }

    @PostMapping("/reenviar")
    public ResponseEntity<MensagemResponse> reenviarVerificacao(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        Integer idUsuario = jwt.getClaim("id");
        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(confirmacaoService.reenviarVerificacao(idUsuario, ip));
    }
}
