package br.com.luizotavionazar.permluiz.domain.role;

import br.com.luizotavionazar.permluiz.domain.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByNome(String nome);
}
