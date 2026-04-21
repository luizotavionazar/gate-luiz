package br.com.luizotavionazar.permluiz.config.setup;

import br.com.luizotavionazar.permluiz.domain.configuracao.ConfiguracaoAplicacaoRepository;
import br.com.luizotavionazar.permluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SetupFilter extends OncePerRequestFilter {

    private final ConfiguracaoAplicacaoRepository configuracaoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/setup") || path.startsWith("/error")) {
            chain.doFilter(request, response);
            return;
        }

        ConfiguracaoAplicacao config = configuracaoRepository.findById(1L).orElse(null);
        if (config == null || !config.getSetupConcluido()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"mensagem\":\"Serviço não configurado. Acesse /setup para concluir a configuração inicial!\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
