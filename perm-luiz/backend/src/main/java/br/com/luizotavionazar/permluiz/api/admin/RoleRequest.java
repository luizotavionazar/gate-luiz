package br.com.luizotavionazar.permluiz.api.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou atualizar um role")
public record RoleRequest(
        @Schema(description = "Nome do role (convertido para maiúsculas)", example = "EDITOR")
        @NotBlank @Size(max = 100) String nome,

        @Schema(description = "Descrição opcional do role", example = "Pode editar artigos", nullable = true)
        @Size(max = 255) String descricao
) {
}
