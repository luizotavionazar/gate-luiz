package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.LoginPendente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginPendenteRepository extends JpaRepository<LoginPendente, Long> {

    Optional<LoginPendente> findByTokenPendente(String tokenPendente);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM LoginPendente lp WHERE lp.expiraEm < :momento")
    void deleteByExpiraEmBefore(@Param("momento") LocalDateTime momento);
}
