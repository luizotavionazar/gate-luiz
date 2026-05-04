package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.permluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.permluiz.domain.permissao.PermissaoRepository;
import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import br.com.luizotavionazar.permluiz.domain.role.RoleRepository;
import br.com.luizotavionazar.permluiz.domain.role.entity.Role;
import br.com.luizotavionazar.permluiz.domain.usuariorole.UsuarioRoleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleRepository roleRepository;
    private final PermissaoRepository permissaoRepository;
    private final UsuarioRoleRepository usuarioRoleRepository;
    private final AdminVerificador adminVerificador;

    @GetMapping
    List<RoleResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.exigirAdmin(jwt);
        return roleRepository.findAll().stream().map(RoleResponse::de).toList();
    }

    @Auditavel(acao = AcaoAuditoria.ROLE_CRIADA)
    @PostMapping
    ResponseEntity<RoleResponse> criar(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody @Valid RoleRequest request) {
        adminVerificador.exigirAdmin(jwt);

        if (roleRepository.existsByNome(request.nome())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um role com esse nome!");
        }

        Role role = new Role();
        role.setNome(request.nome().toUpperCase());
        role.setDescricao(request.descricao());
        roleRepository.save(role);

        AuditoriaService.definirDetalhes("Role '" + role.getNome() + "'"
                + (role.getDescricao() != null ? " — " + role.getDescricao() : ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.de(role));
    }

    @Auditavel(acao = AcaoAuditoria.ROLE_ATUALIZADA)
    @PutMapping("/{id}")
    RoleResponse atualizar(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable Long id,
                           @RequestBody @Valid RoleRequest request) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        String nomeAnterior = role.getNome();
        String descricaoAnterior = role.getDescricao();
        role.setNome(request.nome().toUpperCase());
        role.setDescricao(request.descricao());
        roleRepository.save(role);

        AuditoriaService.definirDetalhes("Role atualizado: nome '" + nomeAnterior + "' → '" + role.getNome() + "'"
                + " | descrição '" + descricaoAnterior + "' → '" + role.getDescricao() + "'");
        return RoleResponse.de(role);
    }

    @Auditavel(acao = AcaoAuditoria.ROLE_DELETADA)
    @DeleteMapping("/{id}")
    ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        if (usuarioRoleRepository.existsByIdRole(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role está vinculado a um ou mais usuários e não pode ser removido!");
        }

        AuditoriaService.definirDetalhes("Role '" + role.getNome() + "' removido"
                + (role.getDescricao() != null ? " — " + role.getDescricao() : ""));
        roleRepository.delete(role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissoes")
    List<PermissaoResponse> listarPermissoes(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        return role.getPermissoes().stream().map(PermissaoResponse::de).toList();
    }

    @Auditavel(acao = AcaoAuditoria.ROLE_PERMISSOES_REDEFINIDAS)
    @PutMapping("/{id}/permissoes")
    Map<String, Object> redefinirPermissoes(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable Long id,
                                            @RequestBody List<Long> idsPermissoes) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        List<Permissao> permissoes = permissaoRepository.findAllByIdIn(idsPermissoes);
        role.setPermissoes(permissoes);
        roleRepository.save(role);

        String listaPermissoes = permissoes.stream()
                .map(p -> p.getRecurso() + ":" + p.getAcao())
                .collect(java.util.stream.Collectors.joining(", "));
        AuditoriaService.definirDetalhes("Role '" + role.getNome() + "' — permissões redefinidas para: ["
                + listaPermissoes + "]");
        return Map.of("mensagem", "Permissões do role atualizadas com sucesso!");
    }
}
