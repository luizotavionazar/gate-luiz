package br.com.luizotavionazar.permluiz.domain.permissao;

import br.com.luizotavionazar.permluiz.domain.permissao.entity.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

    boolean existsByRecursoAndAcao(String recurso, String acao);

    List<Permissao> findAllByIdIn(List<Long> ids);
}
