package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.common.exception.ExcecaoLimiteTentativas;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.notificacao.port.NotificacaoTelefonePort;
import br.com.luizotavionazar.authluiz.domain.notificacao.service.EmailService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class ConfirmacaoService {

    private final TokenConfirmacaoService tokenConfirmacaoService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final NotificacaoTelefonePort notificacaoTelefonePort;
    private final EnvioCodigoRateLimitService envioCodigoRateLimitService;

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirmarEmail(String publicId, String codigo) {
        Integer idUsuario = resolverIdInterno(publicId);
        TipoTokenConfirmacao tipo = detectarTipoPendente(idUsuario);

        TokenConfirmacao token = tokenConfirmacaoService.buscarTokenValidoPorUsuario(idUsuario, tipo, codigo);
        Usuario usuario = token.getUsuario();

        if (tipo == TipoTokenConfirmacao.VERIFICACAO_CADASTRO) {
            usuario.setEmailVerificado(true);
            usuarioRepository.save(usuario);
        } else {
            String novoEmail = token.getEmailDestino();
            if (usuarioRepository.existsByEmailAndIdNot(novoEmail, usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso por outra conta!");
            }
            usuario.setEmail(novoEmail);
            usuario.setEmailPendente(null);
            usuarioRepository.save(usuario);
        }

        String emailConfirmado = tipo == TipoTokenConfirmacao.ALTERACAO_EMAIL
                ? token.getEmailDestino()
                : usuario.getEmail();
        AuditoriaService.definirDetalhes("E-mail: " + emailConfirmado);
        tokenConfirmacaoService.confirmar(token);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirmarTelefone(String publicId, String codigo) {
        Integer idUsuario = resolverIdInterno(publicId);
        TipoTokenConfirmacao tipo = detectarTipoPendenteTelefone(idUsuario);
        TokenConfirmacao token = tokenConfirmacaoService.buscarTokenValidoPorUsuario(idUsuario, tipo, codigo);
        Usuario usuario = token.getUsuario();

        if (tipo == TipoTokenConfirmacao.VERIFICACAO_TELEFONE) {
            usuario.setTelefoneVerificado(true);
            usuarioRepository.save(usuario);
            AuditoriaService.definirDetalhes("Telefone verificado: " + usuario.getTelefone());
        } else {
            String novoTelefone = token.getTelefoneDestino();
            if (usuarioRepository.existsByTelefoneAndIdNot(novoTelefone, usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este telefone já está em uso por outra conta!");
            }
            usuario.setTelefone(novoTelefone);
            usuario.setTelefonePendente(null);
            usuario.setTelefoneVerificado(true);
            usuarioRepository.save(usuario);
            AuditoriaService.definirDetalhes("Telefone: " + novoTelefone);
        }

        tokenConfirmacaoService.confirmar(token);
    }

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public MensagemResponse enviarVerificacaoEmail(String publicId, String ip) {
        Usuario usuario = usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));
        Integer idUsuario = usuario.getId();

        envioCodigoRateLimitService.validarLimitePorIp(ip);

        if (!usuario.isEmailVerificado()) {
            if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.VERIFICACAO_CADASTRO)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Aguarde alguns instantes antes de solicitar um novo código de verificação!");
            }
            String codigo = tokenConfirmacaoService.criarTokenVerificacaoCadastro(usuario, ip);
            emailService.enviarVerificacaoCadastro(usuario.getNome(), usuario.getEmail(), codigo);
            return new MensagemResponse("Código de verificação enviado para " + usuario.getEmail() + ".");
        }

        if (usuario.getEmailPendente() != null) {
            if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Aguarde alguns instantes antes de solicitar um novo código de verificação!");
            }
            String codigo = tokenConfirmacaoService.criarTokenAlteracaoEmail(usuario, usuario.getEmailPendente(), ip);
            emailService.enviarConfirmacaoAlteracaoEmail(usuario.getNome(), usuario.getEmailPendente(), codigo);
            return new MensagemResponse("Código de verificação enviado para " + usuario.getEmailPendente() + ".");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há verificação de e-mail pendente.");
    }

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public MensagemResponse enviarVerificacaoTelefone(String publicId, String ip) {
        Usuario usuario = usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));
        Integer idUsuario = usuario.getId();

        envioCodigoRateLimitService.validarLimitePorIp(ip);
        notificacaoTelefonePort.validarDisponibilidade();

        if (usuario.getTelefonePendente() != null) {
            if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.ALTERACAO_TELEFONE)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Aguarde alguns instantes antes de solicitar um novo código de verificação.");
            }
            String codigo = tokenConfirmacaoService.criarTokenAlteracaoTelefone(usuario, usuario.getTelefonePendente(), ip);
            notificacaoTelefonePort.enviarCodigoVerificacao(usuario.getTelefonePendente(), codigo);
            return new MensagemResponse("Código de verificação enviado para " + usuario.getTelefonePendente() + ".");
        }

        if (usuario.getTelefone() != null && !usuario.isTelefoneVerificado()) {
            if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.VERIFICACAO_TELEFONE)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Aguarde alguns instantes antes de solicitar um novo código de verificação.");
            }
            String codigo = tokenConfirmacaoService.criarTokenVerificacaoTelefone(usuario, ip);
            notificacaoTelefonePort.enviarCodigoVerificacao(usuario.getTelefone(), codigo);
            return new MensagemResponse("Código de verificação enviado para " + usuario.getTelefone() + ".");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há verificação de telefone pendente.");
    }

    private Integer resolverIdInterno(String publicId) {
        return usuarioRepository.findByPublicId(publicId)
                .map(Usuario::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));
    }

    private TipoTokenConfirmacao detectarTipoPendente(Integer idUsuario) {
        if (tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.VERIFICACAO_CADASTRO)) {
            return TipoTokenConfirmacao.VERIFICACAO_CADASTRO;
        }
        if (tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
            return TipoTokenConfirmacao.ALTERACAO_EMAIL;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Nenhum código de verificação ativo encontrado!");
    }

    private TipoTokenConfirmacao detectarTipoPendenteTelefone(Integer idUsuario) {
        if (tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.VERIFICACAO_TELEFONE)) {
            return TipoTokenConfirmacao.VERIFICACAO_TELEFONE;
        }
        if (tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.ALTERACAO_TELEFONE)) {
            return TipoTokenConfirmacao.ALTERACAO_TELEFONE;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Nenhum código de verificação ativo encontrado!");
    }
}
