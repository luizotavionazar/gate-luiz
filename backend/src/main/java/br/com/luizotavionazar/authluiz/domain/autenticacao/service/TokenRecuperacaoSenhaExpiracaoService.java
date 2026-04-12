package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.TokenRecuperacaoSenha;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.TokenRecuperacaoSenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenRecuperacaoSenhaExpiracaoService {

    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void encerrarSeExpirado(TokenRecuperacaoSenha token) {
        if (token.expirado() && !token.usado() && !token.encerrado()) {
            token.setEncerradoEm(LocalDateTime.now());
            tokenRecuperacaoSenhaRepository.saveAndFlush(token);
        }
    }
}
