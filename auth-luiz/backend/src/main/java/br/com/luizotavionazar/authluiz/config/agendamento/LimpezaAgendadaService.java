package br.com.luizotavionazar.authluiz.config.agendamento;

import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaLimpezaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LoginPendenteService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LogoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Ponto único de agendamento para limpeza de registros do banco.
 *
 * Cada domínio implementa seu próprio método de limpeza com sua regra de retenção,
 * mas a execução é centralizada aqui — todos os jobs rodam no mesmo horário e num
 * único lugar para facilitar manutenção. Falhas são isoladas: se um domínio lançar
 * exceção, os demais continuam executando.
 *
 * Para adicionar uma nova limpeza: implemente o método no serviço de domínio
 * correspondente e registre a chamada abaixo.
 *
 * Horário: 03:00 diariamente.
 *
 * Retenções atuais:
 *   - Log de auditoria  → auditoriaRetencaoDias (padrão 90 dias, configurável via setup)
 *   - Blacklist de tokens (logout) → até expiraEm do token (máx. jwt.expiration-minutes)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LimpezaAgendadaService {

    private final AuditoriaLimpezaService auditoriaLimpezaService;
    private final LogoutService logoutService;
    private final LoginPendenteService loginPendenteService;

    @Scheduled(cron = "0 0 3 * * *")
    public void limpar() {
        executar("auditoria", auditoriaLimpezaService::limpar);
        executar("blacklist de tokens", logoutService::limparExpirados);
        executar("login pendente", loginPendenteService::limparExpirados);
    }

    private void executar(String dominio, Runnable job) {
        try {
            job.run();
        } catch (Exception e) {
            log.error("Falha na limpeza agendada [{}]: {}", dominio, e.getMessage(), e);
        }
    }
}
