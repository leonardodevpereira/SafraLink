package br.com.safralink.web;

import br.com.safralink.service.AuthService;
import br.com.safralink.web.dto.AuthDtos.EsqueciSenhaRequest;
import br.com.safralink.web.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.safralink.web.dto.AuthDtos.RegistroRequest;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest req) {
        return service.registrar(req);
    }

    @PostMapping("/esqueci-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest req) {
        service.solicitarRecuperacao(req.email());
    }

    @PostMapping("/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        service.redefinirSenha(req);
    }
}
