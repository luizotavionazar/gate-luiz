package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados completos da conta do usuário (sem token)")
public record ContaResponse(
        @Schema(description = "ID público imutável (UUID v4)", example = "550e8400-e29b-41d4-a716-446655440000")
        String publicId,
        @Schema(description = "Username único público", example = "joao_silva")
        String username,
        @Schema(description = "Nome de exibição", example = "João Silva")
        String nome,
        @Schema(description = "E-mail atual da conta", example = "joao@email.com")
        String email,
        @Schema(description = "Se a conta possui senha definida")
        Boolean temSenha,
        @Schema(description = "Se o Google está vinculado à conta")
        Boolean temLoginGoogle,
        @Schema(description = "Se o e-mail foi verificado")
        Boolean emailVerificado,
        @Schema(description = "Novo e-mail aguardando confirmação (null se não há alteração pendente)", nullable = true)
        String emailPendente,
        @Schema(description = "Provider de origem da conta (GOOGLE ou null para e-mail/senha)", nullable = true, example = "GOOGLE")
        String providerOrigem,
        @Schema(description = "Telefone em formato E.164", example = "+5511987654321", nullable = true)
        String telefone,
        @Schema(description = "Se o telefone foi verificado")
        Boolean telefoneVerificado,
        @Schema(description = "Novo telefone aguardando confirmação (null se não há alteração pendente)", nullable = true)
        String telefonePendente,
        @Schema(description = "Data de criação da conta")
        LocalDateTime dataCriacao,
        @Schema(description = "Data da última atualização")
        LocalDateTime dataAtualiza) {
    public static ContaResponse from(Usuario usuario, boolean temLoginGoogle) {
        ProviderExterno provider = usuario.getProviderOrigem();
        return new ContaResponse(
                usuario.getPublicId(),
                usuario.getUsername(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.possuiSenha(),
                temLoginGoogle,
                usuario.isEmailVerificado(),
                usuario.getEmailPendente(),
                provider != null ? provider.name() : null,
                usuario.getTelefone(),
                usuario.isTelefoneVerificado(),
                usuario.getTelefonePendente(),
                usuario.getDataCriacao(),
                usuario.getDataAtualiza());
    }
}
