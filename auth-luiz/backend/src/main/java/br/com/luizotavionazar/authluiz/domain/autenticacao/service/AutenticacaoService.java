package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.*;
import br.com.luizotavionazar.authluiz.api.common.exception.ExcecaoLimiteTentativas;
import br.com.luizotavionazar.authluiz.config.security.JwtService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenRecuperacaoSenha;
import br.com.luizotavionazar.authluiz.domain.autenticacao.event.UsuarioCadastradoEvent;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenRecuperacaoSenhaRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.util.TokenUtils;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.notificacao.port.NotificacaoTelefonePort;
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

import java.time.LocalDateTime;
import java.util.Optional;

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
    private final NotificacaoTelefonePort notificacaoTelefonePort;
    private final EnvioCodigoRateLimitService envioCodigoRateLimitService;
    private final PoliticaSenhaService politicaSenhaService;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final TokenRecuperacaoSenhaExpiracaoService tokenRecuperacaoSenhaExpiracaoService;
    private final IpConfiavelService ipConfiavelService;
    private final LoginPendenteService loginPendenteService;

    private static final long COOLDOWN_TOKEN_MINUTES = 1;
    private static final long EXPIRACAO_TOKEN_MINUTES = 5;
    private static final int MAX_TENTATIVAS_RECUPERACAO = 5;

    @Transactional
    public ContaResponse cadastrar(CadastroRequest request, String ip) {
        String emailNormalizado = request.emailNormalizado();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado!");
        }

        politicaSenhaService.validar(request.senha());

        Usuario usuario = usuarioService.cadastrar(
                request.usernameNormalizado(),
                request.nomeNormalizado(),
                emailNormalizado,
                request.senha(),
                request.telefone());

        usuario.setEmailVerificado(false);
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(
                new UsuarioCadastradoEvent(usuario.getPublicId(), usuario.getNome(), usuario.getEmail()));

        AuditoriaService.definirDetalhes("E-mail: " + emailNormalizado);
        return ContaResponse.from(usuario, false);
    }

    @Transactional
    public Object login(LoginRequest request, String ip) {
        String identificadorNormalizado = request.identificadorNormalizado();
        String msgCredenciaisInvalidas = "Credenciais inválidas!";

        Optional<Usuario> usuarioOpt = switch (request.tipoIdentificador()) {
            case EMAIL    -> usuarioRepository.findByEmail(identificadorNormalizado);
            case TELEFONE -> usuarioRepository.findByTelefone(identificadorNormalizado);
            case USERNAME -> usuarioRepository.findByUsername(identificadorNormalizado);
        };

        Usuario usuario = usuarioOpt
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, msgCredenciaisInvalidas));

        if (!usuario.possuiSenha()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msgCredenciaisInvalidas);
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msgCredenciaisInvalidas);
        }

        String detalhe = switch (request.tipoIdentificador()) {
            case EMAIL    -> "E-mail: " + identificadorNormalizado;
            case TELEFONE -> "Telefone: " + identificadorNormalizado;
            case USERNAME -> "Username: " + identificadorNormalizado;
        };
        AuditoriaService.definirDetalhes(detalhe);

        if (usuario.isVerificacaoExtraAtiva()) {
            boolean ipConhecido = ip != null && (ip.equals(usuario.getUltimoIp())
                    || ipConfiavelService.ehConfiavel(usuario.getId(), ip));

            if (!ipConhecido) {
                LoginPendente lp = loginPendenteService.criar(usuario, ip);
                return LoginPendenteResponse.from(lp, usuario);
            }
        }

        return completarLogin(usuario, ip);
    }

    @Transactional
    public LoginResponse completarLogin(Usuario usuario, String ip) {
        usuarioRepository.atualizarUltimoLogin(usuario.getId(), LocalDateTime.now());
        usuarioRepository.atualizarUltimoIp(usuario.getId(), ip);

        String token = jwtService.gerarToken(usuario);
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(
                usuario.getId(), ProviderExterno.GOOGLE);

        return LoginResponse.from(usuario, temLoginGoogle, token, jwtService.getExpirationMinutes());
    }

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public RecuperacaoIniciarResponse iniciarRecuperacaoSenha(RecuperacaoSenhaRequest request, String ip) {
        envioCodigoRateLimitService.validarLimitePorIp(ip);

        if (!request.usarEmail()) {
            notificacaoTelefonePort.validarDisponibilidade();
        }

        Optional<Usuario> usuarioOpt = request.usarEmail()
                ? usuarioRepository.findByEmail(request.emailNormalizado())
                : usuarioRepository.findByTelefone(request.telefoneNormalizado());

        String identificador = request.usarEmail()
                ? request.emailNormalizado()
                : request.telefoneNormalizado();

        boolean[] sugestaoLoginGoogle = {false};

        usuarioOpt.ifPresent(usuario -> {
            boolean contaGoogleOnly = !usuario.possuiSenha()
                    && identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                            ProviderExterno.GOOGLE);
            if (contaGoogleOnly) {
                sugestaoLoginGoogle[0] = true;
                return;
            }

            if (!request.usarEmail() && !usuario.isTelefoneVerificado()) return;

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
                    .filter(t -> !t.expirado())
                    .isPresent();

            if (aindaExisteTokenAtivo) return;

            String codigoBruto = TokenUtils.gerarCodigoNumerico6Digitos();

            TokenRecuperacaoSenha token = TokenRecuperacaoSenha.builder()
                    .usuario(usuario)
                    .codigo(codigoBruto)
                    .expiraEm(agora.plusMinutes(EXPIRACAO_TOKEN_MINUTES))
                    .ipSolicitacao(ip)
                    .build();

            tokenRecuperacaoSenhaRepository.save(token);

            if (request.usarEmail()) {
                emailService.enviarRecuperacaoSenha(usuario.getNome(), usuario.getEmail(), codigoBruto);
            } else {
                notificacaoTelefonePort.enviarCodigoVerificacao(usuario.getTelefone(), codigoBruto);
                emailService.enviarAvisoRecuperacaoViaTelefone(
                        usuario.getNome(), usuario.getEmail(), ip, agora, usuario.getTelefone());
            }
        });

        String prefixo = request.usarEmail() ? "E-mail: " : "Telefone: ";
        AuditoriaService.definirDetalhes(prefixo + identificador);
        return mensagemGenericaRecuperacao(sugestaoLoginGoogle[0]);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public MensagemResponse validarCodigoRecuperacao(ValidarCodigoRecuperacaoRequest request) {
        Usuario usuario = (request.usarEmail()
                ? usuarioRepository.findByEmail(request.emailNormalizado())
                : usuarioRepository.findByTelefone(request.telefoneNormalizado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código de recuperação inválido ou expirado!"));

        LocalDateTime agora = LocalDateTime.now();

        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository
                .findFirstByUsuarioIdAndUsadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código de recuperação inválido ou expirado!"));

        if (token.expirado()) {
            tokenRecuperacaoSenhaExpiracaoService.encerrarSeExpirado(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de recuperação inválido ou expirado!");
        }

        if (!request.codigo().equals(token.getCodigo())) {
            token.setTentativasErradas(token.getTentativasErradas() + 1);
            if (token.getTentativasErradas() >= MAX_TENTATIVAS_RECUPERACAO) {
                token.setEncerradoEm(agora);
                tokenRecuperacaoSenhaRepository.saveAndFlush(token);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código bloqueado após muitas tentativas incorretas. Solicite uma nova recuperação de senha.");
            }
            tokenRecuperacaoSenhaRepository.saveAndFlush(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de recuperação inválido!");
        }

        String prefixo = request.usarEmail() ? "E-mail: " : "Telefone: ";
        AuditoriaService.definirDetalhes(prefixo +
                (request.usarEmail() ? usuario.getEmail() : usuario.getTelefone()));
        return new MensagemResponse("Código válido.");
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public MensagemResponse redefinirSenha(RedefinirSenhaRequest request, String ip) {
        Usuario usuario = (request.usarEmail()
                ? usuarioRepository.findByEmail(request.emailNormalizado())
                : usuarioRepository.findByTelefone(request.telefoneNormalizado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código de recuperação inválido ou expirado!"));

        LocalDateTime agora = LocalDateTime.now();

        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository
                .findFirstByUsuarioIdAndUsadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código de recuperação inválido ou expirado!"));

        if (token.expirado()) {
            tokenRecuperacaoSenhaExpiracaoService.encerrarSeExpirado(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de recuperação inválido ou expirado!");
        }

        if (!request.codigo().equals(token.getCodigo())) {
            token.setTentativasErradas(token.getTentativasErradas() + 1);
            if (token.getTentativasErradas() >= MAX_TENTATIVAS_RECUPERACAO) {
                token.setEncerradoEm(agora);
                tokenRecuperacaoSenhaRepository.saveAndFlush(token);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código bloqueado após muitas tentativas incorretas. Solicite uma nova recuperação de senha.");
            }
            tokenRecuperacaoSenhaRepository.saveAndFlush(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de recuperação inválido!");
        }

        politicaSenhaService.validar(request.novaSenha());

        if (usuario.possuiSenha() && passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da atual!");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.saveAndFlush(usuario);

        token.setUsadoEm(agora);
        token.setEncerradoEm(agora);
        tokenRecuperacaoSenhaRepository.saveAndFlush(token);

        emailService.enviarNotificacaoRedefinicaoSenha(usuario.getNome(), usuario.getEmail(), ip, agora);

        String prefixo = request.usarEmail() ? "E-mail: " : "Telefone: ";
        AuditoriaService.definirDetalhes(prefixo +
                (request.usarEmail() ? usuario.getEmail() : usuario.getTelefone()));
        return new MensagemResponse("Senha redefinida com sucesso!");
    }

    private RecuperacaoIniciarResponse mensagemGenericaRecuperacao(boolean sugestaoLoginGoogle) {
        return new RecuperacaoIniciarResponse(
                "Enviaremos as instruções de recuperação caso exista uma conta vinculada a esse identificador!",
                sugestaoLoginGoogle);
    }
}
