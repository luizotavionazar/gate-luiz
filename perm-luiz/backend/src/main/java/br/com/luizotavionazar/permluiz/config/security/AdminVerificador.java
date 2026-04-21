package br.com.luizotavionazar.permluiz.config.security;

import br.com.luizotavionazar.permluiz.domain.configuracao.ConfiguracaoAplicacaoRepository;
import br.com.luizotavionazar.permluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AdminVerificador {

    private final ConfiguracaoAplicacaoRepository configuracaoRepository;

    public Long extrairIdUsuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public boolean isAdmin(Jwt jwt) {
        Long idUsuario = extrairIdUsuario(jwt);
        return configuracaoRepository.findById(1L)
                .map(c -> idUsuario.equals(c.getIdAdminMestre()))
                .orElse(false);
    }

    @Transactional
    public void exigirAdmin(Jwt jwt) {
        Long idUsuario = extrairIdUsuario(jwt);
        ConfiguracaoAplicacao config = configuracaoRepository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        if (config.getIdAdminMestre() == null) {
            config.setIdAdminMestre(idUsuario);
            configuracaoRepository.save(config);
            return;
        }

        if (!idUsuario.equals(config.getIdAdminMestre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao admin mestre!");
        }
    }
}
