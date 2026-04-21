package br.com.luizotavionazar.permluiz.domain.permissao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "permissao")
@Getter
@Setter
@NoArgsConstructor
public class Permissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String recurso;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void prePersist() {
        dataCriacao = LocalDateTime.now();
    }
}
