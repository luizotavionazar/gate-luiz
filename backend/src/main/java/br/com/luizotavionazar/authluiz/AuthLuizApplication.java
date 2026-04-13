package br.com.luizotavionazar.authluiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthLuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthLuizApplication.class, args);
    }
}
