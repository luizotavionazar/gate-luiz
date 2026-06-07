package br.com.luizotavionazar.authluiz.api.interno;

import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.CodigoBackup2faRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.UsuarioIPConfiavelRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LogoutService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Tag(name = "Interno")
@RestController
@RequestMapping("/auth/interno")
@RequiredArgsConstructor
public class InternoController {

    private final UsuarioRepository usuarioRepository;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final LogoutService logoutService;
    private final CodigoBackup2faRepository codigoBackup2faRepository;
    private final UsuarioIPConfiavelRepository usuarioIPConfiavelRepository;

    @Value("${auth.service.key}")
    private String serviceKey;

    @Operation(summary = "Verificar se token está na blacklist",
            description = "Retorna `true` se o JWT com o `jti` informado foi invalidado por logout. " +
                    "Usado pelo PermLuiz para validar tokens antes de processar requisições.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "true se inválido, false se válido"),
            @ApiResponse(responseCode = "401", description = "X-Service-Key inválida", content = @Content)
    })
    @GetMapping("/tokens/{jti}/invalido")
    boolean tokenEstaInvalidado(
            @PathVariable String jti,
            @Parameter(description = "Chave de serviço (`AUTH_LUIZ_SERVICE_KEY`)", required = true)
            @RequestHeader("X-Service-Key") String chave) {
        if (!serviceKey.equals(chave)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave de serviço inválida!");
        }
        return logoutService.estaInvalidado(jti);
    }

    @Operation(summary = "Verificar se usuário existe",
            description = "Retorna `true` se o usuário com o publicId informado existe. Usado pelo PermLuiz antes de atribuir roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "true se existe, false se não existe"),
            @ApiResponse(responseCode = "401", description = "X-Service-Key inválida", content = @Content)
    })
    @GetMapping("/usuarios/{publicId}/existe")
    boolean usuarioExiste(
            @PathVariable String publicId,
            @Parameter(description = "Chave de serviço (`AUTH_LUIZ_SERVICE_KEY`)", required = true)
            @RequestHeader("X-Service-Key") String chave) {
        if (!serviceKey.equals(chave)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave de serviço inválida!");
        }
        return usuarioRepository.findByPublicId(publicId).isPresent();
    }

    @Operation(summary = "Listar todos os usuários",
            description = "Retorna dados completos de todos os usuários cadastrados. " +
                    "Usado pelo PermLuiz para montar a lista de usuários no painel admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários"),
            @ApiResponse(responseCode = "401", description = "X-Service-Key inválida", content = @Content)
    })
    @GetMapping("/usuarios")
    List<UsuarioInternoResponse> listarUsuarios(
            @Parameter(description = "Chave de serviço (`AUTH_LUIZ_SERVICE_KEY`)", required = true)
            @RequestHeader("X-Service-Key") String chave) {
        if (!serviceKey.equals(chave)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave de serviço inválida!");
        }
        Set<Integer> idsComGoogle = identidadeExternaRepository.findUsuarioIdsByProvider(ProviderExterno.GOOGLE);
        return usuarioRepository.findAll().stream()
                .map(u -> UsuarioInternoResponse.de(
                        u,
                        idsComGoogle.contains(u.getId()),
                        codigoBackup2faRepository.countByIdUsuarioAndUsadoEmIsNull(u.getId()),
                        usuarioIPConfiavelRepository.countByIdUsuario(u.getId())
                ))
                .toList();
    }
}
