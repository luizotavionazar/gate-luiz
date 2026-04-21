package br.com.luizotavionazar.permluiz.api.setup;

import br.com.luizotavionazar.permluiz.domain.configuracao.ConfiguracaoAplicacaoRepository;
import br.com.luizotavionazar.permluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final ConfiguracaoAplicacaoRepository configuracaoRepository;

    @Value("${app.setup.master-key}")
    private String masterKey;

    Map<String, Object> status() {
        ConfiguracaoAplicacao config = configuracaoRepository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        return Map.of("setupConcluido", config.getSetupConcluido());
    }

    Map<String, Object> concluirSetup(String chave, SetupRequest request) {
        if (!masterKey.trim().equals(chave)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave mestra inválida!");
        }

        ConfiguracaoAplicacao config = configuracaoRepository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

        if (config.getSetupConcluido()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup já foi concluído!");
        }

        config.setIdAdminMestre(request.idUsuario());
        config.setSetupConcluido(true);
        configuracaoRepository.save(config);

        return Map.of(
                "mensagem", "Setup concluído com sucesso!",
                "setupConcluido", true,
                "idAdminMestre", request.idUsuario()
        );
    }
}
