package br.com.luizotavionazar.authluiz.api.oauth.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginPendenteResponse;
import br.com.luizotavionazar.authluiz.api.oauth.dto.DesvincularGoogleRequest;
import br.com.luizotavionazar.authluiz.api.oauth.dto.GoogleLoginRequest;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.service.GoogleAuthService;
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

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final GoogleAuthService googleAuthService;

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
