package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.Preco;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.TipoNotificacao;
import br.com.safralink.repository.CerealistaRepository;
import br.com.safralink.repository.PrecoRepository;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.CatalogoDtos.PrecoResponse;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrecoService {
    private final PrecoRepository precoRepository;
    private final CerealistaRepository cerealistaRepository;
    private final ProdutoService produtoService;
    private final AuditoriaService auditoria;
    private final NotificacaoService notificacoes;

    public PrecoService(PrecoRepository precoRepository, CerealistaRepository cerealistaRepository,
                        ProdutoService produtoService, AuditoriaService auditoria, NotificacaoService notificacoes) {
        this.precoRepository = precoRepository;
        this.cerealistaRepository = cerealistaRepository;
        this.produtoService = produtoService;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
    }

    @Transactional
    public PrecoResponse definir(Long cerealistaId, Long produtoId, BigDecimal valorSaca) {
        Cerealista cerealista = cerealistaRepository.findById(cerealistaId)
                .orElseThrow(() -> ApiException.proibido("Apenas a cerealista pode alterar preços."));
        Produto produto = produtoService.buscarAtivo(produtoId);
        BigDecimal anterior = produtoService.precoAtual(produtoId).orElse(null);

        Preco preco = new Preco();
        preco.setProduto(produto);
        preco.setValorSaca(valorSaca);
        preco.setDefinidoPor(cerealista);
        preco = precoRepository.save(preco);

        String detalhe = produto.getNome() + ": " + (anterior == null ? "novo preço " : moeda(anterior) + " -> ")
                + moeda(valorSaca);
        auditoria.registrar(cerealista, AcaoAuditoria.ALTERACAO_PRECO, "produtos", produtoId, detalhe);
        notificacoes.notificarProdutores(TipoNotificacao.PRECO_ATUALIZADO,
                "Preço de " + produto.getNome() + " atualizado para " + moeda(valorSaca) + "/sc");
        return resposta(preco);
    }

    @Transactional(readOnly = true)
    public List<PrecoResponse> historico(Long produtoId, int dias) {
        produtoService.buscarAtivo(produtoId);
        int janela = Math.min(Math.max(dias, 1), 365);
        Instant desde = Instant.now().minus(Duration.ofDays(janela));
        return precoRepository.findByProdutoIdAndCriadoEmGreaterThanEqualOrderByCriadoEmAscIdAsc(produtoId, desde)
                .stream().map(this::resposta).toList();
    }

    private PrecoResponse resposta(Preco p) {
        return new PrecoResponse(p.getId(), p.getProduto().getId(), p.getProduto().getNome(), p.getValorSaca(),
                p.getCriadoEm());
    }

    static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor);
    }

    @Transactional(readOnly = true)
    public PrecoResponse buscar(Long id) {
        return resposta(precoRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Preço não encontrado.")));
    }

    @Transactional
    public void excluir(Long cerealistaId, Long id) {
        Preco preco = precoRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Preço não encontrado."));
        Long produtoId = preco.getProduto().getId();
        if (precoRepository.countByProdutoId(produtoId) <= 1) {
            throw ApiException.conflito("Não é possível excluir o único preço do produto.");
        }
        precoRepository.delete(preco);
        auditoria.registrar(cerealistaRepository.findById(cerealistaId)
                        .orElseThrow(() -> ApiException.proibido("Apenas a cerealista pode excluir preços.")),
                AcaoAuditoria.PRECO_EXCLUIDO, "precos", id,
                preco.getProduto().getNome() + ": " + moeda(preco.getValorSaca()));
    }
}
