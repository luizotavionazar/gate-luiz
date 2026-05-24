package br.com.luizotavionazar.authluiz.domain.autenticacao.service;

import br.com.luizotavionazar.authluiz.domain.autenticacao.entity.CodigoBackup2fa;
import br.com.luizotavionazar.authluiz.domain.autenticacao.repository.CodigoBackup2faRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CodigoBackupService {

    private static final int QUANTIDADE_CODIGOS = 8;
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CodigoBackup2faRepository repo;
    private final PasswordEncoder passwordEncoder;

    public List<String> gerarParaUsuario(Integer idUsuario) {
        repo.deleteByIdUsuario(idUsuario);

        List<String> codigos = new ArrayList<>();
        List<CodigoBackup2fa> entidades = new ArrayList<>();
        SecureRandom rng = new SecureRandom();

        for (int i = 0; i < QUANTIDADE_CODIGOS; i++) {
            String codigo = gerarCodigo(rng);
            codigos.add(codigo);
            entidades.add(CodigoBackup2fa.builder()
                    .idUsuario(idUsuario)
                    .codigoHash(passwordEncoder.encode(codigo))
                    .build());
        }

        repo.saveAll(entidades);
        return codigos;
    }

    public boolean usar(Integer idUsuario, String codigoInformado) {
        for (CodigoBackup2fa entry : repo.findByIdUsuarioAndUsadoEmIsNull(idUsuario)) {
            if (passwordEncoder.matches(codigoInformado, entry.getCodigoHash())) {
                entry.setUsadoEm(LocalDateTime.now());
                repo.save(entry);
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public int contar(Integer idUsuario) {
        return repo.countByIdUsuarioAndUsadoEmIsNull(idUsuario);
    }

    public void deletarParaUsuario(Integer idUsuario) {
        repo.deleteByIdUsuario(idUsuario);
    }

    private String gerarCodigo(SecureRandom rng) {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARSET.charAt(rng.nextInt(CHARSET.length())));
        }
        return sb.substring(0, 4) + "-" + sb.substring(4);
    }
}
