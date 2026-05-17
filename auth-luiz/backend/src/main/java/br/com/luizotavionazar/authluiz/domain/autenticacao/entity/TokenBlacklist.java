package br.com.luizotavionazar.authluiz.domain.autenticacao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "tokenBlacklist")
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, updatable = false, length = 36)
    private String jti;

    @Column(name = "expiraEm", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = Instant.now();
    }
}
