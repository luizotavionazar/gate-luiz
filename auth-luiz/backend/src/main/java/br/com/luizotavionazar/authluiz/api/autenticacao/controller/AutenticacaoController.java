package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.*;

import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.AutenticacaoService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final LogoutService logoutService;

    @Auditavel(acao = AcaoAuditoria.CADASTRO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(
            @Valid @RequestBody CadastroRequest request,
            HttpServletRequest httpRequest
    ) {
        CadastroResponse response = autenticacaoService.cadastrar(request, extrairIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

    @Auditavel(acao = AcaoAuditoria.RECUPERACAO_SENHA_CODIGO_VALIDADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/recuperacao/validar")
    public ResponseEntity<MensagemResponse> validarCodigoRecuperacao(
            @Valid @RequestBody ValidarCodigoRecuperacaoRequest request
    ) {
        return ResponseEntity.ok(autenticacaoService.validarCodigoRecuperacao(request));
    }

    @Auditavel(acao = AcaoAuditoria.RECUPERACAO_SENHA_REDEFINIDA, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/recuperacao/redefinir")
    public ResponseEntity<MensagemResponse> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(autenticacaoService.redefinirSenha(request, extrairIp(httpRequest)));
    }

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
