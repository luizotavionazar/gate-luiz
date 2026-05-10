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

    static final long EXPIRACAO_CODIGO_MINUTES = 5;
    static final long COOLDOWN_REENVIO_MINUTES = 2;
    static final int MAX_TENTATIVAS_ERRADAS = 5;

    private final TokenConfirmacaoRepository tokenConfirmacaoRepository;

    @Transactional
    public String criarTokenVerificacaoCadastro(Usuario usuario, String ip) {
        encerrarTokensAbertos(usuario.getId(), TipoTokenConfirmacao.VERIFICACAO_CADASTRO);
        return criarToken(usuario, TipoTokenConfirmacao.VERIFICACAO_CADASTRO, null, ip,
                LocalDateTime.now().plusMinutes(EXPIRACAO_CODIGO_MINUTES));
    }

    @Transactional
    public String criarTokenAlteracaoEmail(Usuario usuario, String novoEmail, String ip) {
        encerrarTokensAbertos(usuario.getId(), TipoTokenConfirmacao.ALTERACAO_EMAIL);
        return criarToken(usuario, TipoTokenConfirmacao.ALTERACAO_EMAIL, novoEmail, ip,
                LocalDateTime.now().plusMinutes(EXPIRACAO_CODIGO_MINUTES));
    }

    @Transactional
    public TokenConfirmacao buscarTokenValidoPorUsuario(Integer idUsuario, TipoTokenConfirmacao tipo, String codigo) {
        TokenConfirmacao token = tokenConfirmacaoRepository
                .findFirstByUsuarioIdAndTipoAndConfirmadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(idUsuario, tipo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nenhum código de verificação ativo encontrado!"));

        if (token.expirado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código expirado. Solicite um novo e-mail de verificação.");
        }

        String codigoHash = TokenUtils.gerarHash(codigo);
        if (!codigoHash.equals(token.getTokenHash())) {
            token.setTentativasErradas(token.getTentativasErradas() + 1);
            if (token.getTentativasErradas() >= MAX_TENTATIVAS_ERRADAS) {
                token.setEncerradoEm(LocalDateTime.now());
                tokenConfirmacaoRepository.saveAndFlush(token);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código bloqueado após muitas tentativas incorretas. Solicite um novo e-mail de verificação.");
            }
            tokenConfirmacaoRepository.saveAndFlush(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido!");
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
    public boolean temTokenAtivo(Integer idUsuario, TipoTokenConfirmacao tipo) {
        return tokenConfirmacaoRepository
                .findFirstByUsuarioIdAndTipoAndConfirmadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(idUsuario, tipo)
                .map(t -> !t.expirado())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean estaDentroDoCooldown(Integer idUsuario, TipoTokenConfirmacao tipo) {
        return tokenConfirmacaoRepository
                .findFirstByUsuarioIdAndTipoAndConfirmadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(idUsuario, tipo)
                .map(t -> LocalDateTime.now().isBefore(t.getDataCriacao().plusMinutes(COOLDOWN_REENVIO_MINUTES)))
                .orElse(false);
    }

    private String criarToken(Usuario usuario, TipoTokenConfirmacao tipo, String emailDestino, String ip, LocalDateTime expiraEm) {
        String codigoBruto = TokenUtils.gerarCodigoNumerico6Digitos();
        String codigoHash = TokenUtils.gerarHash(codigoBruto);

        TokenConfirmacao token = TokenConfirmacao.builder()
                .usuario(usuario)
                .tipo(tipo)
                .tokenHash(codigoHash)
                .emailDestino(emailDestino)
                .expiraEm(expiraEm)
                .ipSolicitacao(ip)
                .build();

        tokenConfirmacaoRepository.save(token);
        return codigoBruto;
    }

    private void encerrarTokensAbertos(Integer idUsuario, TipoTokenConfirmacao tipo) {
        tokenConfirmacaoRepository.encerrarTokensAbertos(idUsuario, tipo, LocalDateTime.now());
    }
}
