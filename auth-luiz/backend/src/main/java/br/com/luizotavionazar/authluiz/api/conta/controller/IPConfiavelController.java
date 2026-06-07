package br.com.luizotavionazar.authluiz.api.conta.controller;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.api.conta.dto.AdicionarIpRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.IpConfiavelResponse;
import jakarta.servlet.http.HttpServletRequest;
import br.com.luizotavionazar.authluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.IpConfiavelService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "IPs Confiáveis")
@RestController
@RequestMapping("/auth/me/ips-confiaveis")
@RequiredArgsConstructor
public class IPConfiavelController {

    private final IpConfiavelService ipConfiavelService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Listar IPs confiáveis",
            description = "Retorna todos os IPs marcados como confiáveis para a conta autenticada.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Lista de IPs confiáveis")
    @GetMapping
    public ResponseEntity<List<IpConfiavelResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        Integer idUsuario = buscarId(jwt);
        List<IpConfiavelResponse> lista = ipConfiavelService.listar(idUsuario)
                .stream().map(IpConfiavelResponse::from).toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Adicionar IP confiável",
            description = "Adiciona manualmente um IP como confiável. Se `ip` for omitido no body, usa o IP da requisição.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "IP adicionado"),
            @ApiResponse(responseCode = "409", description = "IP já está na lista", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.IP_CONFIAVEL_ADICIONADO, categoria = CategoriaAuditoria.SEGURANCA)
    @PostMapping
    public ResponseEntity<MensagemResponse> adicionar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdicionarIpRequest request,
            HttpServletRequest httpRequest) {
        String ip = request.ip() != null ? request.ip() : IpUtils.extrairIp(httpRequest);
        Integer idUsuario = buscarId(jwt);
        ipConfiavelService.confiarIp(idUsuario, ip, request.rotulo());
        AuditoriaService.definirDetalhes("IP adicionado como confiável: " + ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MensagemResponse("Dispositivo adicionado como confiável."));
    }

    @Operation(summary = "Remover IP confiável por ID",
            description = "Remove um IP específico da lista de IPs confiáveis.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "IP removido"),
            @ApiResponse(responseCode = "404", description = "IP não encontrado", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.IP_CONFIAVEL_REMOVIDO, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        Integer idUsuario = buscarId(jwt);
        ipConfiavelService.remover(id, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remover todos os IPs confiáveis",
            description = "Limpa toda a lista de IPs confiáveis da conta. Próximos logins de qualquer IP exigirão verificação.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Todos os IPs removidos")
    @Auditavel(acao = AcaoAuditoria.IP_CONFIAVEL_REMOVIDO, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping
    public ResponseEntity<Void> removerTodos(@AuthenticationPrincipal Jwt jwt) {
        Integer idUsuario = buscarId(jwt);
        ipConfiavelService.removerTodos(idUsuario);
        return ResponseEntity.noContent().build();
    }

    private Integer buscarId(Jwt jwt) {
        return usuarioRepository.findByPublicId(jwt.getSubject())
                .map(Usuario::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
