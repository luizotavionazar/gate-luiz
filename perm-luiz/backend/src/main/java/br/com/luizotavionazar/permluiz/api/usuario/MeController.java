package br.com.luizotavionazar.permluiz.api.usuario;

import br.com.luizotavionazar.permluiz.api.admin.RoleResponse;
import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.usuariorole.UsuarioRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UsuarioRoleRepository usuarioRoleRepository;
    private final AdminVerificador adminVerificador;

    @GetMapping("/admin")
    Map<String, Object> isAdmin(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("isAdmin", adminVerificador.isAdmin(jwt));
    }

    @GetMapping("/roles")
    Map<String, Object> meusRoles(@AuthenticationPrincipal Jwt jwt) {
        Long idUsuario = adminVerificador.extrairIdUsuario(jwt);
        List<RoleResponse> roles = usuarioRoleRepository
                .findByIdUsuarioWithRolesAndPermissoes(idUsuario)
                .stream()
                .map(ur -> RoleResponse.de(ur.getRole()))
                .toList();

        return Map.of("idUsuario", idUsuario, "roles", roles);
    }
}
