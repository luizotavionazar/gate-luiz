package br.com.luizotavionazar.authluiz.api.autenticacao.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.*;
import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.CodigoBackupService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.EnvioCodigoRateLimitService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.IpConfiavelService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LoginPendenteService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.AutenticacaoService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Login Pendente (2FA)")
@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
public class LoginPendenteController {

    private final LoginPendenteService loginPendenteService;
    private final AutenticacaoService autenticacaoService;
    private final IpConfiavelService ipConfiavelService;
    private final CodigoBackupService codigoBackupService;
    private final EnvioCodigoRateLimitService envioCodigoRateLimitService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Verificar código 2FA e concluir login",
            description = "Confirma o segundo fator (TOTP ou OTP por e-mail/SMS/WhatsApp) e retorna o JWT. " +
                    "Opcionalmente salva o IP como confiável via `confiarEsteIp`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login concluído — retorna JWT"),
            @ApiResponse(responseCode = "401", description = "Código inválido ou tokenPendente expirado", content = @Content),
            @ApiResponse(responseCode = "410", description = "tokenPendente bloqueado após 5 tentativas", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.VERIFICACAO_LOGIN_SUCESSO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/verificar")
    public ResponseEntity<LoginResponse> verificar(
            @Valid @RequestBody VerificarLoginPendenteRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extrairIp(httpRequest);
        LoginPendente lp = loginPendenteService.verificar(request.tokenPendente(), request.codigo());
        Usuario usuario = buscarUsuario(lp.getIdUsuario());

        if (request.confiarEsteIp()) {
            ipConfiavelService.confiarIp(usuario.getId(), ip, request.rotuloDispositivo());
        }

        AuditoriaService.definirDetalhes("Verificação de login confirmada — tipo: " + lp.getTipo());
        return ResponseEntity.ok(autenticacaoService.completarLogin(usuario, ip));
    }

    @Operation(summary = "Reenviar código OTP",
            description = "Reenvia o código OTP para o canal escolhido (EMAIL, SMS ou WHATSAPP). " +
                    "Não disponível para logins do tipo TOTP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código reenviado"),
            @ApiResponse(responseCode = "429", description = "Limite de envios por IP atingido", content = @Content),
            @ApiResponse(responseCode = "404", description = "tokenPendente não encontrado ou expirado", content = @Content)
    })
    @PostMapping("/reenviar")
    public ResponseEntity<ReenviarResponse> reenviar(
            @Valid @RequestBody ReenviarVerificacaoRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extrairIp(httpRequest);
        envioCodigoRateLimitService.validarLimitePorIp(ip);
        LoginPendente lp = loginPendenteService.reenviar(request.tokenPendente(), request.canal());
        Usuario usuario = buscarUsuario(lp.getIdUsuario());
        return ResponseEntity.ok(new ReenviarResponse(
                lp.getTipo(),
                LoginPendenteResponse.mascarar(lp.getTipo(), usuario),
                "Código reenviado com sucesso."));
    }

    @Operation(summary = "Usar código de backup 2FA",
            description = "Conclui login pendente usando um dos 8 códigos de backup (formato XXXX-XXXX). O código é invalidado após uso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login concluído — retorna JWT"),
            @ApiResponse(responseCode = "401", description = "Código de backup inválido ou já utilizado", content = @Content),
            @ApiResponse(responseCode = "404", description = "tokenPendente não encontrado ou expirado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.VERIFICACAO_LOGIN_SUCESSO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping("/codigo-backup")
    public ResponseEntity<LoginResponse> usarCodigoBackup(
            @Valid @RequestBody UsarCodigoBackupRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extrairIp(httpRequest);
        LoginPendente lp = loginPendenteService.buscarAtivo(request.tokenPendente());
        Usuario usuario = buscarUsuario(lp.getIdUsuario());

        boolean usado = codigoBackupService.usar(usuario.getId(), request.codigoBackup());
        if (!usado) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Código de backup inválido ou já utilizado.");
        }

        loginPendenteService.confirmar(lp);
        AuditoriaService.definirDetalhes("Login via código de backup");
        return ResponseEntity.ok(autenticacaoService.completarLogin(usuario, ip));
    }

    private Usuario buscarUsuario(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
