package br.com.luizotavionazar.permluiz.infra;

import java.time.LocalDateTime;

public record UsuarioAuthResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualiza,
        LocalDateTime ultimoLogin,
        boolean emailVerificado,
        boolean telefoneVerificado,
        boolean possuiSenha,
        boolean googleVinculado
) {}
