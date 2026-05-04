package br.com.luizotavionazar.permluiz.config.auditoria;

import br.com.luizotavionazar.permluiz.domain.auditoria.entity.LogAuditoria;
import br.com.luizotavionazar.permluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.permluiz.domain.auditoria.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;

    @Value("${auditoria.atividade:true}")
    private boolean atividadeHabilitado;

    @Around("@annotation(auditavel)")
    public Object auditar(ProceedingJoinPoint pjp, Auditavel auditavel) throws Throwable {
        if (auditavel.categoria() == CategoriaAuditoria.ATIVIDADE && !atividadeHabilitado) {
            return pjp.proceed();
        }

        HttpServletRequest req = extrairRequest();
        String ip = req != null ? req.getRemoteAddr() : null;
        String uri = req != null ? req.getRequestURI() : null;
        String metodo = req != null ? req.getMethod() : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = extrairIdUsuario(auth);

        try {
            Object resultado = pjp.proceed();
            auditoriaService.registrar(LogAuditoria.builder()
                    .acao(auditavel.acao())
                    .categoria(auditavel.categoria())
                    .idUsuario(idUsuario)
                    .ipOrigem(ip)
                    .uri(uri)
                    .metodoHttp(metodo)
                    .statusHttp(200)
                    .sucesso(true)
                    .detalhes(AuditoriaService.lerELimparDetalhes())
                    .build());
            return resultado;
        } catch (Exception ex) {
            AuditoriaService.lerELimparDetalhes();
            auditoriaService.registrar(LogAuditoria.builder()
                    .acao(auditavel.acao())
                    .categoria(auditavel.categoria())
                    .idUsuario(idUsuario)
                    .ipOrigem(ip)
                    .uri(uri)
                    .metodoHttp(metodo)
                    .statusHttp(resolverStatus(ex))
                    .sucesso(false)
                    .detalhes(ex.getMessage())
                    .build());
            throw ex;
        }
    }

    private HttpServletRequest extrairRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private Long extrairIdUsuario(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            try {
                return Long.valueOf(subject);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private int resolverStatus(Exception ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode().value();
        }
        if (ex instanceof AccessDeniedException) {
            return 403;
        }
        return 500;
    }
}
