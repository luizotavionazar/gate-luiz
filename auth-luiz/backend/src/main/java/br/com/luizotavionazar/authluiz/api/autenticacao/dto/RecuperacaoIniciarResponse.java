package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

public record RecuperacaoIniciarResponse(
        String mensagem,
        boolean sugestaoLoginGoogle
) {}
