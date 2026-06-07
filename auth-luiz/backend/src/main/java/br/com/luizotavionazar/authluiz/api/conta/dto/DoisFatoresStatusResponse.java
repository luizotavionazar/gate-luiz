package br.com.luizotavionazar.authluiz.api.conta.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status atual da configuração de dois fatores do usuário")
public record DoisFatoresStatusResponse(
        @Schema(description = "Se o TOTP (autenticador) está ativo")
        boolean totpAtivo,
        @Schema(description = "Quantidade de backup codes não utilizados (0–8)", example = "8")
        int codigosRestantes,
        @Schema(description = "Se a verificação extra está habilitada (exige 2FA em todos os logins de IPs desconhecidos)")
        boolean verificacaoExtraAtiva
) {}
