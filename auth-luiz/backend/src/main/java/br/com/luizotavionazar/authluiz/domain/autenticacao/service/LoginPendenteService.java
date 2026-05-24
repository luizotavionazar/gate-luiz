package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.LoginPendenteRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.util.TokenUtils;
import br.com.luizotavionazar.authluiz.domain.notificacao.port.NotificacaoTelefonePort;
import br.com.luizotavionazar.authluiz.domain.notificacao.service.EmailService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginPendenteService {

    private static final int EXPIRACAO_MINUTOS = 5;
    private static final int MAX_TENTATIVAS = 5;

    private final LoginPendenteRepository repo;
    private final TotpService totpService;
    private final EmailService emailService;
    private final NotificacaoTelefonePort notificacaoTelefonePort;
    private final UsuarioRepository usuarioRepository;

    public LoginPendente criar(Usuario usuario, String ip) {
        String tipo = determinarTipo(usuario);
        String codigo = "TOTP".equals(tipo) ? null : TokenUtils.gerarCodigoNumerico6Digitos();
        String token = (UUID.randomUUID().toString() + UUID.randomUUID().toString())
                .replace("-", "").substring(0, 64);

        LoginPendente loginPendente = LoginPendente.builder()
                .tokenPendente(token)
                .idUsuario(usuario.getId())
                .tipo(tipo)
                .codigo(codigo)
                .ipOrigem(ip)
                .expiraEm(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS))
                .build();
        repo.save(loginPendente);

        if (codigo != null) {
            enviarCodigo(usuario, tipo, codigo);
        }

        return loginPendente;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public LoginPendente verificar(String tokenPendente, String codigo) {
        LoginPendente lp = repo.findByTokenPendente(tokenPendente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Verificação inválida ou expirada."));

        if (!lp.estaAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Verificação inválida ou expirada.");
        }

        boolean valido = validarCodigo(lp, codigo);
        if (!valido) {
            lp.setTentativasErradas(lp.getTentativasErradas() + 1);
            if (lp.getTentativasErradas() >= MAX_TENTATIVAS) {
                lp.setEncerradoEm(LocalDateTime.now());
            }
            repo.save(lp);
            int restantes = MAX_TENTATIVAS - lp.getTentativasErradas();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    restantes > 0
                            ? "Código inválido. " + restantes + " tentativa(s) restante(s)."
                            : "Verificação encerrada por excesso de tentativas.");
        }

        lp.setConfirmadoEm(LocalDateTime.now());
        repo.save(lp);
        return lp;
    }

    public LoginPendente buscarAtivo(String tokenPendente) {
        LoginPendente lp = repo.findByTokenPendente(tokenPendente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Verificação inválida ou expirada."));
        if (!lp.estaAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verificação inválida ou expirada.");
        }
        return lp;
    }

    public void confirmar(LoginPendente lp) {
        lp.setConfirmadoEm(LocalDateTime.now());
        repo.save(lp);
    }

    public LoginPendente reenviar(String tokenPendente, String canal) {
        LoginPendente lp = buscarAtivo(tokenPendente);

        if ("TOTP".equals(lp.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reenvio não disponível para autenticação via aplicativo TOTP.");
        }

        Usuario usuario = usuarioRepository.findById(lp.getIdUsuario()).orElseThrow();

        if (canal != null && !canal.equals(lp.getTipo())) {
            if (!"EMAIL".equals(canal) && (usuario.getTelefone() == null || !usuario.isTelefoneVerificado())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Telefone não cadastrado ou não verificado.");
            }
            lp.setTipo(canal);
        }

        String novoCodigo = TokenUtils.gerarCodigoNumerico6Digitos();
        lp.setCodigo(novoCodigo);
        repo.save(lp);

        enviarCodigo(usuario, lp.getTipo(), novoCodigo);
        return lp;
    }

    public void limparExpirados() {
        repo.deleteByExpiraEmBefore(LocalDateTime.now().minusHours(1));
    }

    private boolean validarCodigo(LoginPendente lp, String codigo) {
        if ("TOTP".equals(lp.getTipo())) {
            Usuario usuario = usuarioRepository.findById(lp.getIdUsuario()).orElseThrow();
            return totpService.validarCodigo(usuario.getTotpSecret(), codigo);
        }
        return lp.getCodigo() != null && lp.getCodigo().equals(codigo);
    }

    private String determinarTipo(Usuario usuario) {
        if (usuario.isTotpAtivo()) return "TOTP";
        if (usuario.getPreferencia2fa() != null) return usuario.getPreferencia2fa();
        return "EMAIL";
    }

    private void enviarCodigo(Usuario usuario, String tipo, String codigo) {
        switch (tipo) {
            case "EMAIL" -> emailService.enviarVerificacaoLogin(usuario, codigo, EXPIRACAO_MINUTOS);
            case "SMS", "WHATSAPP" -> notificacaoTelefonePort.enviarCodigoVerificacao(usuario.getTelefone(), codigo);
        }
    }
}
