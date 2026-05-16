package br.com.luizotavionazar.authluiz.domain.configuracao.service;

import br.com.luizotavionazar.authluiz.api.setup.dto.ConfiguracaoEmailPublicaResponse;
import br.com.luizotavionazar.authluiz.api.setup.dto.SalvarSetupRequest;
import br.com.luizotavionazar.authluiz.api.setup.dto.StatusSetupResponse;
import br.com.luizotavionazar.authluiz.domain.auditoria.service.AuditoriaService;
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

    @Value("${jwt.rsa.private-key:}")
    private String jwtRsaPrivateKey;

    @Value("${app.setup.master-key:}")
    private String masterKey;

    @Transactional(readOnly = true)
    public StatusSetupResponse status() {
        ConfiguracaoAplicacao config = obter();

        boolean bootstrapOk =
                !isBlank(datasourceUrl) &&
                !isBlank(datasourceUsername) &&
                !isBlank(jwtRsaPrivateKey) &&
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
                config.isSmtpStarttls(),
                config.isSetupConcluido(),
                config.getTwilioFromNumber(),
                config.getTwilioCanal(),
                twilioConfigurado(config),
                config.isAuditoriaAtividade(),
                config.getAuditoriaRetencaoDias()
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
        AuditoriaService.definirDetalhes(config.isSetupConcluido() ? "Configuração atualizada" : "Configuração inicial definida");

        config.setSmtpHost(request.smtpHost().trim());
        config.setSmtpPort(request.smtpPort());
        config.setSmtpUsername(request.smtpUsername().trim());
        config.setSmtpPasswordCriptografada(
                criptografiaService.criptografar(request.smtpPassword())
        );
        config.setMailFrom(request.mailFrom().trim());
        config.setFrontendBaseUrl(request.frontendBaseUrl().trim());
        config.setSmtpStarttls(request.smtpStarttls());

        if (!isBlank(request.twilioAccountSid()) && !isBlank(request.twilioAuthToken())) {
            config.setTwilioAccountSidCriptografado(criptografiaService.criptografar(request.twilioAccountSid().trim()));
            config.setTwilioAuthTokenCriptografado(criptografiaService.criptografar(request.twilioAuthToken().trim()));
            config.setTwilioFromNumber(!isBlank(request.twilioFromNumber()) ? request.twilioFromNumber().trim() : null);
            config.setTwilioCanal(!isBlank(request.twilioCanal()) ? request.twilioCanal().trim() : "whatsapp");
        } else {
            config.setTwilioAccountSidCriptografado(null);
            config.setTwilioAuthTokenCriptografado(null);
            config.setTwilioFromNumber(null);
            config.setTwilioCanal(null);
        }

        config.setAuditoriaAtividade(request.auditoriaAtividade() != null ? request.auditoriaAtividade() : true);
        config.setAuditoriaRetencaoDias(request.auditoriaRetencaoDias() != null ? request.auditoriaRetencaoDias() : 90);

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

    @Transactional(readOnly = true)
    public boolean twilioDisponivel() {
        return twilioConfigurado(obter());
    }

    @Transactional(readOnly = true)
    public String obterTwilioAccountSid() {
        return criptografiaService.descriptografar(obter().getTwilioAccountSidCriptografado());
    }

    @Transactional(readOnly = true)
    public String obterTwilioAuthToken() {
        return criptografiaService.descriptografar(obter().getTwilioAuthTokenCriptografado());
    }

    @Transactional(readOnly = true)
    public String obterTwilioFromNumber() {
        return obter().getTwilioFromNumber();
    }

    @Transactional(readOnly = true)
    public String obterTwilioCanal() {
        String canal = obter().getTwilioCanal();
        return canal != null ? canal : "whatsapp";
    }

    @Transactional(readOnly = true)
    public boolean auditoriaAtividadeHabilitada() {
        return obter().isAuditoriaAtividade();
    }

    @Transactional(readOnly = true)
    public int auditoriaRetencaoDias() {
        return obter().getAuditoriaRetencaoDias();
    }

    private boolean twilioConfigurado(ConfiguracaoAplicacao config) {
        return !isBlank(config.getTwilioAccountSidCriptografado()) && !isBlank(config.getTwilioAuthTokenCriptografado());
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}