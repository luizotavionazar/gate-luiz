package br.com.luizotavionazar.authluiz.domain.identidadeexterna.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginResponse;
import br.com.luizotavionazar.authluiz.api.oauth.dto.GoogleLoginRequest;
import br.com.luizotavionazar.authluiz.config.security.JwtService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.IdentidadeExterna;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UsuarioRepository usuarioRepository;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenValidatorService googleIdTokenValidatorService;

    @Transactional
    public LoginResponse autenticar(GoogleLoginRequest request) {
        Jwt jwt = googleIdTokenValidatorService.validar(request.idToken());
        GoogleUsuarioInfo googleUsuario = extrairUsuario(jwt);

        IdentidadeExterna identidadeExistente = identidadeExternaRepository
                .findByProviderAndProviderUserId(ProviderExterno.GOOGLE, googleUsuario.providerUserId())
                .orElse(null);

        if (identidadeExistente != null) {
            return gerarRespostaLogin(identidadeExistente.getUsuario());
        }

        Usuario usuarioExistente = usuarioRepository.findByEmail(googleUsuario.emailNormalizado()).orElse(null);
        if (usuarioExistente != null) {
            return vincularContaExistenteOuFalhar(request, googleUsuario, usuarioExistente);
        }

        if (!googleUsuario.emailVerificado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não foi possível criar a conta com Google porque o e-mail não foi confirmado pelo Google!");
        }

        Usuario novoUsuario = usuarioRepository.save(Usuario.builder()
                .nome(googleUsuario.nomeNormalizado())
                .email(googleUsuario.emailNormalizado())
                .senhaHash(null)
                .build());

        criarVinculoGoogle(novoUsuario, googleUsuario);
        return gerarRespostaLogin(novoUsuario);
    }

    private LoginResponse vincularContaExistenteOuFalhar(
            GoogleLoginRequest request,
            GoogleUsuarioInfo googleUsuario,
            Usuario usuarioExistente
    ) {
        if (!googleUsuario.emailVerificado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma conta com este e-mail, mas o Google não informou o e-mail como verificado. Faça login com sua senha para vincular depois!");
        }

        if (!request.desejaVincularContaExistente()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma conta com este e-mail. Confirme se deseja entrar com Google e vincular essa conta.");
        }

        criarVinculoGoogle(usuarioExistente, googleUsuario);
        return gerarRespostaLogin(usuarioExistente);
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
                nomeNormalizado
        );
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
            String nomeNormalizado
    ) {
    }
}
