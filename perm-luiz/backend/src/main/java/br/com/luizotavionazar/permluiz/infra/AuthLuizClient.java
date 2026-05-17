package br.com.luizotavionazar.permluiz.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AuthLuizClient {

    private final RestClient restClient = RestClient.create();

    @Value("${auth.luiz.base-url}")
    private String baseUrl;

    @Value("${auth.luiz.service-key}")
    private String serviceKey;

    public List<UsuarioAuthResponse> buscarTodosUsuarios() {
        return restClient.get()
                .uri(baseUrl + "/auth/interno/usuarios")
                .header("X-Service-Key", serviceKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public boolean tokenEstaInvalidado(String jti) {
        try {
            Boolean invalido = restClient.get()
                    .uri(baseUrl + "/auth/interno/tokens/" + jti + "/invalido")
                    .header("X-Service-Key", serviceKey)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(invalido);
        } catch (Exception e) {
            return false;
        }
    }
}
