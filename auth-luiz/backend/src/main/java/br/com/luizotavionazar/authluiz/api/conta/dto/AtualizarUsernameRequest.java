package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarUsernameRequest(
        @NotBlank(message = "Username é obrigatório")
        @Size(min = 4, max = 30, message = "Username deve ter entre 4 e 30 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z][a-zA-Z0-9._]{3,29}$",
                message = "Username deve começar com letra e conter apenas letras, números, ponto e underscore"
        )
        String username
) {
    public String usernameSanitizado() {
        return username == null ? null : username.strip().toLowerCase();
    }
}
