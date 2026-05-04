package br.com.luizotavionazar.authluiz.domain.identidadeexterna.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginResponse;
import br.com.luizotavionazar.authluiz.api.oauth.dto.DesvincularGoogleRequest;
import br.com.luizotavionazar.authluiz.api.oauth.dto.GoogleLoginRequest;
import br.com.luizotavionazar.authluiz.config.security.JwtService;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.IdentidadeExterna;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UsuarioRepository usuarioRepository;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdTokenValidatorService googleIdTokenValidatorService;

    @Transactional
    public LoginResponse autenticar(GoogleLoginRequest request) {
        Jwt jwt = googleIdTokenValidatorService.validar(request.idToken());
        GoogleUsuarioInfo googleUsuario = extrairUsuario(jwt);

        IdentidadeExterna identidadeExistente = identidadeExternaRepository
                .findByProviderAndProviderUserId(ProviderExterno.GOOGLE, googleUsuario.providerUserId())
                .orElse(null);

        if (identidadeExistente != null) {
            Usuario usuario = identidadeExistente.getUsuario();
            usuarioRepository.atualizarUltimoLogin(usuario.getId(), LocalDateTime.now());
            AuditoriaService.definirDetalhes("E-mail: " + googleUsuario.emailNormalizado());
            return gerarRespostaLogin(usuario);
        }

        if (usuarioRepository.existsByEmail(googleUsuario.emailNormalizado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma conta com este e-mail. Faça login com sua senha e vincule a conta Google posteriormente!");
        }

        if (!googleUsuario.emailVerificado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não foi possível criar a conta com Google porque o e-mail não foi confirmado pelo Google!");
        }

        Usuario novoUsuario = usuarioRepository.save(Usuario.builder()
                .nome(googleUsuario.nomeNormalizado())
                .email(googleUsuario.emailNormalizado())
                .senhaHash(null)
                .providerOrigem(ProviderExterno.GOOGLE)
                .ultimoLogin(LocalDateTime.now())
                .build());

        criarVinculoGoogle(novoUsuario, googleUsuario);
        AuditoriaService.definirDetalhes("E-mail: " + googleUsuario.emailNormalizado());
        return gerarRespostaLogin(novoUsuario);
    }

    @Transactional
    public ContaResponse vincular(Integer idUsuario, GoogleLoginRequest request) {
        Jwt googleJwt = googleIdTokenValidatorService.validar(request.idToken());
        GoogleUsuarioInfo googleUsuario = extrairUsuario(googleJwt);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de vincular uma conta Google!");
        }

        if (!googleUsuario.emailNormalizado().equals(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O e-mail da conta Google deve ser igual ao e-mail da sua conta!");
        }

        if (identidadeExternaRepository.existsByUsuarioIdAndProvider(idUsuario, ProviderExterno.GOOGLE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta conta já está vinculada ao Google!");
        }

        if (identidadeExternaRepository
                .findByProviderAndProviderUserId(ProviderExterno.GOOGLE, googleUsuario.providerUserId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta conta Google já está vinculada a outra conta!");
        }

        criarVinculoGoogle(usuario, googleUsuario);
        return ContaResponse.from(usuario, true);
    }

    @Transactional
    public ContaResponse desvincular(Integer idUsuario, DesvincularGoogleRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));

        if (!identidadeExternaRepository.existsByUsuarioIdAndProvider(idUsuario, ProviderExterno.GOOGLE)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Esta conta não está vinculada ao Google!");
        }

        if (ProviderExterno.GOOGLE.equals(usuario.getProviderOrigem())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta conta foi criada com Google e não pode ser desvinculada!");
        }

        if (!usuario.possuiSenha()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Defina uma senha antes de desvincular o Google para não perder o acesso à conta!");
        }

        String senha = request != null ? request.senha() : null;
        if (senha == null || senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a senha para confirmar a desvinculação!");
        }

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Senha incorreta!");
        }

        identidadeExternaRepository.deleteByUsuarioIdAndProvider(idUsuario, ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, false);
    }

    private void criarVinculoGoogle(Usuario usuario, GoogleUsuarioInfo googleUsuario) {
        IdentidadeExterna identidadeExterna = IdentidadeExterna.builder()
                .usuario(usuario)
                .provider(ProviderExterno.GOOGLE)
                .providerUserId(googleUsuario.providerUserId())
                .emailProvider(googleUsuario.emailNormalizado())
                .emailVerificadoProvider(googleUsuario.emailVerificado())
                .build();
        identidadeExternaRepository.save(identidadeExterna);
    }

    private LoginResponse gerarRespostaLogin(Usuario usuario) {
        String token = jwtService.gerarToken(usuario);
        return LoginResponse.from(usuario, true, token, jwtService.getExpirationMinutes());
    }

    private GoogleUsuarioInfo extrairUsuario(Jwt jwt) {
        String providerUserId = jwt.getSubject();
        String email = claimComoString(jwt, "email");
        boolean emailVerificado = claimComoBoolean(jwt, "email_verified");
        String nome = claimComoString(jwt, "name");

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O idToken do Google não trouxe o identificador do usuário!");
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O idToken do Google não trouxe um e-mail utilizável!");
        }

        String nomeNormalizado = (nome == null || nome.isBlank())
                ? email.trim()
                : nome.trim();

        return new GoogleUsuarioInfo(
                providerUserId,
                email.trim().toLowerCase(),
                emailVerificado,
                nomeNormalizado);
    }

    private String claimComoString(Jwt jwt, String nomeClaim) {
        Object valor = jwt.getClaim(nomeClaim);
        return valor == null ? null : valor.toString();
    }

    private boolean claimComoBoolean(Jwt jwt, String nomeClaim) {
        Object valor = jwt.getClaim(nomeClaim);
        if (valor instanceof Boolean bool) {
            return bool;
        }
        return valor != null && Boolean.parseBoolean(valor.toString());
    }

    private record GoogleUsuarioInfo(
            String providerUserId,
            String emailNormalizado,
            boolean emailVerificado,
            String nomeNormalizado) {
    }
}
