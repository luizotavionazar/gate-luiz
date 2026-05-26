package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

public record LoginResponse(
        String publicId,
        String username,
        String nome,
        String email,
        Boolean temSenha,
        Boolean temLoginGoogle,
        Boolean emailVerificado,
        String telefone,
        Boolean telefoneVerificado,
        String token,
        String tokenType,
        Long expiresInMinutes,
        String mensagem) {
    public static LoginResponse from(Usuario usuario, boolean temLoginGoogle, String token, long expiresInMinutes) {
        return new LoginResponse(
                usuario.getPublicId(),
                usuario.getUsername(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.possuiSenha(),
                temLoginGoogle,
                usuario.isEmailVerificado(),
                usuario.getTelefone(),
                usuario.isTelefoneVerificado(),
                token,
                "Bearer",
                expiresInMinutes,
                "Login realizado com sucesso");
    }
}
