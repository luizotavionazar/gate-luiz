package br.com.luizotavionazar.authluiz.domain.notificacao.port;

public interface NotificacaoTelefonePort {
    void validarDisponibilidade();
    void enviarCodigoVerificacao(String telefone, String codigo);
}
