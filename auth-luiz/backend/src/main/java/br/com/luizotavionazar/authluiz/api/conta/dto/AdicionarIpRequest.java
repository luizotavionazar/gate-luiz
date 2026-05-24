package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.Size;

public record AdicionarIpRequest(
        @Size(max = 45) String ip,
        @Size(max = 100) String rotulo
) {}
