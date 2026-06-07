package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de uma permissão")
public record PermissaoResponse(
        @Schema(description = "ID interno da permissão", example = "1")
        Long id,
        @Schema(description = "Recurso em minúsculas", example = "artigos")
        String recurso,
        @Schema(description = "Ação em minúsculas", example = "editar")
        String acao,
        @Schema(description = "Descrição da permissão", nullable = true, example = "Permite editar artigos publicados")
        String descricao) {

    public static PermissaoResponse de(Permissao p) {
        return new PermissaoResponse(p.getId(), p.getRecurso(), p.getAcao(), p.getDescricao());
    }
}
