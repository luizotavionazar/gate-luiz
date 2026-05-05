package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.*;
import br.com.luizotavionazar.authluiz.api.common.exception.ExcecaoLimiteTentativas;
import br.com.luizotavionazar.authluiz.config.security.JwtService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.ControleRecuperacaoSenha;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenRecuperacaoSenha;
import br.com.luizotavionazar.authluiz.domain.autenticacao.event.UsuarioCadastradoEvent;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.ControleRecuperacaoSenhaRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenRecuperacaoSenhaRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.util.TokenUtils;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.notificacao.service.EmailService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import br.com.luizotavionazar.authluiz.domain.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final EmailService emailService;
    private final ControleRecuperacaoSenhaRepository controleRecuperacaoSenhaRepository;
    private final PoliticaSenhaService politicaSenhaService;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final TokenRecuperacaoSenhaExpiracaoService tokenRecuperacaoSenhaExpiracaoService;
    private final TokenConfirmacaoService tokenConfirmacaoService;

    private static final long COOLDOWN_TOKEN_MINUTES = 2;
    private static final long EXPIRACAO_TOKEN_MINUTES = 30;
    private static final long JANELA_IP_MINUTES = 10;
    private static final int LIMITE_TENTATIVAS_IP = 5;
    private static final long BLOQUEIO_IP_MINUTES = 2;

    static final String MSG_CREDENCIAIS_INVALIDAS = "E-mail/telefone ou senha incorretos!";

    @Transactional
    public CadastroResponse cadastrar(CadastroRequest request, String ip) {
        String emailNormalizado = request.emailNormalizado();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado!");
        }

        politicaSenhaService.validar(request.senha());

        Usuario usuario = usuarioService.cadastrar(request.nomeNormalizado(), emailNormalizado, request.senha(), request.telefone());

        usuario.setEmailVerificado(false);
        usuarioRepository.save(usuario);
        String tokenVerificacao = tokenConfirmacaoService.criarTokenVerificacaoCadastro(usuario, ip);

        eventPublisher.publishEvent(
                new UsuarioCadastradoEvent(usuario.getId(), usuario.getNome(), usuario.getEmail(), tokenVerificacao));

        AuditoriaService.definirDetalhes("E-mail: " + emailNormalizado);
        return CadastroResponse.from(usuario);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identificadorNormalizado = request.identificadorNormalizado();

        Usuario usuario = (request.isEmail()
                ? usuarioRepository.findByEmail(identificadorNormalizado)
                : usuarioRepository.findByTelefone(identificadorNormalizado))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_CREDENCIAIS_INVALIDAS));

        if (!usuario.possuiSenha()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_CREDENCIAIS_INVALIDAS);
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_CREDENCIAIS_INVALIDAS);
        }

        usuarioRepository.atualizarUltimoLogin(usuario.getId(), LocalDateTime.now());

        String token = jwtService.gerarToken(usuario);
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);

        String detalhe = request.isEmail()
                ? "E-mail: " + identificadorNormalizado
                : "Telefone: " + identificadorNormalizado;
        AuditoriaService.definirDetalhes(detalhe);

        return LoginResponse.from(usuario, temLoginGoogle, token, jwtService.getExpirationMinutes());
    }

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public MensagemResponse iniciarRecuperacaoSenha(RecuperacaoSenhaRequest request, String ip) {
        validarLimitePorIp(ip);

        String emailNormalizado = request.emailNormalizado();

        usuarioRepository.findByEmail(emailNormalizado).ifPresent(usuario -> {
            boolean contaGoogleOnly = !usuario.possuiSenha()
                    && identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                            ProviderExterno.GOOGLE);
            if (contaGoogleOnly) {
                return;
            }

            LocalDateTime agora = LocalDateTime.now();

            tokenRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUsadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(usuario.getId())
                    .ifPresent(tokenAnterior -> {
                        if (!tokenAnterior.expirado()) {
                            if (agora.isBefore(tokenAnterior.getDataCriacao().plusMinutes(COOLDOWN_TOKEN_MINUTES))) {
                                return;
                            }
                            tokenAnterior.setEncerradoEm(agora);
                            tokenRecuperacaoSenhaRepository.saveAndFlush(tokenAnterior);
                            return;
                        }

                        tokenAnterior.setEncerradoEm(agora);
                        tokenRecuperacaoSenhaRepository.saveAndFlush(tokenAnterior);
                    });

            boolean aindaExisteTokenAtivo = tokenRecuperacaoSenhaRepository
                    .findFirstByUsuarioIdAndUsadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(usuario.getId())
                    .filter(token -> !token.expirado())
                    .isPresent();

            if (aindaExisteTokenAtivo) {
                return;
            }

            String tokenBruto = TokenUtils.gerarTokenSeguro();
            String tokenHash = TokenUtils.gerarHash(tokenBruto);

            TokenRecuperacaoSenha token = TokenRecuperacaoSenha.builder()
                    .usuario(usuario)
                    .tokenHash(tokenHash)
                    .expiraEm(agora.plusMinutes(EXPIRACAO_TOKEN_MINUTES))
                    .ipSolicitacao(ip)
                    .build();

            tokenRecuperacaoSenhaRepository.save(token);
            emailService.enviarRecuperacaoSenha(usuario.getNome(), usuario.getEmail(), tokenBruto);
        });

        AuditoriaService.definirDetalhes("E-mail: " + emailNormalizado);
        return mensagemGenericaRecuperacao();
    }

    @Transactional
    public MensagemResponse redefinirSenha(RedefinirSenhaRequest request) {
        String tokenHash = TokenUtils.gerarHash(request.token());

        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Link de recuperação inválido ou expirado!"));

        if (token.usado() || token.encerrado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link de recuperação inválido ou expirado!");
        }

        if (token.expirado()) {
            tokenRecuperacaoSenhaExpiracaoService.encerrarSeExpirado(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link de recuperação inválido ou expirado!");
        }

        politicaSenhaService.validar(request.novaSenha());

        Usuario usuario = token.getUsuario();
        if (usuario.possuiSenha() && passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da atual!");
        }

        LocalDateTime agora = LocalDateTime.now();
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.saveAndFlush(usuario);

        token.setUsadoEm(agora);
        token.setEncerradoEm(agora);
        tokenRecuperacaoSenhaRepository.saveAndFlush(token);

        AuditoriaService.definirDetalhes("E-mail: " + usuario.getEmail());
        return new MensagemResponse("Senha redefinida com sucesso");
    }

    @Transactional
    public MensagemResponse validarTokenRecuperacao(String tokenBruto) {
        String tokenHash = TokenUtils.gerarHash(tokenBruto);

        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Link de recuperação inválido ou expirado!"));

        if (token.usado() || token.encerrado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link de recuperação inválido ou expirado!");
        }

        if (token.expirado()) {
            tokenRecuperacaoSenhaExpiracaoService.encerrarSeExpirado(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link de recuperação inválido ou expirado!");
        }

        return new MensagemResponse("Token válido!");
    }

    private MensagemResponse mensagemGenericaRecuperacao() {
        return new MensagemResponse(
                "Enviaremos as instruções de recuperação caso exista uma conta vinculada a esse e-mail!");
    }

    private void validarLimitePorIp(String ip) {
        LocalDateTime agora = LocalDateTime.now();

        ControleRecuperacaoSenha controle = controleRecuperacaoSenhaRepository.findByIp(ip)
                .orElseGet(() -> ControleRecuperacaoSenha.builder()
                        .ip(ip)
                        .janelaInicio(agora)
                        .quantidade(0)
                        .build());

        if (controle.getBloqueadoAte() != null && agora.isBefore(controle.getBloqueadoAte())) {
            long retryAfterSeconds = Duration.between(agora, controle.getBloqueadoAte()).toSeconds();
            long minutosRestantes = Math.max(1, (retryAfterSeconds + 59) / 60);
            throw new ExcecaoLimiteTentativas(
                    "Foram realizadas muitas solicitações de recuperação a partir deste dispositivo ou rede. Tente novamente em cerca de "
                            + minutosRestantes + " minuto(s)!",
                    retryAfterSeconds);
        }

        if (controle.getJanelaInicio() == null
                || agora.isAfter(controle.getJanelaInicio().plusMinutes(JANELA_IP_MINUTES))) {
            controle.setJanelaInicio(agora);
            controle.setQuantidade(1);
            controle.setBloqueadoAte(null);
            controleRecuperacaoSenhaRepository.save(controle);
            return;
        }

        int novaQuantidade = controle.getQuantidade() + 1;
        controle.setQuantidade(novaQuantidade);

        if (novaQuantidade > LIMITE_TENTATIVAS_IP) {
            LocalDateTime bloqueadoAte = agora.plusMinutes(BLOQUEIO_IP_MINUTES);
            controle.setBloqueadoAte(bloqueadoAte);
            controleRecuperacaoSenhaRepository.save(controle);

            long retryAfterSeconds = Duration.between(agora, bloqueadoAte).toSeconds();
            long minutosRestantes = Math.max(1, (retryAfterSeconds + 59) / 60);
            throw new ExcecaoLimiteTentativas(
                    "Foram realizadas muitas solicitações de recuperação a partir deste dispositivo ou rede. Tente novamente em cerca de "
                            + minutosRestantes + " minuto(s)!",
                    retryAfterSeconds);
        }

        controleRecuperacaoSenhaRepository.save(controle);
    }
}
