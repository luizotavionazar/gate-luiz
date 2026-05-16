package br.com.luizotavionazar.authluiz.domain.auditoria.service;

import br.com.luizotavionazar.authluiz.domain.auditoria.repository.LogAuditoriaRepository;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditoriaLimpezaService {

    private final LogAuditoriaRepository repository;
    private final SetupService setupService;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpar() {
        repository.deleteByCriadoEmBefore(LocalDateTime.now().minusDays(setupService.auditoriaRetencaoDias()));
    }
}
