package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para verificar o segundo fator e concluir o login pendente")
public record VerificarLoginPendenteRequest(
        @Schema(description = "Token opaco recebido na resposta 202 do login", example = "a3f9b2c1d4e5...")
        @NotBlank String tokenPendente,

        @Schema(description = "Código TOTP (6 dígitos) ou OTP por e-mail/SMS (6 dígitos). " +
                "Para TOTP use apenas os 6 dígitos. Para backup code use formato XXXX-XXXX.",
                example = "123456")
        @NotBlank @Size(min = 6, max = 9) String codigo,

        @Schema(description = "Se true, adiciona o IP atual como confiável (não exigirá 2FA em logins futuros deste IP)",
                example = "false")
        boolean confiarEsteIp,

        @Schema(description = "Rótulo opcional para identificar o dispositivo na lista de IPs confiáveis",
                example = "Notebook do trabalho", nullable = true)
        String rotuloDispositivo
) {}
