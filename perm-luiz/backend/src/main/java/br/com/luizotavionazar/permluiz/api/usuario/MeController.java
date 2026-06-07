package br.com.luizotavionazar.permluiz.api.usuario;

import br.com.luizotavionazar.permluiz.api.admin.RoleResponse;
import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.usuariorole.UsuarioRoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Tag(name = "Minha Conta")
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UsuarioRoleRepository usuarioRoleRepository;
    private final AdminVerificador adminVerificador;

    @Operation(summary = "Verificar se é admin mestre",
            description = "Retorna `{ isAdmin: true/false }`. " +
                    "Se nenhum admin estiver configurado, o usuário atual é automaticamente promovido a admin.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Status de admin")
    @GetMapping("/admin")
    Map<String, Object> isAdmin(@AuthenticationPrincipal Jwt jwt) {
        try {
            adminVerificador.exigirAdmin(jwt);
            return Map.of("isAdmin", true);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return Map.of("isAdmin", false);
            }
            throw e;
        }
    }

    @Operation(summary = "Resetar admin mestre",
            description = "Limpa o `idAdminMestre` salvo. Apenas o admin atual pode chamar este endpoint. " +
                    "Usado pelo AuthLuiz quando o admin exclui sua conta.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin resetado"),
            @ApiResponse(responseCode = "403", description = "Apenas o admin atual pode resetar", content = @Content)
    })
    @DeleteMapping("/admin")
    void resetarAdmin(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.resetarAdmin(jwt);
    }

    @Operation(summary = "Listar roles e permissões do usuário autenticado",
            description = "Retorna o `idUsuario` e a lista de roles com suas permissões atribuídas ao usuário do JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Roles e permissões do usuário")
    @GetMapping("/roles")
    Map<String, Object> meusRoles(@AuthenticationPrincipal Jwt jwt) {
        String idUsuario = adminVerificador.extrairIdUsuario(jwt);
        List<RoleResponse> roles = usuarioRoleRepository
                .findByIdUsuarioWithRolesAndPermissoes(idUsuario)
                .stream()
                .map(ur -> RoleResponse.de(ur.getRole()))
                .toList();

        return Map.of("idUsuario", idUsuario, "roles", roles);
    }
}
