package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.configuracao.service.CriptografiaConfiguracaoService;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TotpService {

    private final CriptografiaConfiguracaoService criptografiaService;

    public String gerarSegredo() {
        return new DefaultSecretGenerator(20).generate();
    }

    public String gerarOtpauthUri(String segredo, String email, String emissor) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(segredo)
                .issuer(emissor)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    public boolean validarCodigo(String segredoCriptografado, String codigo) {
        String segredo = criptografiaService.descriptografar(segredoCriptografado);
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(segredo, codigo);
    }

    public String criptografar(String segredo) {
        return criptografiaService.criptografar(segredo);
    }

    public String descriptografar(String segredoCriptografado) {
        return criptografiaService.descriptografar(segredoCriptografado);
    }
}
