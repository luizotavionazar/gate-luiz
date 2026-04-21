package br.com.luizotavionazar.permluiz.api.admin;

import br.com.luizotavionazar.permluiz.config.security.AdminVerificador;
import br.com.luizotavionazar.permluiz.domain.permissao.PermissaoRepository;
import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/permissoes")
@RequiredArgsConstructor
public class AdminPermissaoController {

    private final PermissaoRepository permissaoRepository;
    private final AdminVerificador adminVerificador;

    @GetMapping
    List<PermissaoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        adminVerificador.exigirAdmin(jwt);
        return permissaoRepository.findAll().stream().map(PermissaoResponse::de).toList();
    }

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

        return ResponseEntity.status(HttpStatus.CREATED).body(PermissaoResponse.de(permissao));
    }

    @PutMapping("/{id}")
    PermissaoResponse atualizar(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable Long id,
                                @RequestBody @Valid PermissaoRequest request) {
        adminVerificador.exigirAdmin(jwt);

        Permissao permissao = permissaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada!"));

        permissao.setRecurso(request.recurso().toLowerCase());
        permissao.setAcao(request.acao().toLowerCase());
        permissao.setDescricao(request.descricao());
        permissaoRepository.save(permissao);

        return PermissaoResponse.de(permissao);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVerificador.exigirAdmin(jwt);

        if (!permissaoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissão não encontrada!");
        }
        permissaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
