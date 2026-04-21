package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.domain.role.entity.Role;

import java.util.List;

public record RoleResponse(
        Long id,
        String nome,
        String descricao,
        List<PermissaoResponse> permissoes
) {
    public static RoleResponse de(Role role) {
        List<PermissaoResponse> perms = role.getPermissoes() == null
                ? List.of()
                : role.getPermissoes().stream().map(PermissaoResponse::de).toList();
        return new RoleResponse(role.getId(), role.getNome(), role.getDescricao(), perms);
    }
}
