package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.api.conta.dto.*;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import br.com.luizotavionazar.authluiz.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoisFatoresService {

    private final UsuarioRepository usuarioRepository;
    private final TotpService totpService;
    private final CodigoBackupService codigoBackupService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.totp.issuer:AuthLuiz}")
    private String totpIssuer;

    public IniciarTotpResponse iniciarTotp(String publicId) {
        Usuario usuario = buscar(publicId);

        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de ativar a autenticação de dois fatores.");
        }

        String segredo = totpService.gerarSegredo();
        String segredoCriptografado = totpService.criptografar(segredo);
        usuarioRepository.atualizarTotpSecretPendente(usuario.getId(), segredoCriptografado);

        String uri = totpService.gerarOtpauthUri(segredo, usuario.getEmail(), totpIssuer);
        return new IniciarTotpResponse(uri);
    }

    public ConfirmarTotpResponse confirmarTotp(String publicId, String codigo) {
        Usuario usuario = buscar(publicId);

        if (usuario.getTotpSecretPendente() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhuma configuração TOTP pendente. Inicie o setup novamente.");
        }

        if (!totpService.validarCodigo(usuario.getTotpSecretPendente(), codigo)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código TOTP inválido.");
        }

        usuarioRepository.ativarTotp(usuario.getId(), usuario.getTotpSecretPendente());

        List<String> codigos = codigoBackupService.gerarParaUsuario(usuario.getId());
        return new ConfirmarTotpResponse(codigos);
    }

    public void desativar(String publicId, String senha) {
        Usuario usuario = buscar(publicId);

        if (!usuario.possuiSenha() || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
        }

        usuarioRepository.desativarTotp(usuario.getId());
        codigoBackupService.deletarParaUsuario(usuario.getId());
    }

    public ConfirmarTotpResponse regerarBackupCodes(String publicId, String codigo) {
        Usuario usuario = buscar(publicId);

        if (!usuario.isTotpAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOTP não está ativo.");
        }

        if (!totpService.validarCodigo(usuario.getTotpSecret(), codigo)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código TOTP inválido.");
        }

        List<String> codigos = codigoBackupService.gerarParaUsuario(usuario.getId());
        return new ConfirmarTotpResponse(codigos);
    }

    public void atualizarVerificacaoExtra(String publicId, boolean ativo, String senha) {
        Usuario usuario = buscar(publicId);
        if (!usuario.isEmailVerificado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Confirme seu e-mail antes de alterar as configurações de autenticação.");
        }
        if (!ativo && usuario.isTotpAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Desative o autenticador antes de desabilitar a verificação extra.");
        }
        if (!ativo && usuario.possuiSenha()) {
            if (senha == null || senha.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe a senha para desativar a verificação extra.");
            }
            if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
            }
        }
        usuarioRepository.atualizarVerificacaoExtra(usuario.getId(), ativo);
        AuditoriaService.definirDetalhes(ativo ? "Verificação extra ativada" : "Verificação extra desativada");
    }

    @Transactional(readOnly = true)
    public DoisFatoresStatusResponse obterStatus(String publicId) {
        Usuario usuario = buscar(publicId);
        int codigosRestantes = codigoBackupService.contar(usuario.getId());
        return new DoisFatoresStatusResponse(usuario.isTotpAtivo(), codigosRestantes, usuario.isVerificacaoExtraAtiva());
    }

    private Usuario buscar(String publicId) {
        return usuarioRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
