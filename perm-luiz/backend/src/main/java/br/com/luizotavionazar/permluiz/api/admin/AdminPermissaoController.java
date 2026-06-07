package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.config.auditoria.Auditavel;
import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.permluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.permluiz.domain.permissao.PermissaoRepository;
import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import br.com.luizotavionazar.permluiz.domain.role.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Admin - Permissões")
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissaoController {

    private final PermissaoRepository permissaoRepository;
    private final RoleRepository roleRepository;
    private final AdminVerificador adminVerificador;

    @Operation(summary = "Listar todas as permissões",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de permissões"),
            @ApiResponse(responseCode = "403", description = "Não é admin mestre", content = @Content)
    })
    @GetMapping
    List<PermissaoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.exigirAdmin(jwt);
        return permissaoRepository.findAll().stream().map(PermissaoResponse::de).toList();
    }

    @Operation(summary = "Criar permissão",
            description = """
                    Cria uma nova permissão. Recurso e ação são convertidos para minúsculas. \
                    A combinação `recurso+acao` deve ser única.

                    **Próximo passo após o 201:** a permissão criada ainda não está associada a nenhum role. \
                    Use o `id` retornado e chame `PUT /admin/roles/{id}/permissions` \
                    passando a lista de IDs de permissões para associá-la a um role.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Permissão criada — próximo passo: associar a um role via `PUT /admin/roles/{id}/permissions`"),
            @ApiResponse(responseCode = "409", description = "Permissão com mesmo recurso+ação já existe", content = @Content),
            @ApiResponse(responseCode = "403", description = "Não é admin mestre", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.PERMISSAO_CRIADA)
    @PostMapping
    ResponseEntity<PermissaoResponse> criar(@AuthenticationPrincipal Jwt jwt,
                                            @RequestBody @Valid PermissaoRequest request) {
        adminVerificador.exigirAdmin(jwt);

        if (permissaoRepository.existsByRecursoAndAcao(request.recurso(), request.acao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma permissão com esse recurso e ação!");
        }

        Permissao permissao = new Permissao();
        permissao.setRecurso(request.recurso().toLowerCase());
        permissao.setAcao(request.acao().toLowerCase());
        permissao.setDescricao(request.descricao());
        permissaoRepository.save(permissao);

        AuditoriaService.definirDetalhes("Permissão '" + permissao.getRecurso() + ":" + permissao.getAcao() + "'"
                + (permissao.getDescricao() != null ? " — " + permissao.getDescricao() : ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissaoResponse.de(permissao));
    }

    @Operation(summary = "Atualizar permissão",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissão atualizada"),
            @ApiResponse(responseCode = "404", description = "Permissão não encontrada", content = @Content),
            @ApiResponse(responseCode = "403", description = "Não é admin mestre", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.PERMISSAO_ATUALIZADA)
    @PutMapping("/{id}")
    PermissaoResponse atualizar(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable Long id,
                                @RequestBody @Valid PermissaoRequest request) {
        adminVerificador.exigirAdmin(jwt);

        Permissao permissao = permissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada!"));

        String chaveAnterior = permissao.getRecurso() + ":" + permissao.getAcao();
        String descricaoAnterior = permissao.getDescricao();
        permissao.setRecurso(request.recurso().toLowerCase());
        permissao.setAcao(request.acao().toLowerCase());
        permissao.setDescricao(request.descricao());
        permissaoRepository.save(permissao);

        AuditoriaService.definirDetalhes("Permissão atualizada: '" + chaveAnterior + "' → '"
                + permissao.getRecurso() + ":" + permissao.getAcao() + "'"
                + " | descrição '" + descricaoAnterior + "' → '" + permissao.getDescricao() + "'");
        return PermissaoResponse.de(permissao);
    }

    @Operation(summary = "Remover permissão",
            description = "Remove a permissão se não estiver vinculada a nenhum role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permissão removida"),
            @ApiResponse(responseCode = "404", description = "Permissão não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Permissão está vinculada a roles", content = @Content),
            @ApiResponse(responseCode = "403", description = "Não é admin mestre", content = @Content)
    })
    @Auditavel(acao = AcaoAuditoria.PERMISSAO_DELETADA)
    @DeleteMapping("/{id}")
    ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        Permissao permissao = permissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada!"));

        if (roleRepository.existsByPermissoesId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Permissão está vinculada a um ou mais roles e não pode ser removida!");
        }

        AuditoriaService.definirDetalhes("Permissão '" + permissao.getRecurso() + ":" + permissao.getAcao() + "' removida"
                + (permissao.getDescricao() != null ? " — " + permissao.getDescricao() : ""));
        permissaoRepository.delete(permissao);
        return ResponseEntity.noContent().build();
    }
}
