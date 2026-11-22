package br.com.safralink.service;

import br.com.safralink.web.dto.CatalogoDtos.EntregaAtualizacaoRequest;
import java.math.BigDecimal;
import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.Entrega;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.Produtor;
import br.com.safralink.domain.TipoNotificacao;
import br.com.safralink.repository.CerealistaRepository;
import br.com.safralink.repository.EntregaRepository;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.util.Tempo;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.CatalogoDtos.EntregaRequest;
import br.com.safralink.web.dto.CatalogoDtos.EntregaResponse;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaService {
    private final EntregaRepository entregaRepository;
    private final ProdutorRepository produtorRepository;
    private final CerealistaRepository cerealistaRepository;
    private final ProdutoService produtoService;
    private final SaldoService saldoService;
    private final AuditoriaService auditoria;
    private final NotificacaoService notificacoes;

    public EntregaService(EntregaRepository entregaRepository, ProdutorRepository produtorRepository,
                          CerealistaRepository cerealistaRepository, ProdutoService produtoService,
                          SaldoService saldoService, AuditoriaService auditoria, NotificacaoService notificacoes) {
        this.entregaRepository = entregaRepository;
        this.produtorRepository = produtorRepository;
        this.cerealistaRepository = cerealistaRepository;
        this.produtoService = produtoService;
        this.saldoService = saldoService;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
    }

    @Transactional
    public EntregaResponse registrar(Long cerealistaId, EntregaRequest req) {
        Cerealista cerealista = cerealistaRepository.findById(cerealistaId)
                .orElseThrow(() -> ApiException.proibido("Apenas a cerealista registra entregas."));
        Produtor produtor = produtorRepository.findById(req.produtorId())
                .filter(Produtor::isAtivo)
                .orElseThrow(() -> ApiException.naoEncontrado("Produtor não encontrado."));
        Produto produto = produtoService.buscarAtivo(req.produtoId());
        LocalDate data = req.dataEntrega() == null ? Tempo.hoje() : req.dataEntrega();
        if (data.isAfter(Tempo.hoje())) {
            throw ApiException.regraDeNegocio("A data da entrega não pode ser futura.");
        }

        Entrega entrega = new Entrega();
        entrega.setProdutor(produtor);
        entrega.setProduto(produto);
        entrega.setQuantidade(req.quantidade());
        entrega.setDataEntrega(data);
        entrega.setObservacao(Mapeador.limpar(req.observacao()));
        entrega.setRegistradaPor(cerealista);
        entrega = entregaRepository.save(entrega);

        saldoService.creditar(produtor, produto, req.quantidade());

        auditoria.registrar(cerealista, AcaoAuditoria.ENTREGA_REGISTRADA, "entregas", entrega.getId(),
                req.quantidade() + " sc de " + produto.getNome() + " de " + produtor.getNome());
        notificacoes.notificar(produtor, TipoNotificacao.ENTREGA_REGISTRADA,
                "Entrega registrada: " + req.quantidade() + " sc de " + produto.getNome() + " creditadas no seu saldo");
        return resposta(entrega);
    }

    @Transactional(readOnly = true)
    public PageResponse<EntregaResponse> listar(UsuarioPrincipal usuario, int pagina, int tamanho) {
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), AuditoriaService.limitar(tamanho));
        Page<Entrega> page = usuario.isCerealista()
                ? entregaRepository.findAllByOrderByDataEntregaDescIdDesc(pageable)
                : entregaRepository.findByProdutorIdOrderByDataEntregaDescIdDesc(usuario.getId(), pageable);
        return PageResponse.de(page.map(this::resposta));
    }

    private EntregaResponse resposta(Entrega e) {
        return new EntregaResponse(e.getId(), e.getProdutor().getId(), e.getProdutor().getNome(),
                e.getProduto().getId(), e.getProduto().getNome(), e.getQuantidade(), e.getDataEntrega(),
                e.getObservacao(), e.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public EntregaResponse buscar(UsuarioPrincipal usuario, Long id) {
        Entrega e = carregar(id);
        if (!usuario.isCerealista() && !e.getProdutor().getId().equals(usuario.getId())) {
            throw ApiException.proibido("Você só pode consultar as suas próprias entregas.");
        }
        return resposta(e);
    }

    @Transactional
    public EntregaResponse atualizar(Long cerealistaId, Long id, EntregaAtualizacaoRequest req) {
        Entrega e = carregar(id);
        LocalDate data = req.dataEntrega() == null ? e.getDataEntrega() : req.dataEntrega();
        if (data.isAfter(Tempo.hoje())) {
            throw ApiException.regraDeNegocio("A data da entrega não pode ser futura.");
        }
        BigDecimal variacao = req.quantidade().subtract(e.getQuantidade());
        if (variacao.signum() != 0) {
            saldoService.ajustar(e.getProdutor().getId(), e.getProduto().getId(), variacao);
        }
        e.setQuantidade(req.quantidade());
        e.setDataEntrega(data);
        e.setObservacao(Mapeador.limpar(req.observacao()));
        auditoria.registrar(cerealistaRepository.findById(cerealistaId)
                        .orElseThrow(() -> ApiException.proibido("Apenas a cerealista altera entregas.")),
                AcaoAuditoria.ENTREGA_ALTERADA, "entregas", id,
                req.quantidade() + " sc de " + e.getProduto().getNome() + " de " + e.getProdutor().getNome());
        return resposta(e);
    }

    @Transactional
    public void excluir(Long cerealistaId, Long id) {
        Entrega e = carregar(id);
        saldoService.ajustar(e.getProdutor().getId(), e.getProduto().getId(), e.getQuantidade().negate());
        entregaRepository.delete(e);
        auditoria.registrar(cerealistaRepository.findById(cerealistaId)
                        .orElseThrow(() -> ApiException.proibido("Apenas a cerealista exclui entregas.")),
                AcaoAuditoria.ENTREGA_EXCLUIDA, "entregas", id,
                e.getQuantidade() + " sc de " + e.getProduto().getNome() + " de " + e.getProdutor().getNome());
    }

    private Entrega carregar(Long id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Entrega não encontrada."));
    }
}
