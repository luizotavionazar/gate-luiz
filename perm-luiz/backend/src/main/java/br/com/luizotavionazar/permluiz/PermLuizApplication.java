package br.com.luizotavionazar.permluiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PermLuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(PermLuizApplication.class, args);
    }
}
