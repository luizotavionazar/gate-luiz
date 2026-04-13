package br.com.luizotavionazar.authluiz.api.setup.dto;

import jakarta.validation.constraints.*;

public record SalvarSetupRequest(
        @NotBlank(message = "Host SMTP é obrigatório")
        String smtpHost,

        @NotNull(message = "Porta SMTP é obrigatória")
        @Min(value = 1, message = "Porta SMTP inválida")
        @Max(value = 65535, message = "Porta SMTP inválida")
        Integer smtpPort,

        @NotBlank(message = "Usuário SMTP é obrigatório")
        String smtpUsername,

        @NotBlank(message = "Senha SMTP é obrigatória")
        String smtpPassword,

        @NotBlank(message = "E-mail remetente é obrigatório")
        @Email(message = "E-mail remetente inválido")
        String mailFrom,

        @NotBlank(message = "URL do frontend é obrigatória")
        String frontendBaseUrl,

        boolean smtpAuth,
        boolean smtpStarttls,
        boolean confirmacaoEmailHabilitada
) {
}