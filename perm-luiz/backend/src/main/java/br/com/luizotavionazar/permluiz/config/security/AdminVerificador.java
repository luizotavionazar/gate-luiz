package br.com.luizotavionazar.permluiz.config.security;

import br.com.luizotavionazar.permluiz.domain.configuracao.ConfiguracaoAplicacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AdminVerificador {

    private final ConfiguracaoAplicacaoRepository configuracaoRepository;

    public Long extrairIdUsuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public void exigirAdmin(Jwt jwt) {
        Long idUsuario = extrairIdUsuario(jwt);
        Long idAdminMestre = configuracaoRepository.findById(1L)
                .map(c -> c.getIdAdminMestre())
                .orElse(null);

        if (!idUsuario.equals(idAdminMestre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao admin mestre!");
        }
    }
}
