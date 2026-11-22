package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Preco;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.QuantidadeDisponivel;
import br.com.safralink.domain.TipoGrao;
import br.com.safralink.repository.EntregaRepository;
import br.com.safralink.repository.PrecoRepository;
import br.com.safralink.repository.ProdutoRepository;
import br.com.safralink.repository.QuantidadeDisponivelRepository;
import br.com.safralink.repository.RequisicaoVendaRepository;
import br.com.safralink.repository.SaldoRepository;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.CatalogoDtos.ProdutoRequest;
import br.com.safralink.web.dto.CatalogoDtos.ProdutoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepository;
    private final PrecoRepository precoRepository;
    private final QuantidadeDisponivelRepository quantidadeRepository;
    private final EntregaRepository entregaRepository;
    private final SaldoRepository saldoRepository;
    private final RequisicaoVendaRepository requisicaoRepository;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;

    public ProdutoService(ProdutoRepository produtoRepository, PrecoRepository precoRepository,
                          QuantidadeDisponivelRepository quantidadeRepository, EntregaRepository entregaRepository,
                          SaldoRepository saldoRepository, RequisicaoVendaRepository requisicaoRepository,
                          UsuarioService usuarioService, AuditoriaService auditoria) {
        this.produtoRepository = produtoRepository;
        this.precoRepository = precoRepository;
        this.quantidadeRepository = quantidadeRepository;
        this.entregaRepository = entregaRepository;
        this.saldoRepository = saldoRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.usuarioService = usuarioService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return listar(null);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar(TipoGrao grao) {
        List<Produto> produtos = grao == null
                ? produtoRepository.findByAtivoTrueOrderByIdAsc()
                : produtoRepository.findByGraoAndAtivoTrueOrderByIdAsc(grao);
        return produtos.stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return resposta(buscarAtivo(id));
    }

    @Transactional
    public ProdutoResponse criar(Long cerealistaId, ProdutoRequest req) {
        if (produtoRepository.existsByGraoAndMercado(req.grao(), req.mercado())) {
            throw ApiException.conflito("Já existe um produto de " + req.grao() + " no mercado " + req.mercado() + ".");
        }
        Produto p = new Produto();
        p.setNome(req.nome().trim());
        p.setGrao(req.grao());
        p.setMercado(req.mercado());
        p = produtoRepository.save(p);
        QuantidadeDisponivel q = new QuantidadeDisponivel();
        q.setProduto(p);
        quantidadeRepository.save(q);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.PRODUTO_CRIADO, "produtos", p.getId(),
                p.getNome());
        return resposta(p);
    }

    @Transactional
    public ProdutoResponse atualizar(Long cerealistaId, Long id, ProdutoRequest req) {
        Produto p = buscarAtivo(id);
        if (produtoRepository.existsByGraoAndMercadoAndIdNot(req.grao(), req.mercado(), id)) {
            throw ApiException.conflito("Já existe um produto de " + req.grao() + " no mercado " + req.mercado() + ".");
        }
        p.setNome(req.nome().trim());
        p.setGrao(req.grao());
        p.setMercado(req.mercado());
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.PRODUTO_ALTERADO, "produtos", id,
                p.getNome());
        return resposta(p);
    }

    @Transactional
    public void excluir(Long cerealistaId, Long id) {
        Produto p = buscarAtivo(id);
        if (precoRepository.existsByProdutoId(id) || entregaRepository.existsByProdutoId(id)
                || saldoRepository.existsByProdutoId(id) || requisicaoRepository.existsByProdutoId(id)) {
            throw ApiException.conflito("O produto possui preços, entregas, saldos ou requisições associados e não pode ser excluído.");
        }
        quantidadeRepository.findByProdutoId(id).ifPresent(quantidadeRepository::delete);
        produtoRepository.delete(p);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.PRODUTO_EXCLUIDO, "produtos", id,
                p.getNome());
    }

    @Transactional(readOnly = true)
    public Produto buscarAtivo(Long id) {
        return produtoRepository.findById(id).filter(Produto::isAtivo)
                .orElseThrow(() -> ApiException.naoEncontrado("Produto não encontrado."));
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> precoAtual(Long produtoId) {
        return precoRepository.findFirstByProdutoIdOrderByCriadoEmDescIdDesc(produtoId).map(Preco::getValorSaca);
    }

    public ProdutoResponse resposta(Produto p) {
        List<Preco> ultimos = precoRepository.findTop2ByProdutoIdOrderByCriadoEmDescIdDesc(p.getId());
        Preco atual = ultimos.isEmpty() ? null : ultimos.get(0);
        Preco anterior = ultimos.size() > 1 ? ultimos.get(1) : null;
        BigDecimal variacao = null;
        if (atual != null && anterior != null && anterior.getValorSaca().signum() > 0) {
            variacao = atual.getValorSaca().subtract(anterior.getValorSaca())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(anterior.getValorSaca(), 2, RoundingMode.HALF_UP);
        }
        BigDecimal quantidade = quantidadeRepository.findByProdutoId(p.getId())
                .map(q -> q.getQuantidade()).orElse(BigDecimal.ZERO);
        return new ProdutoResponse(p.getId(), p.getNome(), p.getGrao(), p.getMercado(),
                atual == null ? null : atual.getValorSaca(), variacao,
                atual == null ? null : atual.getCriadoEm(), quantidade);
    }
}
