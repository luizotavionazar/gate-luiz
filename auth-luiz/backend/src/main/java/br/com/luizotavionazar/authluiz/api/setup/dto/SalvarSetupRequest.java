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

        boolean smtpStarttls,

        String twilioAccountSid,
        String twilioAuthToken,
        String twilioFromNumber,
        String twilioCanal,

        Boolean auditoriaAtividade,

        @Min(value = 1, message = "Retenção de auditoria deve ser de pelo menos 1 dia")
        @Max(value = 3650, message = "Retenção de auditoria não pode ultrapassar 3650 dias")
        Integer auditoriaRetencaoDias
) {
}