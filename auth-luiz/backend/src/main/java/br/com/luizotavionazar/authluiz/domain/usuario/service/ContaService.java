package br.com.luizotavionazar.authluiz.domain.usuario.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarEmailRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarNomeRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarSenhaRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarTelefoneRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.DeletarContaRequest;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.ControleAlteracaoEmail;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.ControleAlteracaoEmailRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenRecuperacaoSenhaRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.PoliticaSenhaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.TokenConfirmacaoService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.notificacao.service.EmailService;
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

    private static final long JANELA_ALTERACAO_EMAIL_MINUTES = 1440;
    private static final int LIMITE_ALTERACAO_EMAIL = 5;
    private static final long BLOQUEIO_ALTERACAO_EMAIL_MINUTES = 1440;

    private final UsuarioRepository usuarioRepository;
    private final IdentidadeExternaRepository identidadeExternaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaSenhaService politicaSenhaService;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final TokenConfirmacaoService tokenConfirmacaoService;
    private final ControleAlteracaoEmailRepository controleAlteracaoEmailRepository;
    private final EmailService emailService;

    @Transactional
    public ContaResponse obterMinhaConta(Integer idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);

        if (usuario.getEmailPendente() != null
                && !tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
            usuarioRepository.atualizarEmailPendente(idUsuario, null);
            usuario.setEmailPendente(null);
        }

        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarNome(Integer idUsuario, AtualizarNomeRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar o nome!");
        }

        usuario.setNome(request.nomeNormalizado());
        usuarioRepository.save(usuario);
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarEmail(Integer idUsuario, AtualizarEmailRequest request, String ip) {
        Usuario usuario = buscarUsuario(idUsuario);

        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        if (temLoginGoogle) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contas vinculadas ao Google não podem alterar o e-mail!");
        }

        String emailNormalizado = request.emailNormalizado();

        if (usuarioRepository.existsByEmailAndIdNot(emailNormalizado, usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado!");
        }

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail atual antes de solicitar uma alteração!");
        }

        if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns instantes antes de solicitar uma nova confirmação de e-mail!");
        }

        validarLimiteAlteracaoEmail(idUsuario);

        usuarioRepository.atualizarEmailPendente(usuario.getId(), emailNormalizado);
        usuario.setEmailPendente(emailNormalizado); // apenas para o DTO da resposta, entidade já desanexada

        String token = tokenConfirmacaoService.criarTokenAlteracaoEmail(usuario, emailNormalizado, ip);
        emailService.enviarConfirmacaoAlteracaoEmail(usuario.getNome(), emailNormalizado, token);

        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarTelefone(Integer idUsuario, AtualizarTelefoneRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar o telefone!");
        }

        String telefoneNormalizado = (request.telefone() != null && !request.telefone().isBlank())
                ? request.telefone().trim()
                : null;

        if (telefoneNormalizado != null
                && usuarioRepository.existsByTelefoneAndIdNot(telefoneNormalizado, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já cadastrado!");
        }

        usuario.setTelefone(telefoneNormalizado);
        usuario.setTelefoneVerificado(false);
        usuarioRepository.save(usuario);

        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public MensagemResponse atualizarSenha(Integer idUsuario, AtualizarSenhaRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar a senha!");
        }

        politicaSenhaService.validar(request.novaSenha());

        if (usuario.possuiSenha()) {
            String senhaAtual = request.senhaAtual();
            if (senhaAtual == null || senhaAtual.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe a senha atual para alterar a senha!");
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
        return new MensagemResponse("Senha definida com sucesso!");
    }

    @Transactional
    public void deletarConta(Integer idUsuario, DeletarContaRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);

        if (usuario.possuiSenha()) {
            String senha = request != null ? request.senha() : null;
            if (senha == null || senha.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe a senha para confirmar a exclusão da conta!");
            }
            if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta!");
            }
        }

        usuarioRepository.delete(usuario);
    }

    private void validarLimiteAlteracaoEmail(Integer idUsuario) {
        LocalDateTime agora = LocalDateTime.now();

        ControleAlteracaoEmail controle = controleAlteracaoEmailRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> ControleAlteracaoEmail.builder()
                        .idUsuario(idUsuario)
                        .janelaInicio(agora)
                        .quantidade(0)
                        .build());

        if (controle.getBloqueadoAte() != null && agora.isBefore(controle.getBloqueadoAte())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas solicitações de alteração de e-mail. Tente novamente mais tarde!");
        }

        if (controle.getJanelaInicio() == null
                || agora.isAfter(controle.getJanelaInicio().plusMinutes(JANELA_ALTERACAO_EMAIL_MINUTES))) {
            controle.setJanelaInicio(agora);
            controle.setQuantidade(1);
            controle.setBloqueadoAte(null);
            controleAlteracaoEmailRepository.save(controle);
            return;
        }

        int novaQuantidade = controle.getQuantidade() + 1;
        controle.setQuantidade(novaQuantidade);

        if (novaQuantidade > LIMITE_ALTERACAO_EMAIL) {
            controle.setBloqueadoAte(agora.plusMinutes(BLOQUEIO_ALTERACAO_EMAIL_MINUTES));
            controleAlteracaoEmailRepository.save(controle);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas solicitações de alteração de e-mail. Tente novamente mais tarde!");
        }

        controleAlteracaoEmailRepository.save(controle);
    }

    private Usuario buscarUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));
    }
}
