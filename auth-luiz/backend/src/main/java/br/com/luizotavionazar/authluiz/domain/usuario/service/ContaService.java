package br.com.luizotavionazar.authluiz.domain.usuario.service;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.ContaResponse;
import br.com.luizotavionazar.authluiz.api.autenticacao.dto.LoginPendenteResponse;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarEmailRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarNomeRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarSenhaRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarTelefoneRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.AtualizarUsernameRequest;
import br.com.luizotavionazar.authluiz.api.conta.dto.DeletarContaRequest;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.LoginPendenteService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.TotpService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.ControleAlteracaoEmail;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.api.common.exception.ExcecaoLimiteTentativas;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.ControleAlteracaoEmailRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenRecuperacaoSenhaRepository;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.EnvioCodigoRateLimitService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.PoliticaSenhaService;
import br.com.luizotavionazar.authluiz.domain.autenticacao.service.TokenConfirmacaoService;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository.IdentidadeExternaRepository;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.notificacao.port.NotificacaoTelefonePort;
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
    private final UsernameValidator usernameValidator;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final TokenConfirmacaoService tokenConfirmacaoService;
    private final ControleAlteracaoEmailRepository controleAlteracaoEmailRepository;
    private final EmailService emailService;
    private final NotificacaoTelefonePort notificacaoTelefonePort;
    private final EnvioCodigoRateLimitService envioCodigoRateLimitService;
    private final br.com.luizotavionazar.authluiz.domain.autenticacao.service.IpConfiavelService ipConfiavelService;
    private final LoginPendenteService loginPendenteService;
    private final TotpService totpService;

    @Transactional
    public ContaResponse obterMinhaConta(String publicId) {
        Usuario usuario = buscarUsuario(publicId);
        Integer idUsuario = usuario.getId();

        if (usuario.getEmailPendente() != null
                && !tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
            usuarioRepository.atualizarEmailPendente(idUsuario, null);
            usuario.setEmailPendente(null);
        }

        if (usuario.getTelefonePendente() != null
                && !tokenConfirmacaoService.temTokenAtivo(idUsuario, TipoTokenConfirmacao.ALTERACAO_TELEFONE)) {
            usuarioRepository.atualizarTelefonePendente(idUsuario, null);
            usuario.setTelefonePendente(null);
        }

        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(idUsuario,
                ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarUsername(String publicId, AtualizarUsernameRequest request) {
        Usuario usuario = buscarUsuario(publicId);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar o username!");
        }

        String novoUsername = request.usernameSanitizado();
        usernameValidator.validar(novoUsername);

        if (usuarioRepository.existsByUsernameAndIdNot(novoUsername, usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username já em uso!");
        }

        String anterior = usuario.getUsername();
        usuario.setUsername(novoUsername);
        usuarioRepository.save(usuario);
        AuditoriaService.definirDetalhes("Username alterado de '" + anterior + "' para '" + novoUsername + "'");
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarNome(String publicId, AtualizarNomeRequest request) {
        Usuario usuario = buscarUsuario(publicId);

        String anterior = usuario.getNome();
        usuario.setNome(request.nomeNormalizado());
        usuarioRepository.save(usuario);
        AuditoriaService.definirDetalhes("Nome alterado de '" + anterior + "' para '" + request.nomeNormalizado() + "'");
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public ContaResponse atualizarEmail(String publicId, AtualizarEmailRequest request, String ip) {
        Usuario usuario = buscarUsuario(publicId);

        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);
        if (temLoginGoogle) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contas vinculadas ao Google não podem alterar o e-mail!");
        }

        String emailNormalizado = request.emailNormalizado();

        if (emailNormalizado.equals(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O novo e-mail deve ser diferente do e-mail atual!");
        }

        if (usuarioRepository.existsByEmailAndIdNot(emailNormalizado, usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado!");
        }

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail atual antes de solicitar uma alteração!");
        }

        envioCodigoRateLimitService.validarLimitePorIp(ip);

        Integer idUsuario = usuario.getId();
        if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.ALTERACAO_EMAIL)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns instantes antes de solicitar uma nova confirmação de e-mail!");
        }

        validarLimiteAlteracaoEmail(idUsuario);

        usuarioRepository.atualizarEmailPendente(idUsuario, emailNormalizado);
        usuario.setEmailPendente(emailNormalizado); // apenas para o DTO da resposta, entidade já desanexada

        String codigo = tokenConfirmacaoService.criarTokenAlteracaoEmail(usuario, emailNormalizado, ip);
        emailService.enviarConfirmacaoAlteracaoEmail(usuario.getNome(), emailNormalizado, codigo);
        AuditoriaService.definirDetalhes("E-mail alterado de '" + usuario.getEmail() + "' para '" + emailNormalizado + "'");

        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional(noRollbackFor = ExcecaoLimiteTentativas.class)
    public ContaResponse atualizarTelefone(String publicId, AtualizarTelefoneRequest request, String ip) {
        Usuario usuario = buscarUsuario(publicId);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar o telefone!");
        }

        String telefoneNormalizado = (request.telefone() != null && !request.telefone().isBlank())
                ? request.telefone().trim()
                : null;

        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(usuario.getId(),
                ProviderExterno.GOOGLE);

        // Remoção direta — sem verificação necessária
        if (telefoneNormalizado == null) {
            usuario.setTelefone(null);
            usuario.setTelefonePendente(null);
            usuario.setTelefoneVerificado(false);
            usuarioRepository.save(usuario);
            AuditoriaService.definirDetalhes("Telefone removido");
            return ContaResponse.from(usuario, temLoginGoogle);
        }

        if (telefoneNormalizado.equals(usuario.getTelefone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O novo telefone deve ser diferente do telefone atual!");
        }

        if (usuarioRepository.existsByTelefoneAndIdNot(telefoneNormalizado, usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já cadastrado!");
        }

        envioCodigoRateLimitService.validarLimitePorIp(ip);

        Integer idUsuario = usuario.getId();
        if (tokenConfirmacaoService.estaDentroDoCooldown(idUsuario, TipoTokenConfirmacao.ALTERACAO_TELEFONE)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns instantes antes de solicitar um novo código de verificação!");
        }

        notificacaoTelefonePort.validarDisponibilidade();

        usuarioRepository.atualizarTelefonePendente(idUsuario, telefoneNormalizado);
        usuario.setTelefonePendente(telefoneNormalizado);

        String codigo = tokenConfirmacaoService.criarTokenAlteracaoTelefone(usuario, telefoneNormalizado, ip);
        notificacaoTelefonePort.enviarCodigoVerificacao(telefoneNormalizado, codigo);
        AuditoriaService.definirDetalhes("Alteração de telefone iniciada para '" + telefoneNormalizado + "'");

        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public ContaResponse atualizarSenha(String publicId, AtualizarSenhaRequest request, String ip) {
        Usuario usuario = buscarUsuario(publicId);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar a senha!");
        }

        politicaSenhaService.validar(request.novaSenha());

        LocalDateTime agora = LocalDateTime.now();
        boolean temLoginGoogle = identidadeExternaRepository.existsByUsuarioIdAndProvider(
                usuario.getId(), ProviderExterno.GOOGLE);

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
            tokenRecuperacaoSenhaRepository.encerrarTokensAbertosDoUsuario(usuario.getId(), agora);
            ipConfiavelService.removerTodos(usuario.getId());
            emailService.enviarNotificacaoAlteracaoSenha(usuario.getNome(), usuario.getEmail(), ip, agora);
            AuditoriaService.definirDetalhes("Senha alterada");
            return ContaResponse.from(usuario, temLoginGoogle);
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        tokenRecuperacaoSenhaRepository.encerrarTokensAbertosDoUsuario(usuario.getId(), agora);
        ipConfiavelService.removerTodos(usuario.getId());
        emailService.enviarNotificacaoAlteracaoSenha(usuario.getNome(), usuario.getEmail(), ip, agora);
        AuditoriaService.definirDetalhes("Senha definida pela primeira vez");
        return ContaResponse.from(usuario, temLoginGoogle);
    }

    @Transactional
    public void deletarConta(String publicId, DeletarContaRequest request) {
        Usuario usuario = buscarUsuario(publicId);

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

        if (usuario.isVerificacaoExtraAtiva()) {
            String codigo = request != null ? request.codigo() : null;
            if (codigo == null || codigo.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código de verificação obrigatório.");
            }
            if (usuario.isTotpAtivo()) {
                if (!totpService.validarCodigo(usuario.getTotpSecret(), codigo)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código TOTP inválido.");
                }
            } else {
                String tokenPendente = request.tokenPendente();
                if (tokenPendente == null || tokenPendente.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Token de verificação obrigatório.");
                }
                loginPendenteService.verificar(tokenPendente, codigo);
            }
        }

        usuarioRepository.delete(usuario);
    }

    @Transactional
    public LoginPendenteResponse enviarCodigoExclusaoConta(String publicId, String ip) {
        Usuario usuario = buscarUsuario(publicId);
        if (!usuario.isVerificacaoExtraAtiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verificação extra não está ativa.");
        }
        if (usuario.isTotpAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use o código do seu aplicativo autenticador.");
        }
        LoginPendente lp = loginPendenteService.criar(usuario, ip);
        return LoginPendenteResponse.from(lp, usuario);
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

    private Usuario buscarUsuario(String publicId) {
        return usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada!"));
    }
}
