package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.common.exception.ExcecaoLimiteTentativas;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.ControleEnvioCodigoIp;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.ControleEnvioCodigoIpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnvioCodigoRateLimitService {

    private static final long JANELA_MINUTOS = 10;
    private static final int LIMITE = 5;
    private static final long BLOQUEIO_MINUTOS = 2;

    private final ControleEnvioCodigoIpRepository controleRepository;

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public void validarLimitePorIp(String ip) {
        LocalDateTime agora = LocalDateTime.now();

        ControleEnvioCodigoIp controle = controleRepository.findByIp(ip)
                .orElseGet(() -> ControleEnvioCodigoIp.builder()
                        .ip(ip)
                        .janelaInicio(agora)
                        .quantidade(0)
                        .build());

        if (controle.getBloqueadoAte() != null && agora.isBefore(controle.getBloqueadoAte())) {
            long retryAfterSeconds = Duration.between(agora, controle.getBloqueadoAte()).toSeconds();
            long minutosRestantes = Math.max(1, (retryAfterSeconds + 59) / 60);
            throw new ExcecaoLimiteTentativas(
                    "Muitas solicitações de envio de código a partir deste dispositivo ou rede. " +
                    "Tente novamente em cerca de " + minutosRestantes + " minuto(s)!",
                    retryAfterSeconds);
        }

        // Bloqueio expirou ou janela de 10 min expirou: reinicia contagem
        boolean bloqueioExpirou = controle.getBloqueadoAte() != null;
        boolean janelaExpirou = controle.getJanelaInicio() == null
                || agora.isAfter(controle.getJanelaInicio().plusMinutes(JANELA_MINUTOS));

        if (bloqueioExpirou || janelaExpirou) {
            controle.setJanelaInicio(agora);
            controle.setQuantidade(1);
            controle.setBloqueadoAte(null);
            controleRepository.save(controle);
            return;
        }

        int novaQuantidade = controle.getQuantidade() + 1;
        controle.setQuantidade(novaQuantidade);

        if (novaQuantidade > LIMITE) {
            LocalDateTime bloqueadoAte = agora.plusMinutes(BLOQUEIO_MINUTOS);
            controle.setBloqueadoAte(bloqueadoAte);
            controleRepository.save(controle);
            long retryAfterSeconds = Duration.between(agora, bloqueadoAte).toSeconds();
            long minutosRestantes = Math.max(1, (retryAfterSeconds + 59) / 60);
            throw new ExcecaoLimiteTentativas(
                    "Muitas solicitações de envio de código a partir deste dispositivo ou rede. " +
                    "Tente novamente em cerca de " + minutosRestantes + " minuto(s)!",
                    retryAfterSeconds);
        }

        controleRepository.save(controle);
    }
}
