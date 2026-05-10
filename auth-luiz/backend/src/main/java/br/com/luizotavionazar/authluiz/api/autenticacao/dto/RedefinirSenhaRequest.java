package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.PoliticaSenha;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

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
    public String emailNormalizado() {
        return email.trim().toLowerCase();
    }
}
