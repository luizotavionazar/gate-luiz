package br.com.luizotavionazar.authluiz.api.conta.dto;

public record DeletarContaRequest(String senha, String codigo, String tokenPendente) {
}
