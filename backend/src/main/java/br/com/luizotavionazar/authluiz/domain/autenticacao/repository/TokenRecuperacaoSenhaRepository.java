package br.com.luizotavionazar.authluiz.domain.autenticacao.repository;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {

    Optional<TokenRecuperacaoSenha> findByTokenHash(String tokenHash);

    Optional<TokenRecuperacaoSenha> findFirstByUsuarioIdAndUsadoEmIsNullAndEncerradoEmIsNullOrderByDataCriacaoDesc(Integer idUsuario);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TokenRecuperacaoSenha t
               set t.encerradoEm = :agora
             where t.usuario.id = :idUsuario
               and t.usadoEm is null
               and t.encerradoEm is null
            """)
    int encerrarTokensAbertosDoUsuario(@Param("idUsuario") Integer idUsuario, @Param("agora") LocalDateTime agora);
}
