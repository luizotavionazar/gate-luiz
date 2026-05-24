package br.com.luizotavionazar.authluiz.domain.autenticacao.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "codigoBackup2fa")
public class CodigoBackup2fa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "idUsuario", nullable = false, updatable = false)
    private Integer idUsuario;

    @Column(name = "codigoHash", nullable = false, updatable = false, length = 255)
    private String codigoHash;

    @Column(name = "usadoEm")
    private LocalDateTime usadoEm;

    @CreationTimestamp
    @Column(name = "criadoEm", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
