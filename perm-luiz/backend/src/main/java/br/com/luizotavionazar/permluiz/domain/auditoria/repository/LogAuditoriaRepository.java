package br.com.luizotavionazar.permluiz.domain.auditoria.repository;

import br.com.luizotavionazar.permluiz.domain.auditoria.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    void deleteByCriadoEmBefore(LocalDateTime limite);
}
