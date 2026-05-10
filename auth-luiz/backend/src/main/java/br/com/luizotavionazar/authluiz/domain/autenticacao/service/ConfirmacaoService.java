package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenConfirmacao;
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

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void confirmarEmail(Integer idUsuario, String codigo) {
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

    @Transactional
    public MensagemResponse reenviarVerificacao(Integer idUsuario, String ip) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));

        if (usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O e-mail desta conta já foi verificado!");
        }

        if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.VERIFICACAO_CADASTRO)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns instantes antes de solicitar um novo e-mail de verificação!");
        }

        String novoCodigo = tokenConfirmacaoService.criarTokenVerificacaoCadastro(usuario, ip);
        emailService.enviarVerificacaoCadastro(usuario.getNome(), usuario.getEmail(), novoCodigo);

        return new MensagemResponse("E-mail de verificação reenviado com sucesso!");
    }

    @Transactional
    public MensagemResponse reenviarConfirmacaoAlteracaoEmail(Integer idUsuario, String ip) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));

        if (usuario.getEmailPendente() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há alteração de e-mail pendente.");
        }

        if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns instantes antes de solicitar um novo e-mail de confirmação.");
        }

        String novoCodigo = tokenConfirmacaoService.criarTokenAlteracaoEmail(usuario, usuario.getEmailPendente(), ip);
        emailService.enviarConfirmacaoAlteracaoEmail(usuario.getNome(), usuario.getEmailPendente(), novoCodigo);

        return new MensagemResponse("E-mail de confirmação reenviado com sucesso!");
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
}
