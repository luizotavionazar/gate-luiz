package br.com.luizotavionazar.permluiz.domain.usuariorole.entity;

import java.io.Serializable;

public record UsuarioRoleId(Long idUsuario, Long idRole) implements Serializable {
}
