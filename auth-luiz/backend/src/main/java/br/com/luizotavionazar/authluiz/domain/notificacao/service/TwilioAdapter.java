package br.com.luizotavionazar.authluiz.domain.notificacao.service;

import br.com.luizotavionazar.authluiz.domain.notificacao.port.NotificacaoTelefonePort;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
public class TwilioAdapter implements NotificacaoTelefonePort {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    @Value("${twilio.canal:whatsapp}")
    private String canal;

    @Override
    public void validarDisponibilidade() {
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de verificação por telefone não está disponível no momento.");
        }
    }

    @Override
    @Async
    public void enviarCodigoVerificacao(String telefone, String codigo) {
        try {
            Twilio.init(accountSid, authToken);
            Message message = Message.creator(
                    new PhoneNumber(prefixar(telefone)),
                    new PhoneNumber(prefixar(fromNumber)),
                    "Seu código de verificação AuthLuiz: " + codigo + ". Expira em 5 minutos."
            ).create();
            log.info("Mensagem Twilio enviada para {} via {} — SID: {}, status: {}", telefone, canal, message.getSid(), message.getStatus());
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem ({}) para {}: {}", canal, telefone, e.getMessage(), e);
        }
    }

    private String prefixar(String numero) {
        return "whatsapp".equalsIgnoreCase(canal) ? "whatsapp:" + numero : numero;
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
