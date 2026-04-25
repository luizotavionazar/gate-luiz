package br.com.luizotavionazar.authluiz.domain.usuario.repository;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsByTelefone(String telefone);

    boolean existsByTelefoneAndIdNot(String telefone, Integer id);

    // Atualiza apenas a coluna emailPendente via JPQL direto para evitar que o
    // @UpdateTimestamp dispare — dataAtualiza só deve ser atualizada quando o
    // e-mail for de fato confirmado e trocado, não ao registrar a pendência.
    // clearAutomatically=true esvazia o contexto de persistência após o JPQL,
    // garantindo que a entidade usuario fique desanexada e não seja re-flushed
    // pelo Hibernate no commit da transação.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.emailPendente = :emailPendente WHERE u.id = :id")
    void atualizarEmailPendente(@Param("id") Integer id, @Param("emailPendente") String emailPendente);
}
