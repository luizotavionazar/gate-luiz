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
@Table(name = "loginPendente")
public class LoginPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tokenPendente", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenPendente;

    @Column(name = "idUsuario", nullable = false, updatable = false)
    private Integer idUsuario;

    @Column(name = "tipo", nullable = false, updatable = false, length = 20)
    private String tipo;

    @Column(name = "codigo", length = 6)
    private String codigo;

    @Column(name = "ipOrigem", nullable = false, updatable = false, length = 45)
    private String ipOrigem;

    @Column(name = "expiraEm", nullable = false, updatable = false)
    private LocalDateTime expiraEm;

    @Builder.Default
    @Column(name = "tentativasErradas", nullable = false)
    private int tentativasErradas = 0;

    @Column(name = "confirmadoEm")
    private LocalDateTime confirmadoEm;

    @Column(name = "encerradoEm")
    private LocalDateTime encerradoEm;

    @CreationTimestamp
    @Column(name = "criadoEm", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public boolean estaAtivo() {
        return confirmadoEm == null
                && encerradoEm == null
                && LocalDateTime.now().isBefore(expiraEm)
                && tentativasErradas < 5;
    }
}
