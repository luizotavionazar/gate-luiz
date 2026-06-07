package br.com.luizotavionazar.authluiz.api.setup.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.setup.dto.ConfiguracaoEmailPublicaResponse;
import br.com.luizotavionazar.authluiz.api.setup.dto.SalvarSetupRequest;
import br.com.luizotavionazar.authluiz.api.setup.dto.StatusSetupResponse;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Setup")
@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    @Operation(summary = "Status do setup e variáveis de ambiente",
            description = "Retorna se o setup foi concluído (`setupConcluido`) e se as variáveis de ambiente obrigatórias estão presentes (`bootstrapOk`).")
    @ApiResponse(responseCode = "200", description = "Status do setup")
    @GetMapping("/status")
    public ResponseEntity<StatusSetupResponse> status() {
        return ResponseEntity.ok(setupService.status());
    }

    @Operation(summary = "Obter configuração de e-mail pública",
            description = "Retorna as configurações SMTP não sensíveis (host e porta) atualmente salvas.")
    @ApiResponse(responseCode = "200", description = "Configuração pública de e-mail")
    @GetMapping
    public ResponseEntity<ConfiguracaoEmailPublicaResponse> obter() {
        return ResponseEntity.ok(setupService.obterConfiguracaoPublica());
    }

    @Operation(summary = "Salvar configuração inicial",
            description = "Persiste as configurações SMTP, Twilio e preferências de auditoria. " +
                    "Protegido pelo header `X-Master-Key` com o valor de `APP_SETUP_MASTER_KEY`. " +
                    "Pode ser chamado novamente para atualizar configurações.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setup salvo com sucesso"),
            @ApiResponse(responseCode = "401", description = "Chave mestra inválida ou ausente", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.SETUP_CONFIGURADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping
    public ResponseEntity<MensagemResponse> salvar(@Valid @RequestBody SalvarSetupRequest request) {
        setupService.salvar(request);
        return ResponseEntity.ok(new MensagemResponse("Setup salvo com sucesso."));
    }
}
