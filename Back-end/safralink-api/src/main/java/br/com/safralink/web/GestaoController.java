package br.com.safralink.web;

import br.com.safralink.web.dto.AuthDtos.RegistroRequest;
import br.com.safralink.web.dto.UsuarioDtos.PerfilRequest;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.AuditoriaService;
import br.com.safralink.service.DashboardService;
import br.com.safralink.service.DescontoService;
import br.com.safralink.service.ProdutorService;
import br.com.safralink.service.TransacaoService;
import br.com.safralink.web.dto.CatalogoDtos.SaldoResponse;
import br.com.safralink.web.dto.OutrosDtos.AuditoriaResponse;
import br.com.safralink.web.dto.OutrosDtos.DashboardCerealistaResponse;
import br.com.safralink.web.dto.OutrosDtos.DashboardProdutorResponse;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import br.com.safralink.web.dto.OutrosDtos.ProdutorResumoResponse;
import br.com.safralink.web.dto.RequisicaoDtos.DescontoRequest;
import br.com.safralink.web.dto.RequisicaoDtos.DescontoResponse;
import br.com.safralink.web.dto.RequisicaoDtos.TransacaoResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GestaoController {
    private final TransacaoService transacaoService;
    private final DescontoService descontoService;
    private final ProdutorService produtorService;
    private final DashboardService dashboardService;
    private final AuditoriaService auditoriaService;

    public GestaoController(TransacaoService transacaoService, DescontoService descontoService,
                            ProdutorService produtorService, DashboardService dashboardService,
                            AuditoriaService auditoriaService) {
        this.transacaoService = transacaoService;
        this.descontoService = descontoService;
        this.produtorService = produtorService;
        this.dashboardService = dashboardService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/transacoes")
    public PageResponse<TransacaoResponse> transacoes(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) Long produtorId,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) StatusRequisicao status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return transacaoService.listar(usuario, produtorId, produtoId, status, de, ate, pagina, tamanho);
    }

    @GetMapping("/transacoes/{id}")
    public TransacaoResponse transacao(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return transacaoService.buscar(usuario, id);
    }

    @GetMapping("/auditoria/{id}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public AuditoriaResponse auditoria(@PathVariable Long id) {
        return auditoriaService.buscar(id);
    }

    @GetMapping("/descontos")
    public List<DescontoResponse> descontos() {
        return descontoService.listar();
    }

    @PostMapping("/descontos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CEREALISTA')")
    public DescontoResponse criarDesconto(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                          @Valid @RequestBody DescontoRequest req) {
        return descontoService.criar(usuario.getId(), req);
    }

    @PutMapping("/descontos/{id}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public DescontoResponse atualizarDesconto(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                              @PathVariable Long id, @Valid @RequestBody DescontoRequest req) {
        return descontoService.atualizar(usuario.getId(), id, req);
    }

    @GetMapping("/produtores")
    @PreAuthorize("hasRole('CEREALISTA')")
    public List<ProdutorResumoResponse> produtores(@RequestParam(required = false) String busca) {
        return produtorService.listar(busca);
    }

    @GetMapping("/produtores/{id}/saldos")
    @PreAuthorize("hasRole('CEREALISTA')")
    public List<SaldoResponse> saldosDoProdutor(@PathVariable Long id) {
        return produtorService.saldosDoProdutor(id);
    }

    @GetMapping("/dashboard/produtor")
    @PreAuthorize("hasRole('PRODUTOR')")
    public DashboardProdutorResponse dashboardProdutor(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return dashboardService.doProdutor(usuario.getId());
    }

    @GetMapping("/dashboard/cerealista")
    @PreAuthorize("hasRole('CEREALISTA')")
    public DashboardCerealistaResponse dashboardCerealista() {
        return dashboardService.daCerealista();
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('CEREALISTA')")
    public PageResponse<AuditoriaResponse> auditoria(
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return auditoriaService.listar(acao, de, ate, pagina, tamanho);
    }

    @GetMapping("/descontos/{id}")
    public DescontoResponse desconto(@PathVariable Long id) {
        return descontoService.buscar(id);
    }

    @DeleteMapping("/descontos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CEREALISTA')")
    public void excluirDesconto(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        descontoService.excluir(usuario.getId(), id);
    }

    @GetMapping("/produtores/{id}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public ProdutorResumoResponse produtor(@PathVariable Long id) {
        return produtorService.buscar(id);
    }

    @PostMapping("/produtores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CEREALISTA')")
    public UsuarioResponse criarProdutor(@Valid @RequestBody RegistroRequest req) {
        return produtorService.criar(req);
    }

    @PutMapping("/produtores/{id}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public ProdutorResumoResponse atualizarProdutor(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                    @PathVariable Long id, @Valid @RequestBody PerfilRequest req) {
        return produtorService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/produtores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CEREALISTA')")
    public void excluirProdutor(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        produtorService.excluir(usuario.getId(), id);
    }
}
