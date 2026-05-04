package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.permluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.permluiz.domain.role.RoleRepository;
import br.com.luizotavionazar.permluiz.domain.role.entity.Role;
import br.com.luizotavionazar.permluiz.domain.usuariorole.UsuarioRoleRepository;
import br.com.luizotavionazar.permluiz.domain.usuariorole.entity.UsuarioRole;
import br.com.luizotavionazar.permluiz.domain.usuariorole.entity.UsuarioRoleId;
import br.com.luizotavionazar.permluiz.infra.AuthLuizClient;
import br.com.luizotavionazar.permluiz.infra.UsuarioAuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioRoleRepository usuarioRoleRepository;
    private final RoleRepository roleRepository;
    private final AdminVerificador adminVerificador;
    private final AuthLuizClient authLuizClient;

    @GetMapping
    List<UsuarioComRolesResponse> listarUsuarios(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.exigirAdmin(jwt);

        List<UsuarioAuthResponse> usuarios = authLuizClient.buscarTodosUsuarios();

        Map<Long, List<RoleResponse>> rolesPorUsuario = usuarioRoleRepository.findAllWithRoles().stream()
                .collect(Collectors.groupingBy(
                        UsuarioRole::getIdUsuario,
                        Collectors.mapping(ur -> RoleResponse.de(ur.getRole()), Collectors.toList())
                ));

        return usuarios.stream()
                .map(u -> new UsuarioComRolesResponse(
                        u.id(),
                        u.nome(),
                        u.email(),
                        u.telefone(),
                        u.dataCriacao(),
                        u.dataAtualiza(),
                        u.ultimoLogin(),
                        u.emailVerificado(),
                        u.telefoneVerificado(),
                        u.possuiSenha(),
                        u.googleVinculado(),
                        rolesPorUsuario.getOrDefault(u.id(), List.of())
                ))
                .toList();
    }

    @GetMapping("/{idUsuario}/roles")
    List<RoleResponse> listarRoles(@AuthenticationPrincipal Jwt jwt, @PathVariable Long idUsuario) {
        adminVerificador.exigirAdmin(jwt);
        return usuarioRoleRepository.findByIdUsuario(idUsuario).stream()
                .map(ur -> RoleResponse.de(ur.getRole()))
                .toList();
    }

    @Auditavel(acao = AcaoAuditoria.ROLE_USUARIO_ATRIBUIDA)
    @PostMapping("/{idUsuario}/roles/{idRole}")
    ResponseEntity<Map<String, Object>> atribuirRole(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long idUsuario,
                                                     @PathVariable Long idRole) {
        adminVerificador.exigirAdmin(jwt);

        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));

        if (usuarioRoleRepository.existsById(new UsuarioRoleId(idUsuario, idRole))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já possui esse role!");
        }

        UsuarioRole usuarioRole = new UsuarioRole();
        usuarioRole.setIdUsuario(idUsuario);
        usuarioRole.setIdRole(idRole);
        usuarioRole.setAtribuidoPor(adminVerificador.extrairIdUsuario(jwt));
        usuarioRoleRepository.save(usuarioRole);

        AuditoriaService.definirDetalhes("Role '" + role.getNome() + "' atribuído ao usuário #" + idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("mensagem", "Role atribuído com sucesso!", "role", role.getNome())
        );
    }

    @Auditavel(acao = AcaoAuditoria.ROLE_USUARIO_REMOVIDA)
    @DeleteMapping("/{idUsuario}/roles/{idRole}")
    ResponseEntity<Void> removerRole(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long idUsuario,
                                     @PathVariable Long idRole) {
        adminVerificador.exigirAdmin(jwt);

        if (!usuarioRoleRepository.existsById(new UsuarioRoleId(idUsuario, idRole))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não possui esse role!");
        }
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role não encontrado!"));
        AuditoriaService.definirDetalhes("Role '" + role.getNome() + "' removido do usuário #" + idUsuario);
        usuarioRoleRepository.deleteByIdUsuarioAndIdRole(idUsuario, idRole);
        return ResponseEntity.noContent().build();
    }
}
