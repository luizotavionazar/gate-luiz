package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.PoliticaSenha;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(

        @Email(message = "E-mail inválido")
        String email,

        @Pattern(regexp = "\\+?\\d{8,15}", message = "Telefone inválido")
        String telefone,

        @NotBlank(message = "Código é obrigatório")
        @Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos numéricos")
        String codigo,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(
                min = PoliticaSenha.MIN_CARACTERES,
                max = PoliticaSenha.MAX_CARACTERES,
                message = "A senha deve ter entre " + PoliticaSenha.MIN_CARACTERES
                        + " e " + PoliticaSenha.MAX_CARACTERES + " caracteres"
        )
        String novaSenha

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
