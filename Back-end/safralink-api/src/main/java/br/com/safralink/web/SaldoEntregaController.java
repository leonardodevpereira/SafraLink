package br.com.safralink.web;

import br.com.safralink.web.dto.CatalogoDtos.EntregaAtualizacaoRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.EntregaService;
import br.com.safralink.service.SaldoService;
import br.com.safralink.web.dto.CatalogoDtos.EntregaRequest;
import br.com.safralink.web.dto.CatalogoDtos.EntregaResponse;
import br.com.safralink.web.dto.CatalogoDtos.SaldoResponse;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SaldoEntregaController {
    private final SaldoService saldoService;
    private final EntregaService entregaService;

    public SaldoEntregaController(SaldoService saldoService, EntregaService entregaService) {
        this.saldoService = saldoService;
        this.entregaService = entregaService;
    }

    @GetMapping("/saldos")
    @PreAuthorize("hasRole('PRODUTOR')")
    public List<SaldoResponse> saldos(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return saldoService.listarDoProdutor(usuario.getId());
    }

    @GetMapping("/saldos/{produtoId}")
    @PreAuthorize("hasRole('PRODUTOR')")
    public SaldoResponse saldo(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long produtoId) {
        return saldoService.buscarDoProdutor(usuario.getId(), produtoId);
    }

    @GetMapping("/entregas")
    public PageResponse<EntregaResponse> entregas(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                  @RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamanho) {
        return entregaService.listar(usuario, pagina, tamanho);
    }

    @PostMapping("/entregas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CEREALISTA')")
    public EntregaResponse registrarEntrega(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                            @Valid @RequestBody EntregaRequest req) {
        return entregaService.registrar(usuario.getId(), req);
    }

    @GetMapping("/entregas/{id}")
    public EntregaResponse entrega(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return entregaService.buscar(usuario, id);
    }

    @PutMapping("/entregas/{id}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public EntregaResponse atualizarEntrega(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
                                            @Valid @RequestBody EntregaAtualizacaoRequest req) {
        return entregaService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/entregas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CEREALISTA')")
    public void excluirEntrega(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        entregaService.excluir(usuario.getId(), id);
    }
}
