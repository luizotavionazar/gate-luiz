package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record ReenviarVerificacaoRequest(
        @NotBlank String tokenPendente,
        String canal
) {}
