package br.com.luizotavionazar.permluiz.domain.auditoria.service;

import br.com.luizotavionazar.permluiz.domain.auditoria.repository.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditoriaLimpezaService {

    private final LogAuditoriaRepository repository;

    @Value("${auditoria.retencao-dias:90}")
    private int retencaoDias;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpar() {
        repository.deleteByCriadoEmBefore(LocalDateTime.now().minusDays(retencaoDias));
    }
}
