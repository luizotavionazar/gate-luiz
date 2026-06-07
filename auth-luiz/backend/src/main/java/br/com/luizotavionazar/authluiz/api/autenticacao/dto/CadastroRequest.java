package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.PoliticaSenha;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação de uma nova conta")
public record CadastroRequest(
        @Schema(description = "Username público único (4–30 chars; letras, números, ponto e underscore; começa com letra)", example = "joao_silva")
        @NotBlank(message = "Username é obrigatório")
        @Size(min = 4, max = 30, message = "Username deve ter entre 4 e 30 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z][a-zA-Z0-9._]{3,29}$",
                message = "Username deve começar com letra e conter apenas letras, números, ponto e underscore"
        )
        String username,

        @Schema(description = "E-mail da conta (máx. 255 chars)", example = "joao@email.com")
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @Schema(description = "Senha da conta (8–128 chars)", example = "MinhaSenha123!")
        @NotBlank(message = "Senha é obrigatória")
        @Size(
                min = PoliticaSenha.MIN_CARACTERES,
                max = PoliticaSenha.MAX_CARACTERES,
                message = "A senha deve ter entre " + PoliticaSenha.MIN_CARACTERES
                        + " e " + PoliticaSenha.MAX_CARACTERES + " caracteres"
        )
        String senha,

        @Schema(description = "Telefone opcional em formato E.164", example = "+5511987654321", nullable = true)
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Telefone deve estar no formato internacional (ex: +5511987654321)")
        String telefone,

        @Schema(description = "Nome de exibição (3–100 chars)", example = "João Silva")
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
