package br.com.luizotavionazar.permluiz.api.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou atualizar uma permissão")
public record PermissaoRequest(
        @Schema(description = "Recurso ao qual a permissão se aplica (convertido para minúsculas)", example = "artigos")
        @NotBlank @Size(max = 100) String recurso,

        @Schema(description = "Ação permitida sobre o recurso (convertida para minúsculas)", example = "editar")
        @NotBlank @Size(max = 50) String acao,

        @Schema(description = "Descrição opcional da permissão", example = "Permite editar artigos publicados", nullable = true)
        @Size(max = 255) String descricao
) {
}
