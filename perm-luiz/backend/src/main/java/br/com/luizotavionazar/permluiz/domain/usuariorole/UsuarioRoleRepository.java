package br.com.luizotavionazar.permluiz.domain.usuariorole;

import br.com.luizotavionazar.permluiz.domain.usuariorole.entity.UsuarioRole;
import br.com.luizotavionazar.permluiz.domain.usuariorole.entity.UsuarioRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UsuarioRoleRepository extends JpaRepository<UsuarioRole, UsuarioRoleId> {

    @Query("SELECT ur FROM UsuarioRole ur JOIN FETCH ur.role r LEFT JOIN FETCH r.permissoes WHERE ur.idUsuario = :idUsuario")
    List<UsuarioRole> findByIdUsuarioWithRolesAndPermissoes(Long idUsuario);

    List<UsuarioRole> findByIdUsuario(Long idUsuario);

    void deleteByIdUsuarioAndIdRole(Long idUsuario, Long idRole);
}
