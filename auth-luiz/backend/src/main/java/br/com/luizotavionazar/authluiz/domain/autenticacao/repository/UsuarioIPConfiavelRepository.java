package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.UsuarioIPConfiavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioIPConfiavelRepository extends JpaRepository<UsuarioIPConfiavel, Long> {

    List<UsuarioIPConfiavel> findByIdUsuario(Integer idUsuario);

    boolean existsByIdUsuarioAndIp(Integer idUsuario, String ip);

    int countByIdUsuario(Integer idUsuario);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UsuarioIPConfiavel u WHERE u.idUsuario = :idUsuario")
    void deleteByIdUsuario(@Param("idUsuario") Integer idUsuario);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UsuarioIPConfiavel u WHERE u.id = :id AND u.idUsuario = :idUsuario")
    void deleteByIdAndIdUsuario(@Param("id") Long id, @Param("idUsuario") Integer idUsuario);
}
