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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/auth/me/ips-confiaveis")
@RequiredArgsConstructor
public class IPConfiavelController {

    private final IpConfiavelService ipConfiavelService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<IpConfiavelResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
        Integer idUsuario = buscarId(jwt);
        List<IpConfiavelResponse> lista = ipConfiavelService.listar(idUsuario)
                .stream().map(IpConfiavelResponse::from).toList();
        return ResponseEntity.ok(lista);
    }

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

    @Auditavel(acao = AcaoAuditoria.IP_CONFIAVEL_REMOVIDO, categoria = CategoriaAuditoria.SEGURANCA)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        Integer idUsuario = buscarId(jwt);
        ipConfiavelService.remover(id, idUsuario);
        return ResponseEntity.noContent().build();
    }

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
