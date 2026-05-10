package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmarEmailRequest(

        @NotBlank(message = "Código é obrigatório")
        @Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos numéricos")
        String codigo
) {
}
