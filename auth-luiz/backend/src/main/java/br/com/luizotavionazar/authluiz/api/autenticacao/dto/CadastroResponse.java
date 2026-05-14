package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

public record CadastroResponse(
        Integer idUsuario,
        String nome,
        String email,
        String mensagem
) {
    public static CadastroResponse from(Usuario usuario) {
        return new CadastroResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                "Conta criada! Verifique seu e-mail para ativar a conta."
        );
    }
}
