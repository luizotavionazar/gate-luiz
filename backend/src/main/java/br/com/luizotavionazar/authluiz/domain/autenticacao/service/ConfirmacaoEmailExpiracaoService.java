package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenConfirmacaoRepository;
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

    private final TokenConfirmacaoRepository tokenConfirmacaoRepository;
    private final UsuarioRepository usuarioRepository;

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void removerContasNaoVerificadasExpiradas() {
        List<Integer> ids = tokenConfirmacaoRepository.findIdUsuariosNaoVerificadosSemTokenAtivo(
                TipoTokenConfirmacao.VERIFICACAO_CADASTRO, LocalDateTime.now()
        );

        if (ids.isEmpty()) {
            return;
        }

        log.info("Removendo {} conta(s) não verificada(s) com token expirado.", ids.size());
        usuarioRepository.deleteAllById(ids);
    }
}
