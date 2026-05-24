package br.com.luizotavionazar.authluiz.api.conta.dto;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.UsuarioIPConfiavel;

import java.time.LocalDateTime;

public record IpConfiavelResponse(Long id, String ip, String rotulo, LocalDateTime criadoEm) {
    public static IpConfiavelResponse from(UsuarioIPConfiavel e) {
        return new IpConfiavelResponse(e.getId(), e.getIp(), e.getRotulo(), e.getCriadoEm());
    }
}
