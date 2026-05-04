package br.com.luizotavionazar.authluiz.domain.auditoria.repository;

import br.com.luizotavionazar.authluiz.domain.auditoria.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    void deleteByCriadoEmBefore(LocalDateTime limite);
}
