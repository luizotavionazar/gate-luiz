package br.com.luizotavionazar.authluiz.domain.auditoria.service;

import br.com.luizotavionazar.authluiz.domain.auditoria.entity.LogAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.repository.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final ThreadLocal<String> DETALHES_CONTEXTO = new ThreadLocal<>();

    private final LogAuditoriaRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(LogAuditoria log) {
        repository.save(log);
    }

    public static void definirDetalhes(String detalhes) {
        DETALHES_CONTEXTO.set(detalhes);
    }

    public static String lerELimparDetalhes() {
        String detalhes = DETALHES_CONTEXTO.get();
        DETALHES_CONTEXTO.remove();
        return detalhes;
    }
}
