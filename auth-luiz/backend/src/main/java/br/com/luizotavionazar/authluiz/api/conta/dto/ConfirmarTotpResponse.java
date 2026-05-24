package br.com.luizotavionazar.authluiz.api.conta.dto;

import java.util.List;

public record ConfirmarTotpResponse(List<String> codigosBackup) {}
