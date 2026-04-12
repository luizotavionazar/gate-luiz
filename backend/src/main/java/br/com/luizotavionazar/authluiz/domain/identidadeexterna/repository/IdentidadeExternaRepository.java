package br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository;

import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.IdentidadeExterna;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentidadeExternaRepository extends JpaRepository<IdentidadeExterna, Long> {

    Optional<IdentidadeExterna> findByProviderAndProviderUserId(ProviderExterno provider, String providerUserId);

    boolean existsByUsuarioIdAndProvider(Integer idUsuario, ProviderExterno provider);
}
