package br.com.luizotavionazar.authluiz.api.conta.dto;

import jakarta.validation.constraints.NotBlank;

public record Desativar2faRequest(@NotBlank String senha) {}
