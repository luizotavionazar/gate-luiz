package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.permluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.permluiz.domain.permissao.PermissaoRepository;
import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
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
@RequestMapping("/admin/permissoes")
@RequiredArgsConstructor
public class AdminPermissaoController {

    private final PermissaoRepository permissaoRepository;
    private final AdminVerificador adminVerificador;

    @GetMapping
    List<PermissaoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.exigirAdmin(jwt);
        return permissaoRepository.findAll().stream().map(PermissaoResponse::de).toList();
    }

    @Auditavel(acao = AcaoAuditoria.PERMISSAO_CRIADA)
    @PostMapping
    ResponseEntity<PermissaoResponse> criar(@AuthenticationPrincipal Jwt jwt,
                                            @RequestBody @Valid PermissaoRequest request) {
        adminVerificador.exigirAdmin(jwt);

        if (permissaoRepository.existsByRecursoAndAcao(request.recurso(), request.acao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma permissão com esse recurso e ação!");
        }

        Permissao permissao = new Permissao();
        permissao.setRecurso(request.recurso().toLowerCase());
        permissao.setAcao(request.acao().toLowerCase());
        permissao.setDescricao(request.descricao());
        permissaoRepository.save(permissao);

        AuditoriaService.definirDetalhes("Permissão '" + permissao.getRecurso() + ":" + permissao.getAcao() + "'"
                + (permissao.getDescricao() != null ? " — " + permissao.getDescricao() : ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissaoResponse.de(permissao));
    }

    @Auditavel(acao = AcaoAuditoria.PERMISSAO_ATUALIZADA)
    @PutMapping("/{id}")
    PermissaoResponse atualizar(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable Long id,
                                @RequestBody @Valid PermissaoRequest request) {
        adminVerificador.exigirAdmin(jwt);

        Permissao permissao = permissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada!"));

        String chaveAnterior = permissao.getRecurso() + ":" + permissao.getAcao();
        String descricaoAnterior = permissao.getDescricao();
        permissao.setRecurso(request.recurso().toLowerCase());
        permissao.setAcao(request.acao().toLowerCase());
        permissao.setDescricao(request.descricao());
        permissaoRepository.save(permissao);

        AuditoriaService.definirDetalhes("Permissão atualizada: '" + chaveAnterior + "' → '"
                + permissao.getRecurso() + ":" + permissao.getAcao() + "'"
                + " | descrição '" + descricaoAnterior + "' → '" + permissao.getDescricao() + "'");
        return PermissaoResponse.de(permissao);
    }

    @Auditavel(acao = AcaoAuditoria.PERMISSAO_DELETADA)
    @DeleteMapping("/{id}")
    ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        Permissao permissao = permissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada!"));
        AuditoriaService.definirDetalhes("Permissão '" + permissao.getRecurso() + ":" + permissao.getAcao() + "' removida"
                + (permissao.getDescricao() != null ? " — " + permissao.getDescricao() : ""));
        permissaoRepository.delete(permissao);
        return ResponseEntity.noContent().build();
    }
}
