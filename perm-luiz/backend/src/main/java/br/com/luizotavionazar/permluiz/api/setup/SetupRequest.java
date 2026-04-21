package br.com.luizotavionazar.permluiz.api.setup;

import jakarta.validation.constraints.NotNull;

public record SetupRequest(@NotNull Long idUsuario) {
}
