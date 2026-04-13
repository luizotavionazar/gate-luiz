package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
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
    private final SetupService setupService;
    private final EmailService emailService;

    @Transactional
    public void confirmarEmail(String tokenBruto) {
        TokenConfirmacao token = tokenConfirmacaoService.buscarTokenValido(tokenBruto);
        Usuario usuario = token.getUsuario();

        if (token.getTipo() == TipoTokenConfirmacao.VERIFICACAO_CADASTRO) {
            usuario.setEmailVerificado(true);
            usuarioRepository.save(usuario);
        } else if (token.getTipo() == TipoTokenConfirmacao.ALTERACAO_EMAIL) {
            String novoEmail = token.getEmailDestino();
            if (usuarioRepository.existsByEmailAndIdNot(novoEmail, usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está em uso por outra conta.");
            }
            usuario.setEmail(novoEmail);
            usuario.setEmailPendente(null);
            usuarioRepository.save(usuario);
        }

        tokenConfirmacaoService.confirmar(token);
    }

    @Transactional
    public MensagemResponse reenviarVerificacao(Integer idUsuario, String ip) {
        if (!setupService.obter().isConfirmacaoEmailHabilitada()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação de e-mail não está habilitada.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));

        if (usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O e-mail desta conta já foi verificado.");
        }

        if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.VERIFICACAO_CADASTRO)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns instantes antes de solicitar um novo e-mail de verificação.");
        }

        String novoToken = tokenConfirmacaoService.criarTokenVerificacaoCadastro(usuario, ip);
        emailService.enviarVerificacaoCadastro(usuario.getNome(), usuario.getEmail(), novoToken);

        return new MensagemResponse("E-mail de verificação reenviado com sucesso.");
    }
}
