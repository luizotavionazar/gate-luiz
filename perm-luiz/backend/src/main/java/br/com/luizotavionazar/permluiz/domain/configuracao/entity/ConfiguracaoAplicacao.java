package br.com.luizotavionazar.permluiz.domain.configuracao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracaoAplicacao")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoAplicacao {

    @Id
    private Long id;

    @Column(nullable = false)
    private Boolean setupConcluido;

    private Long idAdminMestre;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private LocalDateTime dataAtualiza;
}
