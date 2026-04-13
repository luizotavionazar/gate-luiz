package br.com.luizotavionazar.authluiz.domain.autenticacao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "controleAlteracaoEmail")
public class ControleAlteracaoEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "idUsuario", nullable = false, unique = true)
    private Integer idUsuario;

    @Column(name = "janelaInicio", nullable = false)
    private LocalDateTime janelaInicio;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "bloqueadoAte")
    private LocalDateTime bloqueadoAte;

    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "dataAtualiza")
    private LocalDateTime dataAtualiza;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAtualiza = LocalDateTime.now();
    }
}
