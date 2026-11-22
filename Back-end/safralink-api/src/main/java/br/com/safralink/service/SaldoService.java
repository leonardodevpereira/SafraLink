package br.com.safralink.service;

import br.com.safralink.web.ApiException;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.Produtor;
import br.com.safralink.domain.Saldo;
import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.repository.ProdutoRepository;
import br.com.safralink.repository.RequisicaoVendaRepository;
import br.com.safralink.repository.SaldoRepository;
import br.com.safralink.web.dto.CatalogoDtos.SaldoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaldoService {
    private final SaldoRepository saldoRepository;
    private final ProdutoRepository produtoRepository;
    private final RequisicaoVendaRepository requisicaoRepository;
    private final ProdutoService produtoService;

    public SaldoService(SaldoRepository saldoRepository, ProdutoRepository produtoRepository,
                        RequisicaoVendaRepository requisicaoRepository, ProdutoService produtoService) {
        this.saldoRepository = saldoRepository;
        this.produtoRepository = produtoRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.produtoService = produtoService;
    }

    @Transactional(readOnly = true)
    public List<SaldoResponse> listarDoProdutor(Long produtorId) {
        Map<Long, BigDecimal> saldos = saldoRepository.findByProdutorId(produtorId).stream()
                .collect(Collectors.toMap(s -> s.getProduto().getId(), Saldo::getQuantidade, BigDecimal::add));
        return produtoRepository.findByAtivoTrueOrderByIdAsc().stream().map(produto -> {
            BigDecimal quantidade = saldos.getOrDefault(produto.getId(), BigDecimal.ZERO);
            BigDecimal reservado = reservado(produtorId, produto.getId());
            BigDecimal disponivel = quantidade.subtract(reservado).max(BigDecimal.ZERO);
            BigDecimal preco = produtoService.precoAtual(produto.getId()).orElse(null);
            BigDecimal estimado = preco == null ? BigDecimal.ZERO
                    : quantidade.multiply(preco).setScale(2, RoundingMode.HALF_UP);
            return new SaldoResponse(produto.getId(), produto.getNome(), quantidade, reservado, disponivel, preco,
                    estimado);
        }).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal disponivelParaVenda(Long produtorId, Long produtoId) {
        BigDecimal saldo = saldoRepository.findByProdutorIdAndProdutoId(produtorId, produtoId)
                .map(Saldo::getQuantidade).orElse(BigDecimal.ZERO);
        return saldo.subtract(reservado(produtorId, produtoId)).max(BigDecimal.ZERO);
    }

    @Transactional
    public void creditar(Produtor produtor, Produto produto, BigDecimal quantidade) {
        Saldo saldo = saldoRepository.buscarParaAtualizar(produtor.getId(), produto.getId()).orElseGet(() -> {
            Saldo novo = new Saldo();
            novo.setProdutor(produtor);
            novo.setProduto(produto);
            return novo;
        });
        saldo.creditar(quantidade);
        saldoRepository.save(saldo);
    }

    private BigDecimal reservado(Long produtorId, Long produtoId) {
        return requisicaoRepository.somarQuantidade(produtorId, produtoId, StatusRequisicao.PENDENTE);
    }

    @Transactional
    public void ajustar(Long produtorId, Long produtoId, BigDecimal variacao) {
        Saldo saldo = saldoRepository.buscarParaAtualizar(produtorId, produtoId)
                .orElseThrow(() -> ApiException.regraDeNegocio("O produtor não tem saldo neste produto."));
        BigDecimal novo = saldo.getQuantidade().add(variacao);
        if (novo.compareTo(reservado(produtorId, produtoId)) < 0) {
            throw ApiException.conflito("A operação deixaria o saldo (" + novo.max(BigDecimal.ZERO)
                    + " sc) abaixo do que já foi vendido ou está reservado em requisições pendentes.");
        }
        saldo.setQuantidade(novo);
    }
}
