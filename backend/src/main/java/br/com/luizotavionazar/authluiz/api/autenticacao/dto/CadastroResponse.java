package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

public record CadastroResponse(
        Integer idUsuario,
        String nome,
        String email,
        Boolean emailVerificado,
        String mensagem
) {
    public static CadastroResponse from(Usuario usuario) {
        String mensagem = usuario.isEmailVerificado()
                ? "Conta criada com sucesso"
                : "Conta criada! Verifique seu e-mail para ativar a conta.";
        return new CadastroResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.isEmailVerificado(),
                mensagem
        );
    }
}
