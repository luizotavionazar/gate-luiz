package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarEmailRequest(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email
) {
    public String emailNormalizado() {
        return email == null ? null : email.trim().toLowerCase();
    }
}
