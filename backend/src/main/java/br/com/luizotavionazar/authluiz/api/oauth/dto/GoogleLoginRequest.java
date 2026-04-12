package br.com.luizotavionazar.authluiz.api.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "O idToken do Google é obrigatório")
        String idToken,

        Boolean vincularContaExistente,

        String senhaContaExistente
) {
    public boolean desejaVincularContaExistente() {
        return Boolean.TRUE.equals(vincularContaExistente);
    }
}
