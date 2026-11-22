package br.com.safralink.web;

import br.com.safralink.domain.TipoGrao;
import br.com.safralink.web.dto.CatalogoDtos.ProdutoRequest;
import br.com.safralink.web.dto.CatalogoDtos.PrecoCriarRequest;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.PrecoService;
import br.com.safralink.service.ProdutoService;
import br.com.safralink.service.QuantidadeService;
import br.com.safralink.web.dto.CatalogoDtos.PrecoRequest;
import br.com.safralink.web.dto.CatalogoDtos.PrecoResponse;
import br.com.safralink.web.dto.CatalogoDtos.ProdutoResponse;
import br.com.safralink.web.dto.CatalogoDtos.QuantidadeRequest;
import br.com.safralink.web.dto.CatalogoDtos.QuantidadeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogoController {
    private final ProdutoService produtoService;
    private final PrecoService precoService;
    private final QuantidadeService quantidadeService;

    public CatalogoController(ProdutoService produtoService, PrecoService precoService,
                              QuantidadeService quantidadeService) {
        this.produtoService = produtoService;
        this.precoService = precoService;
        this.quantidadeService = quantidadeService;
    }

    @GetMapping("/produtos")
    public List<ProdutoResponse> produtos(@RequestParam(required = false) TipoGrao grao) {
        return produtoService.listar(grao);
    }

    @GetMapping("/produtos/{id}")
    public ProdutoResponse produto(@PathVariable Long id) {
        return produtoService.buscar(id);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CEREALISTA')")
    public ProdutoResponse criarProduto(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                        @Valid @RequestBody ProdutoRequest req) {
        return produtoService.criar(usuario.getId(), req);
    }

    @PutMapping("/produtos/{id}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public ProdutoResponse atualizarProduto(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
                                            @Valid @RequestBody ProdutoRequest req) {
        return produtoService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/produtos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CEREALISTA')")
    public void excluirProduto(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        produtoService.excluir(usuario.getId(), id);
    }

    @GetMapping("/precos/{id}")
    public PrecoResponse preco(@PathVariable Long id) {
        return precoService.buscar(id);
    }

    @PostMapping("/precos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CEREALISTA')")
    public PrecoResponse criarPreco(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                    @Valid @RequestBody PrecoCriarRequest req) {
        return precoService.definir(usuario.getId(), req.produtoId(), req.valorSaca());
    }

    @DeleteMapping("/precos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CEREALISTA')")
    public void excluirPreco(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        precoService.excluir(usuario.getId(), id);
    }

    @GetMapping("/precos/historico")
    public List<PrecoResponse> historico(@RequestParam Long produtoId, @RequestParam(defaultValue = "30") int dias) {
        return precoService.historico(produtoId, dias);
    }

    @PutMapping("/precos/{produtoId}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public PrecoResponse definirPreco(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long produtoId,
                                      @Valid @RequestBody PrecoRequest req) {
        return precoService.definir(usuario.getId(), produtoId, req.valorSaca());
    }

    @GetMapping("/quantidades")
    public List<QuantidadeResponse> quantidades(@RequestParam(required = false) BigDecimal abaixoDe) {
        return abaixoDe == null ? quantidadeService.listar() : quantidadeService.abaixoDe(abaixoDe);
    }

    @GetMapping("/quantidades/{produtoId}")
    public QuantidadeResponse quantidade(@PathVariable Long produtoId) {
        return quantidadeService.buscar(produtoId);
    }

    @PutMapping("/quantidades/{produtoId}")
    @PreAuthorize("hasRole('CEREALISTA')")
    public QuantidadeResponse definirQuantidade(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                @PathVariable Long produtoId,
                                                @Valid @RequestBody QuantidadeRequest req) {
        return quantidadeService.definir(usuario.getId(), produtoId, req.quantidade());
    }
}
