package br.com.luizotavionazar.authluiz.api.jwks;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@Tag(name = "JWKS")
@RestController
@RequestMapping("/auth/.well-known")
@RequiredArgsConstructor
public class JwksController {

    private final RSAPublicKey rsaPublicKey;

    @Operation(summary = "Chave pública RSA (JWKS)",
            description = "Retorna a chave pública RSA-2048 em formato JWKS (RFC 7517). " +
                    "Usada por serviços externos (ex: PermLuiz) para validar JWTs emitidos por este serviço sem compartilhar segredo.")
    @ApiResponse(responseCode = "200", description = "JWKS com a chave pública")
    @GetMapping("/jwks.json")
    Map<String, Object> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey)
                .keyID("authluiz-key")
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}
