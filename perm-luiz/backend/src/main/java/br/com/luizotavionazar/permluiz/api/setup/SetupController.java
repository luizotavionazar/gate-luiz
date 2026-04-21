package br.com.luizotavionazar.permluiz.api.setup;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    @GetMapping
    ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(setupService.status());
    }
}
