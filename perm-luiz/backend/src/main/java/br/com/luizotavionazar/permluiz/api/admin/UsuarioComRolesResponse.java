package br.com.luizotavionazar.permluiz.api.admin;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Dados de um usuário do AuthLuiz com seus roles no PermLuiz")
public record UsuarioComRolesResponse(
        @Schema(description = "ID público do usuário no AuthLuiz (UUID v4)", example = "550e8400-e29b-41d4-a716-446655440000")
        String publicId,
        @Schema(description = "Nome de exibição", example = "João Silva")
        String nome,
        @Schema(description = "E-mail da conta", example = "joao@email.com")
        String email,
        @Schema(description = "Telefone em formato E.164", example = "+5511987654321", nullable = true)
        String telefone,
        @Schema(description = "Data de criação da conta")
        LocalDateTime dataCriacao,
        @Schema(description = "Data da última atualização")
        LocalDateTime dataAtualiza,
        @Schema(description = "Data do último login", nullable = true)
        LocalDateTime ultimoLogin,
        @Schema(description = "Se o e-mail foi verificado")
        boolean emailVerificado,
        @Schema(description = "Se o telefone foi verificado")
        boolean telefoneVerificado,
        @Schema(description = "Se o usuário possui senha definida")
        boolean possuiSenha,
        @Schema(description = "Se o Google está vinculado à conta")
        boolean googleVinculado,
        @Schema(description = "Se a verificação extra (2FA) está habilitada")
        boolean verificacaoExtraAtiva,
        @Schema(description = "Se o TOTP está ativo")
        boolean totpAtivo,
        @Schema(description = "Quantidade de backup codes não utilizados", example = "8")
        int codigosBackupRestantes,
        @Schema(description = "Quantidade de IPs confiáveis cadastrados", example = "2")
        int ipsConfiaveis,
        @Schema(description = "Roles atribuídos ao usuário no PermLuiz")
        List<RoleResponse> roles
) {}
