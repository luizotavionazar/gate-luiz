package br.com.luizotavionazar.permluiz.api.setup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Setup")
@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    @Operation(summary = "Status do admin mestre",
            description = """
                    Retorna `{ adminConfigurado: true/false }` indicando se já existe um admin mestre configurado.

                    **Se `adminConfigurado: false`:** nenhum admin está configurado ainda. \
                    O primeiro usuário autenticado que chamar qualquer endpoint `/admin/**` \
                    será automaticamente promovido a admin mestre. \
                    Próximo passo: autentique-se no AuthLuiz (`POST /auth/login`) e chame, por exemplo, \
                    `GET /admin/roles` para assumir o admin.

                    **Se `adminConfigurado: true`:** o sistema já possui um admin. \
                    Endpoints `/admin/**` só respondem ao usuário admin configurado.
                    """)
    @ApiResponse(responseCode = "200", description = "Status de configuração do admin")
    @GetMapping
    ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(setupService.status());
    }
}
