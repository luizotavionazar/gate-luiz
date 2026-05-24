package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarTotpRequest(
        @NotBlank @Size(min = 6, max = 6) String codigo
) {}
