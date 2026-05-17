package br.com.luizotavionazar.authluiz.domain.autenticacao.event;

public record UsuarioCadastradoEvent(
        String publicId,
        String nome,
        String email
) {
}