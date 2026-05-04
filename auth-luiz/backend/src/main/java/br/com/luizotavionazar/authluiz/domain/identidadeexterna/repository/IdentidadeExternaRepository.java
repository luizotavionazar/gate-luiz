package br.com.luizotavionazar.authluiz.domain.identidadeexterna.repository;

import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.IdentidadeExterna;
import br.com.luizotavionazar.authluiz.domain.identidadeexterna.entity.ProviderExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface IdentidadeExternaRepository extends JpaRepository<IdentidadeExterna, Long> {

    Optional<IdentidadeExterna> findByProviderAndProviderUserId(ProviderExterno provider, String providerUserId);

    boolean existsByUsuarioIdAndProvider(Integer idUsuario, ProviderExterno provider);

    void deleteByUsuarioIdAndProvider(Integer idUsuario, ProviderExterno provider);

    @Query("SELECT ie.usuario.id FROM IdentidadeExterna ie WHERE ie.provider = :provider")
    Set<Integer> findUsuarioIdsByProvider(@Param("provider") ProviderExterno provider);
}
