package br.com.luizotavionazar.authluiz.api.oauth.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginResponse;
import br.com.luizotavionazar.authluiz.api.oauth.dto.DesvincularGoogleRequest;
import br.com.luizotavionazar.authluiz.api.oauth.dto.GoogleLoginRequest;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.service.GoogleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final GoogleAuthService googleAuthService;

    @Auditavel(acao = AcaoAuditoria.LOGIN_GOOGLE, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> autenticarComGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(googleAuthService.autenticar(request));
    }

    @Auditavel(acao = AcaoAuditoria.VINCULAR_GOOGLE)
    @PostMapping("/google/vincular")
    public ResponseEntity<ContaResponse> vincularGoogle(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(googleAuthService.vincular(idUsuario, request));
    }

    @Auditavel(acao = AcaoAuditoria.DESVINCULAR_GOOGLE)
    @DeleteMapping("/google/vincular")
    public ResponseEntity<ContaResponse> desvincularGoogle(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) DesvincularGoogleRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(googleAuthService.desvincular(idUsuario, request));
    }
}
