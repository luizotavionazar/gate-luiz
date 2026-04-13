package br.com.luizotavionazar.authluiz.domain.configuracao.service;

import br.com.luizotavionazar.authluiz.api.setup.dto.ConfiguracaoEmailPublicaResponse;
import br.com.luizotavionazar.authluiz.api.setup.dto.SalvarSetupRequest;
import br.com.luizotavionazar.authluiz.api.setup.dto.StatusSetupResponse;
import br.com.luizotavionazar.authluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import br.com.luizotavionazar.authluiz.domain.configuracao.repository.ConfiguracaoAplicacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final ConfiguracaoAplicacaoRepository repository;
    private final CriptografiaConfiguracaoService criptografiaService;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${app.setup.master-key:}")
    private String masterKey;

    @Transactional(readOnly = true)
    public StatusSetupResponse status() {
        ConfiguracaoAplicacao config = obter();

        boolean bootstrapOk =
                !isBlank(datasourceUrl) &&
                !isBlank(datasourceUsername) &&
                !isBlank(jwtSecret) &&
                !isBlank(masterKey);

        return new StatusSetupResponse(bootstrapOk, config.isSetupConcluido());
    }

    @Transactional(readOnly = true)
    public ConfiguracaoEmailPublicaResponse obterConfiguracaoPublica() {
        ConfiguracaoAplicacao config = obter();

        return new ConfiguracaoEmailPublicaResponse(
                config.getSmtpHost(),
                config.getSmtpPort(),
                config.getSmtpUsername(),
                config.getMailFrom(),
                config.getFrontendBaseUrl(),
                config.isSmtpAuth(),
                config.isSmtpStarttls(),
                config.isSetupConcluido(),
                config.isConfirmacaoEmailHabilitada()
        );
    }

    @Transactional
    public void salvar(SalvarSetupRequest request) {
        StatusSetupResponse status = status();
        if (!status.bootstrapOk()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bootstrap incompleto. Configure o arquivo .env antes de concluir o setup!"
            );
        }

        ConfiguracaoAplicacao config = obter();

        config.setSmtpHost(request.smtpHost().trim());
        config.setSmtpPort(request.smtpPort());
        config.setSmtpUsername(request.smtpUsername().trim());
        config.setSmtpPasswordCriptografada(
                criptografiaService.criptografar(request.smtpPassword())
        );
        config.setMailFrom(request.mailFrom().trim());
        config.setFrontendBaseUrl(request.frontendBaseUrl().trim());
        config.setSmtpAuth(request.smtpAuth());
        config.setSmtpStarttls(request.smtpStarttls());
        config.setConfirmacaoEmailHabilitada(request.confirmacaoEmailHabilitada());
        config.setSetupConcluido(true);

        repository.save(config);
    }

    @Transactional(readOnly = true)
    public ConfiguracaoAplicacao obter() {
        return repository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Configuração da API não encontrada!"
                ));
    }

    @Transactional(readOnly = true)
    public String obterSmtpPasswordDescriptografada() {
        return criptografiaService.descriptografar(obter().getSmtpPasswordCriptografada());
    }

    public boolean setupConcluido() {
        return obter().isSetupConcluido();
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}