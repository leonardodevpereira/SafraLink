package br.com.safralink.web;

import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoAtualizacaoRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.RequisicaoService;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import br.com.safralink.web.dto.RequisicaoDtos.ContraPropostaRequest;
import br.com.safralink.web.dto.RequisicaoDtos.RecusaRequest;
import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoRequest;
import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RequisicaoController {
    private final RequisicaoService service;

    public RequisicaoController(RequisicaoService service) {
        this.service = service;
    }

    @GetMapping("/requisicoes")
    public PageResponse<RequisicaoResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                   @RequestParam(required = false) StatusRequisicao status,
                                                   @RequestParam(required = false) Long produtorId,
                                                   @RequestParam(defaultValue = "0") int pagina,
                                                   @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(usuario, status, produtorId, pagina, tamanho);
    }

    @GetMapping("/requisicoes/{id}")
    public RequisicaoResponse buscar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return service.buscar(usuario, id);
    }

    @PostMapping("/requisicoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PRODUTOR')")
    public RequisicaoResponse criar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                    @Valid @RequestBody RequisicaoRequest req) {
        return service.criar(usuario, req);
    }

    @PostMapping("/requisicoes/{id}/reenviar")
    @PreAuthorize("hasRole('PRODUTOR')")
    public RequisicaoResponse reenviar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return service.reenviar(usuario, id);
    }

    @PostMapping("/requisicoes/{id}/aprovar")
    @PreAuthorize("hasRole('CEREALISTA')")
    public RequisicaoResponse aprovar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return service.aprovar(usuario, id);
    }

    @PostMapping("/requisicoes/{id}/recusar")
    @PreAuthorize("hasRole('CEREALISTA')")
    public RequisicaoResponse recusar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
                                      @Valid @RequestBody RecusaRequest req) {
        return service.recusar(usuario, id, req.motivo());
    }

    @PostMapping("/requisicoes/{id}/processar")
    @PreAuthorize("hasRole('CEREALISTA')")
    public RequisicaoResponse processar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return service.processar(usuario, id);
    }

    @PostMapping("/requisicoes/{id}/contra-propostas")
    @ResponseStatus(HttpStatus.CREATED)
    public RequisicaoResponse contraProposta(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                             @PathVariable Long id, @Valid @RequestBody ContraPropostaRequest req) {
        return service.enviarContraProposta(usuario, id, req);
    }

    @PostMapping("/contra-propostas/{id}/aceitar")
    public RequisicaoResponse aceitar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return service.responderContraProposta(usuario, id, true);
    }

    @PostMapping("/contra-propostas/{id}/recusar")
    public RequisicaoResponse recusarContraProposta(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                    @PathVariable Long id) {
        return service.responderContraProposta(usuario, id, false);
    }

    @PutMapping("/requisicoes/{id}")
    @PreAuthorize("hasRole('PRODUTOR')")
    public RequisicaoResponse atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
                                        @Valid @RequestBody RequisicaoAtualizacaoRequest req) {
        return service.atualizar(usuario, id, req);
    }

    @DeleteMapping("/requisicoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PRODUTOR')")
    public void excluir(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        service.excluir(usuario, id);
    }
}
