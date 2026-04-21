package br.com.luizotavionazar.permluiz.domain.role.entity;

import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private LocalDateTime dataAtualiza;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rolePermissao",
            joinColumns = @JoinColumn(name = "idRole"),
            inverseJoinColumns = @JoinColumn(name = "idPermissao")
    )
    private List<Permissao> permissoes = new ArrayList<>();

    @PrePersist
    void prePersist() {
        dataCriacao = LocalDateTime.now();
    }
}
