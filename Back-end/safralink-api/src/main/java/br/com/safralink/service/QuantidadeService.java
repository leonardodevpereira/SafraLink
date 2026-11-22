package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.QuantidadeDisponivel;
import br.com.safralink.repository.ProdutoRepository;
import br.com.safralink.repository.QuantidadeDisponivelRepository;
import br.com.safralink.web.dto.CatalogoDtos.QuantidadeResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuantidadeService {
    private final QuantidadeDisponivelRepository repository;
    private final ProdutoRepository produtoRepository;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;

    public QuantidadeService(QuantidadeDisponivelRepository repository, ProdutoRepository produtoRepository,
                             ProdutoService produtoService, UsuarioService usuarioService,
                             AuditoriaService auditoria) {
        this.repository = repository;
        this.produtoRepository = produtoRepository;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<QuantidadeResponse> listar() {
        return produtoRepository.findByAtivoTrueOrderByIdAsc().stream().map(p -> {
            var q = repository.findByProdutoId(p.getId()).orElse(null);
            return new QuantidadeResponse(p.getId(), p.getNome(),
                    q == null ? BigDecimal.ZERO : q.getQuantidade(), q == null ? null : q.getAtualizadoEm());
        }).toList();
    }

    @Transactional
    public QuantidadeResponse definir(Long cerealistaId, Long produtoId, BigDecimal quantidade) {
        Produto produto = produtoService.buscarAtivo(produtoId);
        QuantidadeDisponivel q = repository.buscarParaAtualizar(produtoId).orElseGet(() -> {
            QuantidadeDisponivel nova = new QuantidadeDisponivel();
            nova.setProduto(produto);
            return nova;
        });
        BigDecimal anterior = q.getQuantidade();
        q.setQuantidade(quantidade);
        q = repository.save(q);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.ALTERACAO_QUANTIDADE, "produtos",
                produtoId, produto.getNome() + ": " + anterior + " -> " + quantidade + " sacas");
        return new QuantidadeResponse(produtoId, produto.getNome(), q.getQuantidade(), q.getAtualizadoEm());
    }

    @Transactional(readOnly = true)
    public QuantidadeResponse buscar(Long produtoId) {
        Produto produto = produtoService.buscarAtivo(produtoId);
        var q = repository.findByProdutoId(produtoId).orElse(null);
        return new QuantidadeResponse(produtoId, produto.getNome(),
                q == null ? BigDecimal.ZERO : q.getQuantidade(), q == null ? null : q.getAtualizadoEm());
    }

    @Transactional(readOnly = true)
    public List<QuantidadeResponse> abaixoDe(BigDecimal limite) {
        return repository.abaixoDe(limite).stream()
                .filter(q -> q.getProduto().isAtivo())
                .map(q -> new QuantidadeResponse(q.getProduto().getId(), q.getProduto().getNome(), q.getQuantidade(),
                        q.getAtualizadoEm()))
                .toList();
    }
}
