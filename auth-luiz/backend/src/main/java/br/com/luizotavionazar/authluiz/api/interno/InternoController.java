package br.com.luizotavionazar.authluiz.api.interno;

import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.CodigoBackup2faRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.UsuarioIPConfiavelRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LogoutService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

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

    @GetMapping("/tokens/{jti}/invalido")
    boolean tokenEstaInvalidado(@PathVariable String jti, @RequestHeader("X-Service-Key") String chave) {
        if (!serviceKey.equals(chave)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave de serviço inválida!");
        }
        return logoutService.estaInvalidado(jti);
    }

    @GetMapping("/usuarios")
    List<UsuarioInternoResponse> listarUsuarios(@RequestHeader("X-Service-Key") String chave) {
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
