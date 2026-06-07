package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Resposta quando o login exige verificação adicional (2FA ou IP desconhecido)")
public record LoginPendenteResponse(
        @Schema(description = "Token opaco que deve ser passado em `/auth/login/verificar` ou `/auth/login/reenviar`")
        String tokenPendente,

        @Schema(description = "Tipo de verificação exigida",
                allowableValues = {"TOTP", "EMAIL", "SMS", "WHATSAPP", "AGUARDANDO_CANAL"},
                example = "EMAIL")
        String tipo,

        @Schema(description = "Destino mascarado para onde o código foi enviado (null para TOTP e AGUARDANDO_CANAL)",
                example = "jo***@email.com", nullable = true)
        String destinoMascarado,

        @Schema(description = "Canais alternativos disponíveis para reenvio do código",
                example = "[\"SMS\", \"WHATSAPP\"]")
        List<String> canaisDisponiveis,

        @Schema(description = "Minutos restantes até o token expirar", example = "5")
        int expiresInMinutes,

        @Schema(description = "Mensagem informativa", example = "Verificação adicional necessária para este acesso.")
        String mensagem
) {
    public static LoginPendenteResponse from(LoginPendente lp, Usuario usuario) {
        return new LoginPendenteResponse(
                lp.getTokenPendente(),
                lp.getTipo(),
                mascarar(lp.getTipo(), usuario),
                canaisAlternativos(lp.getTipo(), usuario),
                (int) Math.ceil(ChronoUnit.SECONDS.between(LocalDateTime.now(), lp.getExpiraEm()) / 60.0),
                "Verificação adicional necessária para este acesso."
        );
    }

    private static List<String> canaisAlternativos(String tipo, Usuario usuario) {
        if ("TOTP".equals(tipo)) return List.of();
        if ("AGUARDANDO_CANAL".equals(tipo)) {
            List<String> canais = new ArrayList<>();
            canais.add("EMAIL");
            if (usuario.getTelefone() != null && usuario.isTelefoneVerificado()) {
                canais.add("WHATSAPP");
                canais.add("SMS");
            }
            return canais;
        }
        List<String> canais = new ArrayList<>();
        if ("EMAIL".equals(tipo) && usuario.getTelefone() != null && usuario.isTelefoneVerificado()) {
            canais.add("SMS");
            canais.add("WHATSAPP");
        }
        if (!"EMAIL".equals(tipo)) {
            canais.add("EMAIL");
        }
        return canais;
    }

    public static String mascarar(String tipo, Usuario usuario) {
        return switch (tipo) {
            case "TOTP", "AGUARDANDO_CANAL" -> null;
            case "EMAIL" -> mascararEmail(usuario.getEmail());
            default -> mascararTelefone(usuario.getTelefone());
        };
    }

    private static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        String[] partes = email.split("@");
        String local = partes[0];
        String visivel = local.length() <= 2 ? local.charAt(0) + "***" : local.substring(0, 2) + "***";
        return visivel + "@" + partes[1];
    }

    private static String mascararTelefone(String telefone) {
        if (telefone == null || telefone.length() < 6) return null;
        return telefone.substring(0, 3) + " ***-" + telefone.substring(telefone.length() - 4);
    }
}
