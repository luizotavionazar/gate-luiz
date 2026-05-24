package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.UsuarioIPConfiavel;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.UsuarioIPConfiavelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IpConfiavelService {

    private final UsuarioIPConfiavelRepository repo;

    @Transactional(readOnly = true)
    public boolean ehConfiavel(Integer idUsuario, String ip) {
        return repo.existsByIdUsuarioAndIp(idUsuario, ip);
    }

    public void confiarIp(Integer idUsuario, String ip, String rotulo) {
        if (!repo.existsByIdUsuarioAndIp(idUsuario, ip)) {
            repo.save(UsuarioIPConfiavel.builder()
                    .idUsuario(idUsuario)
                    .ip(ip)
                    .rotulo(rotulo)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioIPConfiavel> listar(Integer idUsuario) {
        return repo.findByIdUsuario(idUsuario);
    }

    public void remover(Long id, Integer idUsuario) {
        repo.deleteByIdAndIdUsuario(id, idUsuario);
    }

    public void removerTodos(Integer idUsuario) {
        repo.deleteByIdUsuario(idUsuario);
    }
}
