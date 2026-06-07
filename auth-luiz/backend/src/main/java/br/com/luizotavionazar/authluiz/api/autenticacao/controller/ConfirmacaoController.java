package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ConfirmarEmailRequest;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.ConfirmacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Verificação")
@RestController
@RequestMapping("/auth/verificacao")
@RequiredArgsConstructor
public class ConfirmacaoController {

    private final ConfirmacaoService confirmacaoService;

    @Operation(summary = "Confirmar e-mail via código",
            description = "Valida o código de 6 dígitos enviado ao e-mail. Detecta automaticamente se é verificação de cadastro ou alteração de e-mail.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "E-mail confirmado — retorna conta atualizada"),
            @ApiResponse(responseCode = "401", description = "Código inválido ou expirado", content = @Content),
            @ApiResponse(responseCode = "410", description = "Código bloqueado após 5 tentativas", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.EMAIL_CONFIRMADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/email/confirmar")
    public ResponseEntity<ContaResponse> confirmarEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarEmailRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(confirmacaoService.confirmarEmail(publicId, request.codigo()));
    }

    @Operation(summary = "Enviar código de verificação de e-mail",
            description = "Envia ou reenvia o código de 6 dígitos. Detecta automaticamente o tipo pendente: " +
                    "verificação de cadastro (`emailVerificado=false`) ou alteração (`emailPendente!=null`). Cooldown de 2 minutos.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código enviado"),
            @ApiResponse(responseCode = "400", description = "Nenhuma verificação pendente de e-mail", content = @Content),
            @ApiResponse(responseCode = "429", description = "Cooldown de 2 min ou limite por IP atingido", content = @Content)
    })
    @PostMapping("/email/enviar")
    public ResponseEntity<MensagemResponse> enviarVerificacaoEmail(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        String publicId = jwt.getSubject();
        String ip = IpUtils.extrairIp(httpRequest);
        return ResponseEntity.ok(confirmacaoService.enviarVerificacaoEmail(publicId, ip));
    }

    @Operation(summary = "Confirmar telefone via código",
            description = "Valida o código de 6 dígitos enviado via WhatsApp/SMS. Detecta automaticamente se é verificação inicial ou alteração de telefone.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telefone confirmado — retorna conta atualizada"),
            @ApiResponse(responseCode = "401", description = "Código inválido ou expirado", content = @Content),
            @ApiResponse(responseCode = "410", description = "Código bloqueado após 5 tentativas", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.TELEFONE_CONFIRMADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/telefone/confirmar")
    public ResponseEntity<ContaResponse> confirmarTelefone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmarEmailRequest request
    ) {
        String publicId = jwt.getSubject();
        return ResponseEntity.ok(confirmacaoService.confirmarTelefone(publicId, request.codigo()));
    }

    @Operation(summary = "Enviar código de verificação de telefone",
            description = "Envia ou reenvia o código de 6 dígitos via WhatsApp/SMS. Cooldown de 2 minutos.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código enviado"),
            @ApiResponse(responseCode = "400", description = "Nenhuma verificação pendente de telefone", content = @Content),
            @ApiResponse(responseCode = "429", description = "Cooldown de 2 min ou limite por IP atingido", content = @Content),
            @ApiResponse(responseCode = "503", description = "Twilio não configurado", content = @Content)
    })
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
