package br.com.luizotavionazar.authluiz.domain.usuario.repository;

import br.com.luizotavionazar.authluiz.domain.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByPublicId(String publicId);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTelefone(String telefone);

    Optional<Usuario> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsByTelefone(String telefone);

    boolean existsByTelefoneAndIdNot(String telefone, Integer id);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Integer id);

    // Atualiza apenas a coluna emailPendente via JPQL direto para evitar que o
    // @UpdateTimestamp dispare — dataAtualiza só deve ser atualizada quando o
    // e-mail for de fato confirmado e trocado, não ao registrar a pendência.
    // clearAutomatically=true esvazia o contexto de persistência após o JPQL,
    // garantindo que a entidade usuario fique desanexada e não seja re-flushed
    // pelo Hibernate no commit da transação.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.emailPendente = :emailPendente WHERE u.id = :id")
    void atualizarEmailPendente(@Param("id") Integer id, @Param("emailPendente") String emailPendente);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.telefonePendente = :telefonePendente WHERE u.id = :id")
    void atualizarTelefonePendente(@Param("id") Integer id, @Param("telefonePendente") String telefonePendente);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.ultimoLogin = :ultimoLogin WHERE u.id = :id")
    void atualizarUltimoLogin(@Param("id") Integer id, @Param("ultimoLogin") LocalDateTime ultimoLogin);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.ultimoIp = :ip WHERE u.id = :id")
    void atualizarUltimoIp(@Param("id") Integer id, @Param("ip") String ip);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.totpSecretPendente = :secret WHERE u.id = :id")
    void atualizarTotpSecretPendente(@Param("id") Integer id, @Param("secret") String secret);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.totpSecret = :secret, u.totpAtivo = true, u.totpSecretPendente = null, u.verificacaoExtraAtiva = true WHERE u.id = :id")
    void ativarTotp(@Param("id") Integer id, @Param("secret") String secret);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.verificacaoExtraAtiva = :ativo WHERE u.id = :id")
    void atualizarVerificacaoExtra(@Param("id") Integer id, @Param("ativo") boolean ativo);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.totpSecret = null, u.totpAtivo = false, u.totpSecretPendente = null WHERE u.id = :id")
    void desativarTotp(@Param("id") Integer id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.preferencia2fa = :preferencia WHERE u.id = :id")
    void atualizarPreferencia2fa(@Param("id") Integer id, @Param("preferencia") String preferencia);

    @Query("SELECT u.id FROM Usuario u WHERE u.emailVerificado = false AND u.dataCriacao < :limite")
    List<Integer> findIdsNaoVerificadosAntigos(@Param("limite") LocalDateTime limite);
}
