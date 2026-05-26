package br.com.luizotavionazar.authluiz.domain.usuario.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.service.PoliticaSenhaService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaSenhaService politicaSenhaService;
    private final UsernameValidator usernameValidator;

    @Transactional
    public Usuario cadastrar(String username, String nome, String email, String senha, String telefone) {
        String usernameNormalizado = username.strip().toLowerCase();
        String emailNormalizado = email.strip().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("E-mail já cadastrado!");
        }

        usernameValidator.validar(usernameNormalizado);

        if (usuarioRepository.existsByUsername(usernameNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username já em uso!");
        }

        politicaSenhaService.validar(senha);

        String telefoneNormalizado = (telefone != null && !telefone.isBlank()) ? telefone.trim() : null;

        if (telefoneNormalizado != null && usuarioRepository.existsByTelefone(telefoneNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já cadastrado!");
        }

        Usuario usuario = Usuario.builder()
                .username(usernameNormalizado)
                .nome(nome.strip())
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(senha))
                .telefone(telefoneNormalizado)
                .build();

        return usuarioRepository.save(usuario);
    }
}
