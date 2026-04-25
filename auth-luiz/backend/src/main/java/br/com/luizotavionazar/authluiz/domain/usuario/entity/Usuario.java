package br.com.luizotavionazar.authluiz.domain.usuario.entity;

import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @ToString.Exclude
    @Column(name = "senhaHash", length = 255)
    private String senhaHash;

    @CreationTimestamp
    @Column(name = "dataCriacao", updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "dataAtualiza")
    private LocalDateTime dataAtualiza;

    @Builder.Default
    @Column(name = "emailVerificado", nullable = false)
    private boolean emailVerificado = true;

    @Column(name = "emailPendente", length = 255)
    private String emailPendente;

    @Enumerated(EnumType.STRING)
    @Column(name = "providerOrigem", length = 50)
    private ProviderExterno providerOrigem;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Builder.Default
    @Column(name = "telefoneVerificado", nullable = false)
    private boolean telefoneVerificado = false;

    public boolean possuiSenha() {
        return senhaHash != null && !senhaHash.isBlank();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
