package br.com.luizotavionazar.authluiz.domain.autenticacao.entity;

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
@Table(name = "controleEnvioCodigoIp")
public class ControleEnvioCodigoIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "ip", nullable = false, length = 45, unique = true)
    private String ip;

    @Column(name = "janelaInicio", nullable = false)
    private LocalDateTime janelaInicio;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "bloqueadoAte")
    private LocalDateTime bloqueadoAte;

    @CreationTimestamp
    @Column(name = "dataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "dataAtualiza", nullable = false)
    private LocalDateTime dataAtualiza;

    public boolean bloqueadoAgora() {
        return bloqueadoAte != null && LocalDateTime.now().isBefore(bloqueadoAte);
    }
}
