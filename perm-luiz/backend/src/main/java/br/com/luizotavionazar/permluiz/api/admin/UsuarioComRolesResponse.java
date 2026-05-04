package br.com.luizotavionazar.permluiz.api.admin;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioComRolesResponse(
        Long idUsuario,
        String nome,
        String email,
        String telefone,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualiza,
        LocalDateTime ultimoLogin,
        boolean emailVerificado,
        boolean telefoneVerificado,
        boolean possuiSenha,
        boolean googleVinculado,
        List<RoleResponse> roles
) {}
