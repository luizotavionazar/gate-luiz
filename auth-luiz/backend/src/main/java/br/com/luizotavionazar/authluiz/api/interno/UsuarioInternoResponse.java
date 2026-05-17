package br.com.luizotavionazar.authluiz.api.interno;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioInternoResponse(
        String publicId,
        String nome,
        String email,
        String telefone,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualiza,
        LocalDateTime ultimoLogin,
        boolean emailVerificado,
        boolean telefoneVerificado,
        boolean possuiSenha,
        boolean googleVinculado
) {
    public static UsuarioInternoResponse de(Usuario u, boolean googleVinculado) {
        return new UsuarioInternoResponse(
                u.getPublicId(),
                u.getNome(),
                u.getEmail(),
                u.getTelefone(),
                u.getDataCriacao(),
                u.getDataAtualiza(),
                u.getUltimoLogin(),
                u.isEmailVerificado(),
                u.isTelefoneVerificado(),
                u.possuiSenha(),
                googleVinculado
        );
    }
}
