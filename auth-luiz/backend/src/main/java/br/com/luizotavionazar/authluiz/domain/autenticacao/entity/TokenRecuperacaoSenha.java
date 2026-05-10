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
@Table(name = "tokenRecuperacaoSenha")
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Column(name = "tokenHash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expiraEm", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usadoEm")
    private LocalDateTime usadoEm;

    @Column(name = "ipSolicitacao", length = 45)
    private String ipSolicitacao;

    @Column(name = "encerradoEm")
    private LocalDateTime encerradoEm;

    @Builder.Default
    @Column(name = "tentativasErradas", nullable = false)
    private int tentativasErradas = 0;

    @CreationTimestamp
    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public boolean expirado() {
        return LocalDateTime.now().isAfter(expiraEm);
    }

    public boolean usado() {
        return usadoEm != null;
    }

    public boolean encerrado() {
        return encerradoEm != null;
    }
}