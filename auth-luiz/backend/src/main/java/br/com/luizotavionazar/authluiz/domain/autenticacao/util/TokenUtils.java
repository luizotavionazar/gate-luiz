package br.com.luizotavionazar.authluiz.domain.autenticacao.util;

import java.security.SecureRandom;

public final class TokenUtils {

    private TokenUtils() {}

    public static String gerarCodigoNumerico6Digitos() {
        int codigo = 100000 + new SecureRandom().nextInt(900000);
        return String.valueOf(codigo);
    }
}
