package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.ControleEnvioCodigoIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ControleEnvioCodigoIpRepository extends JpaRepository<ControleEnvioCodigoIp, Long> {

    Optional<ControleEnvioCodigoIp> findByIp(String ip);
}
