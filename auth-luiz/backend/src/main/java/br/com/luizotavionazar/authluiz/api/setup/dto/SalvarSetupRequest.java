package br.com.luizotavionazar.authluiz.api.setup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Configuração inicial da aplicação: SMTP, Twilio e preferências de auditoria")
public record SalvarSetupRequest(
        @Schema(description = "Servidor SMTP para envio de e-mails", example = "smtp.gmail.com")
        @NotBlank(message = "Host SMTP é obrigatório")
        String smtpHost,

        @Schema(description = "Porta do servidor SMTP", example = "587")
        @NotNull(message = "Porta SMTP é obrigatória")
        @Min(value = 1, message = "Porta SMTP inválida")
        @Max(value = 65535, message = "Porta SMTP inválida")
        Integer smtpPort,

        @Schema(description = "Usuário para autenticação SMTP", example = "seu@gmail.com")
        @NotBlank(message = "Usuário SMTP é obrigatório")
        String smtpUsername,

        @Schema(description = "Senha para autenticação SMTP")
        @NotBlank(message = "Senha SMTP é obrigatória")
        String smtpPassword,

        @Schema(description = "E-mail remetente exibido nas mensagens", example = "noreply@seudominio.com")
        @NotBlank(message = "E-mail remetente é obrigatório")
        @Email(message = "E-mail remetente inválido")
        String mailFrom,

        @Schema(description = "URL base do frontend (usada em links de e-mail)", example = "http://localhost")
        @NotBlank(message = "URL do frontend é obrigatória")
        String frontendBaseUrl,

        @Schema(description = "Habilitar STARTTLS no SMTP", example = "true")
        boolean smtpStarttls,

        @Schema(description = "Account SID do Twilio (opcional — necessário para SMS/WhatsApp)", nullable = true)
        String twilioAccountSid,

        @Schema(description = "Auth Token do Twilio (opcional)", nullable = true)
        String twilioAuthToken,

        @Schema(description = "Número remetente do Twilio em formato E.164", example = "+15005550006", nullable = true)
        String twilioFromNumber,

        @Schema(description = "Canal Twilio: SMS ou WHATSAPP", example = "WHATSAPP", nullable = true)
        String twilioCanal,

        @Schema(description = "Habilitar registro de logs de auditoria de atividade", example = "true", nullable = true)
        Boolean auditoriaAtividade,

        @Schema(description = "Dias de retenção dos logs de auditoria (1–3650)", example = "90", nullable = true)
        @Min(value = 1, message = "Retenção de auditoria deve ser de pelo menos 1 dia")
        @Max(value = 3650, message = "Retenção de auditoria não pode ultrapassar 3650 dias")
        Integer auditoriaRetencaoDias
) {
}
