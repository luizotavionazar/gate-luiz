package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.Pattern;

public record AtualizarTelefoneRequest(
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Telefone deve estar no formato internacional (ex: +5511987654321)")
        String telefone
) {}
