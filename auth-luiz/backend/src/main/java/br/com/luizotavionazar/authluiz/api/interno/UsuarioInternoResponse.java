package br.com.luizotavionazar.authluiz.api.interno;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioInternoResponse(
        String publicId,
        String username,
        String nome,
        String email,
        String telefone,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualiza,
        LocalDateTime ultimoLogin,
        boolean emailVerificado,
        boolean telefoneVerificado,
        boolean possuiSenha,
        boolean googleVinculado,
        boolean verificacaoExtraAtiva,
        boolean totpAtivo,
        int codigosBackupRestantes,
        int ipsConfiaveis
) {
    public static UsuarioInternoResponse de(Usuario u, boolean googleVinculado,
                                            int codigosBackupRestantes, int ipsConfiaveis) {
        return new UsuarioInternoResponse(
                u.getPublicId(),
                u.getUsername(),
                u.getNome(),
                u.getEmail(),
                u.getTelefone(),
                u.getDataCriacao(),
                u.getDataAtualiza(),
                u.getUltimoLogin(),
                u.isEmailVerificado(),
                u.isTelefoneVerificado(),
                u.possuiSenha(),
                googleVinculado,
                u.isVerificacaoExtraAtiva(),
                u.isTotpAtivo(),
                codigosBackupRestantes,
                ipsConfiaveis
        );
    }
}
