package br.com.luizotavionazar.permluiz.domain.usuariorole.entity;

import br.com.luizotavionazar.permluiz.domain.role.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarioRole")
@IdClass(UsuarioRoleId.class)
@Getter
@Setter
@NoArgsConstructor
public class UsuarioRole {

    @Id
    @Column(name = "idUsuario")
    private Long idUsuario;

    @Id
    @Column(name = "idRole")
    private Long idRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idRole", insertable = false, updatable = false)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime atribuidoEm;

    @Column(nullable = false)
    private Long atribuidoPor;

    @PrePersist
    void prePersist() {
        atribuidoEm = LocalDateTime.now();
    }
}
