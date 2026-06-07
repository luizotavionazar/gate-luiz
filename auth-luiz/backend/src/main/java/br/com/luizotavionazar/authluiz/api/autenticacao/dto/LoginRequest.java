package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para login local")
public record LoginRequest(

        @Schema(description = "E-mail (joao@email.com), telefone E.164 (+5511...) ou username (joao_silva)",
                example = "joao@email.com")
        @NotBlank(message = "Identificador é obrigatório")
        String identificador,

        @Schema(description = "Senha da conta", example = "MinhaSenha123!")
        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
    public enum TipoIdentificador { EMAIL, TELEFONE, USERNAME }

    public TipoIdentificador tipoIdentificador() {
        if (identificador == null) return TipoIdentificador.USERNAME;
        String id = identificador.trim();
        if (id.contains("@")) return TipoIdentificador.EMAIL;
        if (id.startsWith("+") || id.matches("\\+?\\d{8,15}")) return TipoIdentificador.TELEFONE;
        return TipoIdentificador.USERNAME;
    }

    public boolean isEmail() {
        return tipoIdentificador() == TipoIdentificador.EMAIL;
    }

    public String identificadorNormalizado() {
        if (identificador == null) return null;
        String id = identificador.trim();
        return switch (tipoIdentificador()) {
            case EMAIL    -> id.toLowerCase();
            case TELEFONE -> {
                String tel = id.replaceAll("[\\s().\\-]", "");
                yield tel.startsWith("+") ? tel : "+" + tel;
            }
            case USERNAME -> id.toLowerCase();
        };
    }
}
