package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenConfirmacaoRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.util.TokenUtils;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenConfirmacaoService {

    static final long EXPIRACAO_VERIFICACAO_CADASTRO_DIAS = 7;
    static final long EXPIRACAO_ALTERACAO_EMAIL_MINUTES = 30;
    static final long COOLDOWN_REENVIO_MINUTES = 2;

    private final TokenConfirmacaoRepository tokenConfirmacaoRepository;

    @Transactional
    public String criarTokenVerificacaoCadastro(Usuario usuario, String ip) {
        encerrarTokensAbertos(usuario.getId(), TipoTokenConfirmacao.VERIFICACAO_CADASTRO);
        return criarToken(usuario, TipoTokenConfirmacao.VERIFICACAO_CADASTRO, null, ip,
                LocalDateTime.now().plusDays(EXPIRACAO_VERIFICACAO_CADASTRO_DIAS));
    }

    @Transactional
    public String criarTokenAlteracaoEmail(Usuario usuario, String novoEmail, String ip) {
        encerrarTokensAbertos(usuario.getId(), TipoTokenConfirmacao.ALTERACAO_EMAIL);
        return criarToken(usuario, TipoTokenConfirmacao.ALTERACAO_EMAIL, novoEmail, ip,
                LocalDateTime.now().plusMinutes(EXPIRACAO_ALTERACAO_EMAIL_MINUTES));
    }

    @Transactional(readOnly = true)
    public TokenConfirmacao buscarTokenValido(String tokenBruto) {
        String tokenHash = TokenUtils.gerarHash(tokenBruto);

        TokenConfirmacao token = tokenConfirmacaoRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado!"));

        if (token.confirmado() || token.encerrado() || token.expirado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado!");
        }

        return token;
    }

    @Transactional
    public void confirmar(TokenConfirmacao token) {
        LocalDateTime agora = LocalDateTime.now();
        token.setConfirmadoEm(agora);
        token.setEncerradoEm(agora);
        tokenConfirmacaoRepository.saveAndFlush(token);
    }

    @Transactional(readOnly = true)
    public boolean estaDentroDoCooldown(Integer idUsuario, TipoTokenConfirmacao tipo) {
        return tokenConfirmacaoRepository
                .findFirstByUsuarioIdAndTipoAndConfirmadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(idUsuario, tipo)
                .map(t -> LocalDateTime.now().isBefore(t.getDataCriacao().plusMinutes(COOLDOWN_REENVIO_MINUTES)))
                .orElse(false);
    }

    private String criarToken(Usuario usuario, TipoTokenConfirmacao tipo, String emailDestino, String ip, LocalDateTime expiraEm) {
        String tokenBruto = TokenUtils.gerarTokenSeguro();
        String tokenHash = TokenUtils.gerarHash(tokenBruto);

        TokenConfirmacao token = TokenConfirmacao.builder()
                .usuario(usuario)
                .tipo(tipo)
                .tokenHash(tokenHash)
                .emailDestino(emailDestino)
                .expiraEm(expiraEm)
                .ipSolicitacao(ip)
                .build();

        tokenConfirmacaoRepository.save(token);
        return tokenBruto;
    }

    private void encerrarTokensAbertos(Integer idUsuario, TipoTokenConfirmacao tipo) {
        tokenConfirmacaoRepository.encerrarTokensAbertos(idUsuario, tipo, LocalDateTime.now());
    }
}
