package br.com.luizotavionazar.authluiz.api.conta.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarEmailRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarNomeRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarSenhaRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarTelefoneRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.DeletarContaRequest;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.permluiz.PermLuizService;
import br.com.luizotavionazar.authluiz.domain.usuario.service.ContaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/me")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;
    private final PermLuizService permLuizService;

    @GetMapping("/perm-admin")
    public ResponseEntity<Map<String, Object>> isPermAdmin(@AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = permLuizService.isAdmin(jwt.getTokenValue());
        return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
    }

    @GetMapping
    public ResponseEntity<ContaResponse> minhaConta(@AuthenticationPrincipal Jwt jwt) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(contaService.obterMinhaConta(idUsuario));
    }

    @Auditavel(acao = AcaoAuditoria.ALTERAR_NOME)
    @PatchMapping("/nome")
    public ResponseEntity<ContaResponse> atualizarNome(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarNomeRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(contaService.atualizarNome(idUsuario, request));
    }

    @Auditavel(acao = AcaoAuditoria.ALTERAR_EMAIL_SOLICITADO)
    @PatchMapping("/email")
    public ResponseEntity<ContaResponse> atualizarEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(contaService.atualizarEmail(idUsuario, request, httpRequest.getRemoteAddr()));
    }

    @Auditavel(acao = AcaoAuditoria.ALTERAR_SENHA, categoria = CategoriaAuditoria.SEGURANCA)
    @PatchMapping("/senha")
    public ResponseEntity<MensagemResponse> atualizarSenha(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarSenhaRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(contaService.atualizarSenha(idUsuario, request));
    }

    @Auditavel(acao = AcaoAuditoria.ALTERAR_TELEFONE)
    @PatchMapping("/telefone")
    public ResponseEntity<ContaResponse> atualizarTelefone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizarTelefoneRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(contaService.atualizarTelefone(idUsuario, request));
    }

    @Auditavel(acao = AcaoAuditoria.CONTA_DELETADA, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping
    public ResponseEntity<Void> deletarConta(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) DeletarContaRequest request
    ) {
        Integer idUsuario = Integer.valueOf(jwt.getSubject());
        permLuizService.notificarDelecaoUsuario(jwt.getTokenValue());
        contaService.deletarConta(idUsuario, request);
        return ResponseEntity.noContent().build();
    }
}
