package br.com.safralink.web;

import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.UsuarioService;
import br.com.safralink.web.dto.UsuarioDtos.PerfilRequest;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public UsuarioResponse meuPerfil(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.meuPerfil(usuario.getId());
    }

    @PutMapping
    public UsuarioResponse atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                     @Valid @RequestBody PerfilRequest req) {
        return service.atualizar(usuario.getId(), req);
    }
}
