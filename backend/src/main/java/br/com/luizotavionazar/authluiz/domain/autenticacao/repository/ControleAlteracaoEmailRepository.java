package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.ControleAlteracaoEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ControleAlteracaoEmailRepository extends JpaRepository<ControleAlteracaoEmail, Long> {

    Optional<ControleAlteracaoEmail> findByIdUsuario(Integer idUsuario);
}
