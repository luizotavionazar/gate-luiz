package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "E-mail ou telefone é obrigatório")
        String identificador,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
    public boolean isEmail() {
        return identificador != null && identificador.contains("@");
    }

    public String identificadorNormalizado() {
        if (identificador == null) return null;
        String id = identificador.trim();
        if (isEmail()) return id.toLowerCase();
        String tel = id.replaceAll("[\\s().\\-]", "");
        return tel.startsWith("+") ? tel : "+" + tel;
    }
}
