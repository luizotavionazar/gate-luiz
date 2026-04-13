package br.com.luizotavionazar.authluiz.domain.notificacao.service;

import br.com.luizotavionazar.authluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final SetupService setupService;

    public void enviarBoasVindas(String nome, String email) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(config.getMailFrom());
        mensagem.setTo(email);
        mensagem.setSubject("Bem-vindo ao AuthLuiz!");

        String corpo = """
                Olá, %s!

                Sua conta no AuthLuiz foi criada com sucesso.

                Você já pode acessar sua conta em:
                %s/login

                Seja bem-vindo!
                """.formatted(nome, config.getFrontendBaseUrl());

        mensagem.setText(corpo);
        criarMailSender(config).send(mensagem);
    }

    public void enviarVerificacaoCadastro(String nome, String email, String token) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(config.getMailFrom());
        mensagem.setTo(email);
        mensagem.setSubject("Verifique seu e-mail - AuthLuiz");

        String link = config.getFrontendBaseUrl() + "/verificar-email?token=" + token;

        String corpo = """
                Olá, %s! Bem-vindo ao AuthLuiz!

                Sua conta foi criada com sucesso, mas ainda precisa de uma etapa final: a verificação do seu e-mail.

                Clique no link abaixo para ativar sua conta:
                %s

                Este link é válido por 7 dias. Caso você não confirme dentro deste prazo, sua conta será removida automaticamente e você precisará se cadastrar novamente.

                Se você não criou uma conta no AuthLuiz, ignore este e-mail.
                """.formatted(nome, link);

        mensagem.setText(corpo);
        criarMailSender(config).send(mensagem);
    }

    public void enviarConfirmacaoAlteracaoEmail(String nome, String novoEmail, String token) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(config.getMailFrom());
        mensagem.setTo(novoEmail);
        mensagem.setSubject("Confirme seu novo e-mail - AuthLuiz");

        String link = config.getFrontendBaseUrl() + "/verificar-email?token=" + token;

        String corpo = """
                Olá, %s!

                Recebemos uma solicitação para alterar o e-mail da sua conta no AuthLuiz para este endereço.

                Clique no link abaixo para confirmar a alteração:
                %s

                Este link expira em 30 minutos e pode ser usado apenas uma vez.

                Se você não solicitou essa alteração, ignore este e-mail. Seu e-mail atual permanecerá inalterado.
                """.formatted(nome, link);

        mensagem.setText(corpo);
        criarMailSender(config).send(mensagem);
    }

    public void enviarRecuperacaoSenha(String nome, String email, String token) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(config.getMailFrom());
        mensagem.setTo(email);
        mensagem.setSubject("Recuperação de senha - AuthLuiz");

        String link = config.getFrontendBaseUrl() + "/redefinir-senha?token=" + token;

        String corpo = """
                Olá, %s!

                Recebemos um pedido para redefinir sua senha no AuthLuiz.

                Use o link abaixo para cadastrar uma nova senha:
                %s

                Este link expira em 30 minutos e pode ser usado apenas uma vez.

                Se você não solicitou essa alteração, ignore este e-mail.
                """.formatted(nome, link);

        mensagem.setText(corpo);
        criarMailSender(config).send(mensagem);
    }

    private ConfiguracaoAplicacao validarSetupEmail() {
        ConfiguracaoAplicacao config = setupService.obter();

        if (!config.isSetupConcluido()
                || isBlank(config.getSmtpHost())
                || config.getSmtpPort() == null
                || isBlank(config.getSmtpUsername())
                || isBlank(config.getSmtpPasswordCriptografada())
                || isBlank(config.getMailFrom())
                || isBlank(config.getFrontendBaseUrl())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O envio de e-mails ainda não foi configurado.");
        }

        return config;
    }

    private JavaMailSenderImpl criarMailSender(ConfiguracaoAplicacao config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());
        mailSender.setUsername(config.getSmtpUsername());
        mailSender.setPassword(setupService.obterSmtpPasswordDescriptografada());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(config.isSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStarttls()));
        return mailSender;
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
