package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AtualizarPreferencia2faRequest(
        @NotBlank @Pattern(regexp = "EMAIL|SMS|WHATSAPP") String canal
) {}
