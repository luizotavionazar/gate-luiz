package br.com.luizotavionazar.authluiz.domain.notificacao.service;

import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import br.com.luizotavionazar.authluiz.domain.notificacao.port.NotificacaoTelefonePort;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class TwilioAdapter implements NotificacaoTelefonePort {

    private final SetupService setupService;

    @Override
    public void validarDisponibilidade() {
        if (!setupService.twilioDisponivel()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de verificação por telefone não está disponível no momento.");
        }
    }

    @Override
    @Async
    public void enviarCodigoVerificacao(String telefone, String codigo) {
        String accountSid = setupService.obterTwilioAccountSid();
        String authToken = setupService.obterTwilioAuthToken();
        String fromNumber = setupService.obterTwilioFromNumber();
        String canal = setupService.obterTwilioCanal();

        try {
            Twilio.init(accountSid, authToken);
            Message message = Message.creator(
                    new PhoneNumber(prefixar(telefone, canal)),
                    new PhoneNumber(prefixar(fromNumber, canal)),
                    "Seu código de verificação AuthLuiz: " + codigo + ". Expira em 5 minutos."
            ).create();
            log.info("Mensagem Twilio enviada para {} via {} — SID: {}, status: {}", telefone, canal, message.getSid(), message.getStatus());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem ({}) para {}: {}", canal, telefone, e.getMessage(), e);
        }
    }

    private String prefixar(String numero, String canal) {
        return "whatsapp".equalsIgnoreCase(canal) ? "whatsapp:" + numero : numero;
    }
}
