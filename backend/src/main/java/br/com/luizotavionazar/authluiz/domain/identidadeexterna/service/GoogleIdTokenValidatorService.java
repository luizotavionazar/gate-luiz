package br.com.luizotavionazar.authluiz.domain.identidadeexterna.service;

import br.com.luizotavionazar.authluiz.config.security.GoogleAudienceValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleIdTokenValidatorService {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final String googleClientId;
    private volatile JwtDecoder googleIdTokenDecoder;

    public GoogleIdTokenValidatorService(@Value("${google.oauth.client-id}") String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public Jwt validar(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O login com Google ainda não foi configurado na API!");
        }

        try {
            return getDecoder().decode(idToken);
        } catch (BadJwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "O idToken do Google é inválido ou expirou!");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Não foi possível validar o idToken do Google!");
        }
    }

    private JwtDecoder getDecoder() {
        JwtDecoder decoderLocal = googleIdTokenDecoder;
        if (decoderLocal == null) {
            synchronized (this) {
                decoderLocal = googleIdTokenDecoder;
                if (decoderLocal == null) {
                    NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(GOOGLE_ISSUER);
                    OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);
                    OAuth2TokenValidator<Jwt> audienceValidator = new GoogleAudienceValidator(googleClientId);
                    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
                    googleIdTokenDecoder = decoder;
                    decoderLocal = decoder;
                }
            }
        }
        return decoderLocal;
    }
}
