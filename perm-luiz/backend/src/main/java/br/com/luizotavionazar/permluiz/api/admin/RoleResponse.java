package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.domain.role.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dados de um role com suas permissões")
public record RoleResponse(
        @Schema(description = "ID interno do role", example = "1")
        Long id,
        @Schema(description = "Nome do role em maiúsculas", example = "EDITOR")
        String nome,
        @Schema(description = "Descrição do role", example = "Pode editar artigos", nullable = true)
        String descricao,
        @Schema(description = "Lista de permissões associadas ao role")
        List<PermissaoResponse> permissions
) {
    public static RoleResponse de(Role role) {
        List<PermissaoResponse> perms = role.getPermissoes() == null
                ? List.of()
                : role.getPermissoes().stream().map(PermissaoResponse::de).toList();
        return new RoleResponse(role.getId(), role.getNome(), role.getDescricao(), perms);
    }
}
