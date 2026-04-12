package br.com.luizotavionazar.authluiz.domain.usuario.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarEmailRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarNomeRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarSenhaRequest;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenRecuperacaoSenhaRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.PoliticaSenhaService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final UsuarioRepository usuarioRepository;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaSenhaService politicaSenhaService;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;

    @Transactional(readOnly = true)
    public ContaResponse obterMinhaConta(Integer idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(), ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarNome(Integer idUsuario, AtualizarNomeRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);
        usuario.setNome(request.nomeNormalizado());
        usuarioRepository.save(usuario);
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(), ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarEmail(Integer idUsuario, AtualizarEmailRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);
        String emailNormalizado = request.emailNormalizado();

        if (usuarioRepository.existsByEmailAndIdNot(emailNormalizado, usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado!");
        }

        usuario.setEmail(emailNormalizado);
        usuarioRepository.save(usuario);
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(), ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public MensagemResponse atualizarSenha(Integer idUsuario, AtualizarSenhaRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);
        politicaSenhaService.validar(request.novaSenha());

        if (usuario.possuiSenhaLocal()) {
            String senhaAtual = request.senhaAtual();
            if (senhaAtual == null || senhaAtual.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe a senha atual para alterar a senha local!");
            }
            if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "A senha atual informada é inválida!");
            }
            if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A nova senha deve ser diferente da atual!");
            }

            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
            usuarioRepository.save(usuario);
            tokenRecuperacaoSenhaRepository.encerrarTokensAbertosDoUsuario(usuario.getId(), LocalDateTime.now());
            return new MensagemResponse("Senha alterada com sucesso!");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        tokenRecuperacaoSenhaRepository.encerrarTokensAbertosDoUsuario(usuario.getId(), LocalDateTime.now());
        return new MensagemResponse("Senha local definida com sucesso!");
    }

    private Usuario buscarUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));
    }
}
