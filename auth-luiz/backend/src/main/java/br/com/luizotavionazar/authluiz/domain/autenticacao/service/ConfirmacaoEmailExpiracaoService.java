package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmacaoEmailExpiracaoService {

    static final long DIAS_RETENCAO_CONTA_NAO_VERIFICADA = 7;

    private final UsuarioRepository usuarioRepository;

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void removerContasNaoVerificadasAntigas() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_RETENCAO_CONTA_NAO_VERIFICADA);
        List<Integer> ids = usuarioRepository.findIdsNaoVerificadosAntigos(limite);

        if (ids.isEmpty()) {
            return;
        }

        log.info("Removendo {} conta(s) não verificada(s) com mais de {} dias.", ids.size(), DIAS_RETENCAO_CONTA_NAO_VERIFICADA);
        usuarioRepository.deleteAllById(ids);
    }
}
