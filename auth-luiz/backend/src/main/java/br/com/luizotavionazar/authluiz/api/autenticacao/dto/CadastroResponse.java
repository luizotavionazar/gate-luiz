package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

public record CadastroResponse(
        String publicId,
        String nome,
        String email,
        String mensagem
) {
    public static CadastroResponse from(Usuario usuario) {
        return new CadastroResponse(
                usuario.getPublicId(),
                usuario.getNome(),
                usuario.getEmail(),
                "Conta criada! Verifique seu e-mail para ativar a conta."
        );
    }
}
