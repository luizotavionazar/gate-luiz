package br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "identidadeExterna",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_identidade_externa_provider_usuario", columnNames = {"provider", "providerUserId"}),
                @UniqueConstraint(name = "uk_identidade_externa_usuario_provider", columnNames = {"idUsuario", "provider"})
        }
)
public class IdentidadeExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private ProviderExterno provider;

    @Column(name = "providerUserId", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "emailProvider", length = 255)
    private String emailProvider;

    @Column(name = "emailVerificadoProvider", nullable = false)
    private Boolean emailVerificadoProvider;

    @CreationTimestamp
    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "dataAtualiza")
    private LocalDateTime dataAtualiza;
}
