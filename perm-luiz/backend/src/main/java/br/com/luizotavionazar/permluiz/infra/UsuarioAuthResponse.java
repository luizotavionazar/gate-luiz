package br.com.luizotavionazar.permluiz.infra;

import java.time.LocalDateTime;

public record UsuarioAuthResponse(
        String publicId,
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
) {}
