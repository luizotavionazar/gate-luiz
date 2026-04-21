package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.permissao.PermissaoRepository;
import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import br.com.luizotavionazar.permluiz.domain.role.RoleRepository;
import br.com.luizotavionazar.permluiz.domain.role.entity.Role;
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
    private final AdminVerificador adminVerificador;

    @GetMapping
    List<RoleResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.exigirAdmin(jwt);
        return roleRepository.findAll().stream().map(RoleResponse::de).toList();
    }

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

        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.de(role));
    }

    @PutMapping("/{id}")
    RoleResponse atualizar(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable Long id,
                           @RequestBody @Valid RoleRequest request) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        role.setNome(request.nome().toUpperCase());
        role.setDescricao(request.descricao());
        roleRepository.save(role);

        return RoleResponse.de(role);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        if (!roleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!");
        }
        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissoes")
    List<PermissaoResponse> listarPermissoes(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        return role.getPermissoes().stream().map(PermissaoResponse::de).toList();
    }

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

        return Map.of("mensagem", "Permissões do role atualizadas com sucesso!");
    }
}
