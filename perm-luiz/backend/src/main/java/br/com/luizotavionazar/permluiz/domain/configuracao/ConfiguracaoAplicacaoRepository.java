package br.com.luizotavionazar.permluiz.domain.configuracao;

import br.com.luizotavionazar.permluiz.domain.configuracao.entity.ConfiguracaoAplicacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoAplicacaoRepository extends JpaRepository<ConfiguracaoAplicacao, Long> {
}
