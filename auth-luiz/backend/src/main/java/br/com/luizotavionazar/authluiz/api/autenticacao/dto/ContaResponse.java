package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

import java.time.LocalDateTime;

public record ContaResponse(
        Integer idUsuario,
        String nome,
        String email,
        Boolean temSenha,
        Boolean temLoginGoogle,
        Boolean emailVerificado,
        String emailPendente,
        String providerOrigem,
        String telefone,
        Boolean telefoneVerificado,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualiza) {
    public static ContaResponse from(Usuario usuario, boolean temLoginGoogle) {
        ProviderExterno provider = usuario.getProviderOrigem();
        return new ContaResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.possuiSenha(),
                temLoginGoogle,
                usuario.isEmailVerificado(),
                usuario.getEmailPendente(),
                provider != null ? provider.name() : null,
                usuario.getTelefone(),
                usuario.isTelefoneVerificado(),
                usuario.getDataCriacao(),
                usuario.getDataAtualiza());
    }
}
