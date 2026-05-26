package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.PoliticaSenha;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "Username é obrigatório")
        @Size(min = 4, max = 30, message = "Username deve ter entre 4 e 30 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z][a-zA-Z0-9._]{3,29}$",
                message = "Username deve começar com letra e conter apenas letras, números, ponto e underscore"
        )
        String username,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(
                min = PoliticaSenha.MIN_CARACTERES,
                max = PoliticaSenha.MAX_CARACTERES,
                message = "A senha deve ter entre " + PoliticaSenha.MIN_CARACTERES
                        + " e " + PoliticaSenha.MAX_CARACTERES + " caracteres"
        )
        String senha,

        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Telefone deve estar no formato internacional (ex: +5511987654321)")
        String telefone,

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome
) {
    public String emailNormalizado() {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String usernameNormalizado() {
        return username == null ? null : username.strip().toLowerCase();
    }

    public String nomeNormalizado() {
        return nome == null ? null : nome.strip();
    }
}
