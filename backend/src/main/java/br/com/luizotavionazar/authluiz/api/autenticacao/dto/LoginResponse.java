package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

public record LoginResponse(
        Integer idUsuario,
        String nome,
        String email,
        Boolean temSenhaLocal,
        Boolean temLoginGoogle,
        Boolean emailVerificado,
        String token,
        String tokenType,
        Long expiresInMinutes,
        String mensagem
) {
    public static LoginResponse from(Usuario usuario, boolean temLoginGoogle, String token, long expiresInMinutes) {
        return new LoginResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.possuiSenhaLocal(),
                temLoginGoogle,
                usuario.isEmailVerificado(),
                token,
                "Bearer",
                expiresInMinutes,
                "Login realizado com sucesso"
        );
    }
}
