package br.com.luizotavionazar.authluiz.domain.autenticacao.entity;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tokenConfirmacao")
public class TokenConfirmacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoTokenConfirmacao tipo;

    @Column(name = "tokenHash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "emailDestino", length = 255)
    private String emailDestino;

    @Column(name = "expiraEm", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "confirmadoEm")
    private LocalDateTime confirmadoEm;

    @Column(name = "encerradoEm")
    private LocalDateTime encerradoEm;

    @Column(name = "ipSolicitacao", length = 45)
    private String ipSolicitacao;

    @CreationTimestamp
    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public boolean expirado() {
        return LocalDateTime.now().isAfter(expiraEm);
    }

    public boolean confirmado() {
        return confirmadoEm != null;
    }

    public boolean encerrado() {
        return encerradoEm != null;
    }
}
