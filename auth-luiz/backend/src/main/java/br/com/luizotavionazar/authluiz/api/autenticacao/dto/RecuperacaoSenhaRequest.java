package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record RecuperacaoSenhaRequest(

        @Email(message = "E-mail inválido")
        String email,

        @Pattern(regexp = "\\+?\\d{8,15}", message = "Telefone inválido")
        String telefone

) {
    @AssertTrue(message = "Informe e-mail ou telefone (não ambos)")
    public boolean isIdentificadorValido() {
        boolean temEmail = email != null && !email.isBlank();
        boolean temTelefone = telefone != null && !telefone.isBlank();
        return temEmail ^ temTelefone;
    }

    public boolean usarEmail() {
        return email != null && !email.isBlank();
    }

    public String emailNormalizado() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String telefoneNormalizado() {
        return telefone == null ? null : telefone.trim();
    }
}
