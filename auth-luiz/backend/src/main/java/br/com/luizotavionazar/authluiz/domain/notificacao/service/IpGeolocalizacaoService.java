package br.com.luizotavionazar.authluiz.domain.notificacao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
public class IpGeolocalizacaoService {

    private static final Logger log = LoggerFactory.getLogger(IpGeolocalizacaoService.class);
    private static final Set<String> IPS_LOCALHOST = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");

    public Optional<String> obterLocalizacao(String ip) {
        if (ip == null || ip.isBlank()) {
            log.debug("geo: IP nulo/vazio — linha de localização omitida");
            return Optional.empty();
        }
        if (isIpPrivado(ip)) {
            log.debug("geo: IP privado/local [{}] — linha de localização omitida (esperado em dev/Docker local)", ip);
            return Optional.empty();
        }

        log.debug("geo: consultando localização para IP público [{}]", ip);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip
                            + "?fields=status,city,regionName,country&lang=pt-BR"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Optional<String> resultado = extrairLocalizacao(response.body());
                if (resultado.isPresent()) {
                    log.debug("geo: localização obtida para [{}] → {}", ip, resultado.get());
                } else {
                    log.debug("geo: ip-api.com não retornou localização para [{}] — resposta: {}", ip, response.body());
                }
                return resultado;
            }

            log.warn("geo: ip-api.com retornou HTTP {} para IP [{}]", response.statusCode(), ip);
        } catch (Exception e) {
            log.warn("geo: falha ao consultar localização para IP [{}]: {}", ip, e.getMessage());
        }

        return Optional.empty();
    }

    private Optional<String> extrairLocalizacao(String json) {
        if (!json.contains("\"status\":\"success\"")) {
            return Optional.empty();
        }

        String cidade = extrairCampo(json, "city");
        String estado = extrairCampo(json, "regionName");
        String pais = extrairCampo(json, "country");

        StringBuilder sb = new StringBuilder();
        if (cidade != null) sb.append(cidade);
        if (estado != null && !estado.equals(cidade)) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(estado);
        }
        if (pais != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(pais);
        }

        return sb.isEmpty() ? Optional.empty() : Optional.of(sb.toString());
    }

    private boolean isIpPrivado(String ip) {
        if (IPS_LOCALHOST.contains(ip)) return true;
        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            String[] partes = ip.split("\\.");
            if (partes.length >= 2) {
                try {
                    int segundo = Integer.parseInt(partes[1]);
                    return segundo >= 16 && segundo <= 31;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private String extrairCampo(String json, String campo) {
        String chave = "\"" + campo + "\":\"";
        int inicio = json.indexOf(chave);
        if (inicio == -1) return null;
        inicio += chave.length();
        int fim = json.indexOf("\"", inicio);
        if (fim == -1) return null;
        String valor = json.substring(inicio, fim);
        return valor.isBlank() ? null : valor;
    }
}
