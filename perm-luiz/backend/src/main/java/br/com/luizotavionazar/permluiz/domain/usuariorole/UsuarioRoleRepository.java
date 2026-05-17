package br.com.luizotavionazar.permluiz.domain.usuariorole;

import br.com.luizotavionazar.permluiz.domain.usuariorole.entity.UsuarioRole;
import br.com.luizotavionazar.permluiz.domain.usuariorole.entity.UsuarioRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UsuarioRoleRepository extends JpaRepository<UsuarioRole, UsuarioRoleId> {

    @Query("SELECT ur FROM UsuarioRole ur JOIN FETCH ur.role r LEFT JOIN FETCH r.permissoes WHERE ur.idUsuario = :idUsuario")
    List<UsuarioRole> findByIdUsuarioWithRolesAndPermissoes(String idUsuario);

    @Query("SELECT ur FROM UsuarioRole ur JOIN FETCH ur.role")
    List<UsuarioRole> findAllWithRoles();

    List<UsuarioRole> findByIdUsuario(String idUsuario);

    boolean existsByIdRole(Long idRole);

    @Transactional
    void deleteByIdUsuarioAndIdRole(String idUsuario, Long idRole);
}
