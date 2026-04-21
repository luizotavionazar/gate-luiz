package br.com.luizotavionazar.permluiz.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissaoRequest(
        @NotBlank @Size(max = 100) String recurso,
        @NotBlank @Size(max = 50) String acao,
        @Size(max = 255) String descricao
) {
}
