package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

import java.time.LocalDateTime;

public record ContaResponse(
        Integer idUsuario,
        String nome,
        String email,
        Boolean temSenhaLocal,
        Boolean temLoginGoogle,
        Boolean emailVerificado,
        String emailPendente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualiza
) {
    public static ContaResponse from(Usuario usuario, boolean temLoginGoogle) {
        return new ContaResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.possuiSenhaLocal(),
                temLoginGoogle,
                usuario.isEmailVerificado(),
                usuario.getEmailPendente(),
                usuario.getDataCriacao(),
                usuario.getDataAtualiza()
        );
    }
}
