package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

public record MensagemResponse(
        String mensagem,
        boolean sugestaoLoginGoogle
) {
    public MensagemResponse(String mensagem) {
        this(mensagem, false);
    }
}
