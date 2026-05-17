package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenBlacklist;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Transactional
    public void invalidar(Jwt jwt) {
        if (jwt.getId() == null) {
            return;
        }
        TokenBlacklist entry = TokenBlacklist.builder()
                .jti(jwt.getId())
                .expiraEm(jwt.getExpiresAt())
                .build();
        tokenBlacklistRepository.save(entry);
    }

    public boolean estaInvalidado(String jti) {
        return tokenBlacklistRepository.existsByJti(jti);
    }

    @Transactional
    public void limparExpirados() {
        tokenBlacklistRepository.deleteByExpiraEmBefore(Instant.now());
    }
}
