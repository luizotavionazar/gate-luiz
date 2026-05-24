package br.com.luizotavionazar.authluiz.api.autenticacao.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public record LoginPendenteResponse(
        String tokenPendente,
        String tipo,
        String destinoMascarado,
        List<String> canaisDisponiveis,
        int expiresInMinutes,
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
