package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.CadastroRequest;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.CadastroResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginRequest;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.RecuperacaoSenhaRequest;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.RedefinirSenhaRequest;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(
            @Valid @RequestBody CadastroRequest request,
            HttpServletRequest httpRequest
    ) {
        CadastroResponse response = autenticacaoService.cadastrar(request, extrairIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(autenticacaoService.login(request));
    }

    @PostMapping("/recuperacao/iniciar")
    public ResponseEntity<MensagemResponse> iniciarRecuperacaoSenha(
            @Valid @RequestBody RecuperacaoSenhaRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                autenticacaoService.iniciarRecuperacaoSenha(request, extrairIp(httpRequest))
        );
    }

    @GetMapping("/recuperacao/validar")
    public ResponseEntity<MensagemResponse> validarTokenRecuperacao(@RequestParam String token) {
        return ResponseEntity.ok(autenticacaoService.validarTokenRecuperacao(token));
    }

    @PostMapping("/recuperacao/redefinir")
    public ResponseEntity<MensagemResponse> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return ResponseEntity.ok(autenticacaoService.redefinirSenha(request));
    }

    private String extrairIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
