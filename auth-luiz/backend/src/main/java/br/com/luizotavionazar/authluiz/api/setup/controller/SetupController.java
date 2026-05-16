package br.com.luizotavionazar.authluiz.api.setup.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.setup.dto.ConfiguracaoEmailPublicaResponse;
import br.com.luizotavionazar.authluiz.api.setup.dto.SalvarSetupRequest;
import br.com.luizotavionazar.authluiz.api.setup.dto.StatusSetupResponse;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    @GetMapping("/status")
    public ResponseEntity<StatusSetupResponse> status() {
        return ResponseEntity.ok(setupService.status());
    }

    @GetMapping
    public ResponseEntity<ConfiguracaoEmailPublicaResponse> obter() {
        return ResponseEntity.ok(setupService.obterConfiguracaoPublica());
    }

    @Auditavel(acao = AcaoAuditoria.SETUP_CONFIGURADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping
    public ResponseEntity<MensagemResponse> salvar(@Valid @RequestBody SalvarSetupRequest request) {
        setupService.salvar(request);
        return ResponseEntity.ok(new MensagemResponse("Setup salvo com sucesso."));
    }
}