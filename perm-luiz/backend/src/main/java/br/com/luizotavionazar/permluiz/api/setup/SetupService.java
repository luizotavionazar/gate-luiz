package br.com.luizotavionazar.permluiz.api.setup;

import br.com.luizotavionazar.permluiz.domain.configuracao.ConfiguracaoAplicacaoRepository;
import br.com.luizotavionazar.permluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final ConfiguracaoAplicacaoRepository configuracaoRepository;

    Map<String, Object> status() {
        ConfiguracaoAplicacao config = configuracaoRepository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        return Map.of("adminConfigurado", config.getIdAdminMestre() != null);
    }
}
