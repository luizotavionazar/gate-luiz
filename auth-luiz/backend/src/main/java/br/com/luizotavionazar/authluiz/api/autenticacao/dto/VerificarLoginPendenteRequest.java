package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificarLoginPendenteRequest(
        @NotBlank String tokenPendente,
        @NotBlank @Size(min = 6, max = 9) String codigo,
        boolean confiarEsteIp,
        String rotuloDispositivo
) {}
