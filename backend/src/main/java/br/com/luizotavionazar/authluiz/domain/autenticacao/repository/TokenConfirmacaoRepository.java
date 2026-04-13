package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TipoTokenConfirmacao;
import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenConfirmacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenConfirmacaoRepository extends JpaRepository<TokenConfirmacao, Long> {

    Optional<TokenConfirmacao> findByTokenHash(String tokenHash);

    Optional<TokenConfirmacao> findFirstByUsuarioIdAndTipoAndConfirmadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(
            Integer idUsuario, TipoTokenConfirmacao tipo
    );

    @Query("""
            SELECT u.id FROM Usuario u
            WHERE u.emailVerificado = false
            AND NOT EXISTS (
                SELECT t FROM TokenConfirmacao t
                WHERE t.usuario = u
                AND t.tipo = :tipo
                AND t.confirmadoEm IS NULL
                AND t.encerradoEm IS NULL
                AND t.expiraEm > :agora
            )
            """)
    List<Integer> findIdUsuariosNaoVerificadosSemTokenAtivo(
            @Param("tipo") TipoTokenConfirmacao tipo,
            @Param("agora") LocalDateTime agora
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE TokenConfirmacao t
            SET t.encerradoEm = :agora
            WHERE t.usuario.id = :idUsuario
            AND t.tipo = :tipo
            AND t.confirmadoEm IS NULL
            AND t.encerradoEm IS NULL
            """)
    int encerrarTokensAbertos(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipo") TipoTokenConfirmacao tipo,
            @Param("agora") LocalDateTime agora
    );
}
