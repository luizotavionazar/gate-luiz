package br.com.luizotavionazar.authluiz.domain.notificacao.service;

import br.com.luizotavionazar.authluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import br.com.luizotavionazar.authluiz.domain.configuracao.service.SetupService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final SetupService setupService;
    private final IpGeolocalizacaoService ipGeolocalizacaoService;

    @Async
    public void enviarBoasVindas(String nome, String email) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 0;">Sua conta foi criada com sucesso. Quando quiser, acesse <strong>Minha conta</strong> para confirmar seu e-mail e liberar todos os recursos da plataforma.</p>
                """.formatted(nome);

        String html = construirHtml(
                "Bem-vindo ao AuthLuiz!",
                "Sua conta está pronta",
                corpo,
                null,
                null
        );

        enviar(config, email, "Bem-vindo ao AuthLuiz!", html);
    }

    @Async
    public void enviarVerificacaoCadastro(String nome, String email, String codigo) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 24px;">Insira o código abaixo no AuthLuiz para confirmar seu e-mail.</p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#f0f4ff;border:2px dashed #0d6efd;border-radius:12px;padding:16px 40px;">
                    <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#0d6efd;">%s</span>
                  </div>
                  <p style="margin:12px 0 0;color:#6c757d;font-size:13px;">Este código expira em <strong>5 minutos</strong> e pode ser usado apenas uma vez.</p>
                </div>
                <p style="margin:24px 0 0;color:#6c757d;font-size:13px;">Caso não tenha solicitado isso, ignore este e-mail.</p>
                """.formatted(nome, codigo);

        String html = construirHtml(
                "Ative sua conta",
                "Só mais um passo para começar",
                corpo,
                null,
                null
        );

        enviar(config, email, "Código de ativação - AuthLuiz", html);
    }

    @Async
    public void enviarConfirmacaoAlteracaoEmail(String nome, String novoEmail, String codigo) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 24px;">Recebemos uma solicitação para alterar o e-mail da sua conta para este endereço. Insira o código abaixo no AuthLuiz para confirmar a alteração.</p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#f0f4ff;border:2px dashed #0d6efd;border-radius:12px;padding:16px 40px;">
                    <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#0d6efd;">%s</span>
                  </div>
                  <p style="margin:12px 0 0;color:#6c757d;font-size:13px;">Este código expira em <strong>5 minutos</strong> e pode ser usado apenas uma vez.</p>
                </div>
                <p style="margin:24px 0 0;color:#6c757d;font-size:13px;">Se você não solicitou essa alteração, ignore este e-mail — seu e-mail atual permanecerá inalterado.</p>
                """.formatted(nome, codigo);

        String html = construirHtml(
                "Confirme seu novo e-mail",
                "Solicitação de alteração de e-mail",
                corpo,
                null,
                null
        );

        enviar(config, novoEmail, "Código de confirmação de e-mail - AuthLuiz", html);
    }

    @Async
    public void enviarRecuperacaoSenha(String nome, String email, String codigo) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 24px;">Recebemos um pedido para redefinir a senha da sua conta. Insira o código abaixo no AuthLuiz para criar uma nova senha.</p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#f0f4ff;border:2px dashed #0d6efd;border-radius:12px;padding:16px 40px;">
                    <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#0d6efd;">%s</span>
                  </div>
                  <p style="margin:12px 0 0;color:#6c757d;font-size:13px;">Este código expira em <strong>5 minutos</strong> e pode ser usado apenas uma vez.</p>
                </div>
                <p style="margin:24px 0 0;color:#6c757d;font-size:13px;">Se você não solicitou essa alteração, ignore este e-mail.</p>
                """.formatted(nome, codigo);

        String html = construirHtml(
                "Redefinição de senha",
                "Recebemos sua solicitação",
                corpo,
                null,
                null
        );

        enviar(config, email, "Código de recuperação de senha - AuthLuiz", html);
    }

    @Async
    public void enviarAvisoRecuperacaoViaTelefone(String nome, String email, String ip,
                                                   LocalDateTime dataHora, String telefone) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String dataFormatada = dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"));
        String linhaLocalizacao = construirLinhaLocalizacao(ip);

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 20px;">Detectamos uma solicitação de recuperação de senha para a sua conta via WhatsApp/SMS (<strong>%s</strong>). Confira os detalhes abaixo:</p>
                <div style="background:#fff8e1;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin:0 0 20px;">
                  <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;width:130px;">Endereço IP</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;font-family:monospace;">%s</td>
                    </tr>
                    %s
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;">Data e horário</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="margin:0;color:#6c757d;font-size:13px;">Se não foi você, recomendamos que acesse sua conta imediatamente e altere suas credenciais.</p>
                """.formatted(nome, telefone, ip, linhaLocalizacao, dataFormatada);

        String html = construirHtml(
                "Alerta de recuperação de senha",
                "Solicitação via WhatsApp/SMS",
                corpo,
                null,
                null
        );

        enviar(config, email, "Alerta: recuperação de senha solicitada - AuthLuiz", html);
    }

    @Async
    public void enviarNotificacaoVinculacaoGoogle(String nome, String email, String ip, LocalDateTime dataHora) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String dataFormatada = dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"));
        String linhaLocalizacao = construirLinhaLocalizacao(ip);

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 20px;">Uma conta Google foi vinculada à sua conta com sucesso. Confira abaixo os detalhes da ação:</p>
                <div style="background:#fff8e1;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin:0 0 20px;">
                  <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;width:130px;">Endereço IP</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;font-family:monospace;">%s</td>
                    </tr>
                    %s
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;">Data e horário</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="margin:0;color:#6c757d;font-size:13px;">Se você não realizou esta ação, recomendamos que acesse sua conta imediatamente e altere suas credenciais.</p>
                """.formatted(nome, ip, linhaLocalizacao, dataFormatada);

        String html = construirHtml(
                "Conta Google vinculada",
                "Vinculação com Google realizada",
                corpo,
                null,
                null
        );

        enviar(config, email, "Conta Google vinculada - AuthLuiz", html);
    }

    @Async
    public void enviarNotificacaoAlteracaoTelefone(String nome, String email, String novoTelefone,
                                                    String ip, LocalDateTime dataHora) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String dataFormatada = dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"));
        String linhaLocalizacao = construirLinhaLocalizacao(ip);

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 20px;">O número de telefone da sua conta foi alterado para <strong>%s</strong> com sucesso. Confira abaixo os detalhes da ação:</p>
                <div style="background:#fff8e1;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin:0 0 20px;">
                  <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;width:130px;">Endereço IP</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;font-family:monospace;">%s</td>
                    </tr>
                    %s
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;">Data e horário</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="margin:0;color:#6c757d;font-size:13px;">Se você não realizou esta ação, recomendamos que acesse sua conta imediatamente e altere suas credenciais.</p>
                """.formatted(nome, novoTelefone, ip, linhaLocalizacao, dataFormatada);

        String html = construirHtml(
                "Telefone alterado",
                "Seu número de telefone foi atualizado",
                corpo,
                null,
                null
        );

        enviar(config, email, "Seu telefone foi alterado - AuthLuiz", html);
    }

    @Async
    public void enviarNotificacaoAlteracaoSenha(String nome, String email, String ip, LocalDateTime dataHora) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String dataFormatada = dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"));
        String linhaLocalizacao = construirLinhaLocalizacao(ip);

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 20px;">Sua senha foi alterada com sucesso. Confira abaixo os detalhes da ação:</p>
                <div style="background:#fff8e1;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin:0 0 20px;">
                  <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;width:130px;">Endereço IP</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;font-family:monospace;">%s</td>
                    </tr>
                    %s
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;">Data e horário</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="margin:0;color:#6c757d;font-size:13px;">Se você não realizou esta ação, recomendamos que acesse sua conta imediatamente e utilize a recuperação de senha.</p>
                """.formatted(nome, ip, linhaLocalizacao, dataFormatada);

        String html = construirHtml(
                "Senha alterada",
                "Sua senha foi alterada",
                corpo,
                null,
                null
        );

        enviar(config, email, "Sua senha foi alterada - AuthLuiz", html);
    }

    @Async
    public void enviarVerificacaoLogin(br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario usuario, String codigo, int expiracaoMinutos) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 24px;">Detectamos um acesso à sua conta a partir de um novo dispositivo ou localização. Insira o código abaixo para confirmar que é você.</p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#f0f4ff;border:2px dashed #0d6efd;border-radius:12px;padding:16px 40px;">
                    <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#0d6efd;">%s</span>
                  </div>
                  <p style="margin:12px 0 0;color:#6c757d;font-size:13px;">Este código expira em <strong>%d minutos</strong> e pode ser usado apenas uma vez.</p>
                </div>
                <p style="margin:24px 0 0;color:#6c757d;font-size:13px;">Se não foi você, altere sua senha imediatamente.</p>
                """.formatted(usuario.getNome(), codigo, expiracaoMinutos);

        String html = construirHtml(
                "Código de verificação de acesso",
                "Confirme que é você",
                corpo,
                null,
                null
        );

        enviar(config, usuario.getEmail(), "Código de verificação de acesso - AuthLuiz", html);
    }

    @Async
    public void enviarNotificacaoRedefinicaoSenha(String nome, String email, String ip, LocalDateTime dataHora) {
        ConfiguracaoAplicacao config = validarSetupEmail();

        String dataFormatada = dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"));
        String linhaLocalizacao = construirLinhaLocalizacao(ip);

        String corpo = """
                <p style="margin:0 0 12px;">Olá, <strong>%s</strong>!</p>
                <p style="margin:0 0 20px;">Sua senha foi redefinida com sucesso. Confira abaixo os detalhes da ação:</p>
                <div style="background:#fff8e1;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin:0 0 20px;">
                  <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;width:130px;">Endereço IP</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;font-family:monospace;">%s</td>
                    </tr>
                    %s
                    <tr>
                      <td style="padding:5px 0;color:#6c757d;font-size:13px;">Data e horário</td>
                      <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="margin:0;color:#6c757d;font-size:13px;">Se você não realizou esta ação, recomendamos que acesse sua conta imediatamente e altere sua senha.</p>
                """.formatted(nome, ip, linhaLocalizacao, dataFormatada);

        String html = construirHtml(
                "Senha redefinida",
                "Sua senha foi alterada",
                corpo,
                null,
                null
        );

        enviar(config, email, "Sua senha foi redefinida - AuthLuiz", html);
    }

    private String construirLinhaLocalizacao(String ip) {
        return ipGeolocalizacaoService.obterLocalizacao(ip)
                .map(loc -> """
                        <tr>
                          <td style="padding:5px 0;color:#6c757d;font-size:13px;width:130px;">Localização aproximada</td>
                          <td style="padding:5px 0;font-weight:700;font-size:14px;color:#111827;">%s</td>
                        </tr>
                        """.formatted(loc))
                .orElse("");
    }

    private String construirHtml(String titulo, String subtitulo, String corpoHtml,
                                  String textoBtn, String linkBtn) {
        String botao = (textoBtn != null && linkBtn != null)
                ? """
                  <div style="text-align:center;margin:28px 0;">
                    <a href="%s" target="_blank"
                       style="display:inline-block;padding:13px 32px;background-color:#0d6efd;color:#ffffff;
                              text-decoration:none;border-radius:6px;font-size:15px;font-weight:600;">
                      %s
                    </a>
                  </div>
                  """.formatted(linkBtn, textoBtn)
                : "";

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#eef4ff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eef4ff;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                          <!-- Header -->
                          <tr>
                            <td style="background-color:#0d6efd;padding:28px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;letter-spacing:-0.5px;">AuthLuiz</h1>
                            </td>
                          </tr>

                          <!-- Título e subtítulo -->
                          <tr>
                            <td style="padding:32px 40px 0;text-align:center;">
                              <h2 style="margin:0 0 8px;color:#111827;font-size:20px;font-weight:700;">%s</h2>
                              <p style="margin:0;color:#6c757d;font-size:14px;">%s</p>
                            </td>
                          </tr>

                          <!-- Linha divisória -->
                          <tr>
                            <td style="padding:24px 40px 0;">
                              <hr style="border:none;border-top:1px solid #e9ecef;margin:0;">
                            </td>
                          </tr>

                          <!-- Corpo -->
                          <tr>
                            <td style="padding:24px 40px 0;color:#374151;font-size:15px;line-height:1.6;">
                              %s
                            </td>
                          </tr>

                          <!-- Botão CTA -->
                          <tr>
                            <td style="padding:0 40px;">
                              %s
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:24px 40px 32px;">
                              <hr style="border:none;border-top:1px solid #e9ecef;margin:0 0 20px;">
                              <p style="margin:0;color:#adb5bd;font-size:12px;text-align:center;line-height:1.5;">
                                Este é um e-mail automático. Por favor, não responda.<br>
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(titulo, subtitulo, corpoHtml, botao);
    }

    private void enviar(ConfiguracaoAplicacao config, String destinatario, String assunto, String corpoHtml) {
        try {
            JavaMailSenderImpl mailSender = criarMailSender(config);
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(config.getMailFrom());
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);
            mailSender.send(mensagem);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar e-mail.", e);
        }
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
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isSmtpStarttls()));
        return mailSender;
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
