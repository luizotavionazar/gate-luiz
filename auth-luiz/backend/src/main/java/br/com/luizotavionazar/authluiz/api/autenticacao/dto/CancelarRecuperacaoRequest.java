package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelarRecuperacaoRequest(
        @NotBlank(message = "Token de cancelamento é obrigatório")
        String tokenCancelamento
) {}
