package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.CodigoBackup2fa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CodigoBackup2faRepository extends JpaRepository<CodigoBackup2fa, Long> {

    List<CodigoBackup2fa> findByIdUsuarioAndUsadoEmIsNull(Integer idUsuario);

    int countByIdUsuarioAndUsadoEmIsNull(Integer idUsuario);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CodigoBackup2fa c WHERE c.idUsuario = :idUsuario")
    void deleteByIdUsuario(@Param("idUsuario") Integer idUsuario);
}
