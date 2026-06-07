package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de login bem-sucedido")
public record LoginResponse(
        @Schema(description = "ID público imutável do usuário (UUID v4)", example = "550e8400-e29b-41d4-a716-446655440000")
        String publicId,
        @Schema(description = "Username único do usuário", example = "joao_silva")
        String username,
        @Schema(description = "Nome de exibição", example = "João Silva")
        String nome,
        @Schema(description = "E-mail da conta", example = "joao@email.com")
        String email,
        @Schema(description = "Se o usuário possui senha definida (false para contas só com Google)")
        Boolean temSenha,
        @Schema(description = "Se o Google está vinculado à conta")
        Boolean temLoginGoogle,
        @Schema(description = "Se o e-mail foi verificado")
        Boolean emailVerificado,
        @Schema(description = "Telefone em formato E.164", example = "+5511987654321", nullable = true)
        String telefone,
        @Schema(description = "Se o telefone foi verificado")
        Boolean telefoneVerificado,
        @Schema(description = "JWT RS256 para autenticar nas próximas requisições")
        String token,
        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,
        @Schema(description = "Tempo de expiração do token em minutos", example = "120")
        Long expiresInMinutes,
        @Schema(description = "Mensagem de confirmação", example = "Login realizado com sucesso")
        String mensagem) {
    public static LoginResponse from(Usuario usuario, boolean temLoginGoogle, String token, long expiresInMinutes) {
        return new LoginResponse(
                usuario.getPublicId(),
                usuario.getUsername(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.possuiSenha(),
                temLoginGoogle,
                usuario.isEmailVerificado(),
                usuario.getTelefone(),
                usuario.isTelefoneVerificado(),
                token,
                "Bearer",
                expiresInMinutes,
                "Login realizado com sucesso");
    }
}
