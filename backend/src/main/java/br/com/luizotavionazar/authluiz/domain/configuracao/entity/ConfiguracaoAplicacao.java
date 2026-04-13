package br.com.luizotavionazar.authluiz.domain.configuracao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracaoAplicacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoAplicacao {

    @Id
    private Long id;

    @Column(name = "setupConcluido", nullable = false)
    private boolean setupConcluido;

    @Column(name = "smtpHost")
    private String smtpHost;

    @Column(name = "smtpPort")
    private Integer smtpPort;

    @Column(name = "smtpUsername")
    private String smtpUsername;

    @Column(name = "smtpPasswordCriptografada")
    private String smtpPasswordCriptografada;

    @Column(name = "smtpAuth", nullable = false)
    @Builder.Default
    private boolean smtpAuth = true;

    @Column(name = "smtpStarttls", nullable = false)
    @Builder.Default
    private boolean smtpStarttls = true;

    @Column(name = "mailFrom")
    private String mailFrom;

    @Column(name = "frontendBaseUrl")
    private String frontendBaseUrl;

    @Column(name = "confirmacaoEmailHabilitada", nullable = false)
    private boolean confirmacaoEmailHabilitada;

    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "dataAtualiza", nullable = false)
    private LocalDateTime dataAtualiza;

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        this.dataCriacao = agora;
        this.dataAtualiza = agora;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAtualiza = LocalDateTime.now();
    }
}