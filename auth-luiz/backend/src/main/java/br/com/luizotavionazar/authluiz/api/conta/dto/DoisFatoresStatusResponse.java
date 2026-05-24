package br.com.luizotavionazar.authluiz.api.conta.dto;

public record DoisFatoresStatusResponse(
        boolean totpAtivo,
        int codigosRestantes,
        boolean verificacaoExtraAtiva
) {}
