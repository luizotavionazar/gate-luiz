package br.com.luizotavionazar.authluiz.domain.usuario.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Component
public class UsernameValidator {

    private static final Set<String> RESERVADOS = Set.of(
            "admin", "root", "system", "support", "help", "api", "auth",
            "me", "null", "undefined", "authluiz", "login", "logout",
            "cadastro", "conta", "usuario", "user", "setup", "interno"
    );

    public void validar(String username) {
        if (RESERVADOS.contains(username.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username reservado!");
        }
    }
}
