package br.com.luizotavionazar.authluiz.domain.autenticacao.event;

public record UsuarioCadastradoEvent(
        Integer idUsuario,
        String nome,
        String email
) {
}