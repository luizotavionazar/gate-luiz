package br.com.luizotavionazar.authluiz.config.auditoria;

import br.com.luizotavionazar.authluiz.api.common.IpUtils;
import br.com.luizotavionazar.authluiz.domain.auditoria.entity.LogAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.AcaoAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.enums.CategoriaAuditoria;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;
    private final SetupService setupService;
    private final UsuarioRepository usuarioRepository;

    @Around("@annotation(auditavel)")
    public Object auditar(ProceedingJoinPoint pjp, Auditavel auditavel) throws Throwable {
        if (auditavel.categoria() == CategoriaAuditoria.ATIVIDADE && !setupService.auditoriaAtividadeHabilitada()) {
            return pjp.proceed();
        }

        HttpServletRequest req = extrairRequest();
        String ip = req != null ? IpUtils.extrairIp(req) : null;
        String uri = req != null ? req.getRequestURI() : null;
        String metodo = req != null ? req.getMethod() : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = extrairIdUsuario(auth);

        try {
            Object resultado = pjp.proceed();
            int statusHttp = extrairStatusHttp(resultado);
            auditoriaService.registrar(LogAuditoria.builder()
                    .acao(auditavel.acao())
                    .categoria(auditavel.categoria())
                    .idUsuario(idUsuario)
                    .ipOrigem(ip)
                    .uri(uri)
                    .metodoHttp(metodo)
                    .statusHttp(statusHttp)
                    .sucesso(true)
                    .detalhes(AuditoriaService.lerELimparDetalhes())
                    .build());
            return resultado;
        } catch (Exception ex) {
            AuditoriaService.lerELimparDetalhes();
            try {
                auditoriaService.registrar(LogAuditoria.builder()
                        .acao(resolverAcaoFalha(auditavel.acao()))
                        .categoria(auditavel.categoria())
                        .idUsuario(idUsuario)
                        .ipOrigem(ip)
                        .uri(uri)
                        .metodoHttp(metodo)
                        .statusHttp(resolverStatus(ex))
                        .sucesso(false)
                        .detalhes(ex.getMessage())
                        .build());
            } catch (Exception auditEx) {
                log.error("Falha ao registrar auditoria de erro: {}", auditEx.getMessage(), auditEx);
            }
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
            if (subject == null) return null;
            return usuarioRepository.findByPublicId(subject)
                    .map(u -> u.getId().longValue())
                    .orElse(null);
        }
        return null;
    }

    private int extrairStatusHttp(Object resultado) {
        if (resultado instanceof org.springframework.http.ResponseEntity<?> re) {
            return re.getStatusCode().value();
        }
        return 200;
    }

    private AcaoAuditoria resolverAcaoFalha(AcaoAuditoria acao) {
        return switch (acao) {
            case LOGIN_SUCESSO -> AcaoAuditoria.LOGIN_FALHA;
            case LOGIN_GOOGLE -> AcaoAuditoria.LOGIN_GOOGLE_FALHA;
            case VERIFICACAO_LOGIN_SUCESSO -> AcaoAuditoria.VERIFICACAO_LOGIN_FALHA;
            default -> acao;
        };
    }

    private int resolverStatus(Exception ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode().value();
        }
        if (ex instanceof BadCredentialsException) {
            return 401;
        }
        if (ex instanceof AccessDeniedException) {
            return 403;
        }
        return 500;
    }
}
