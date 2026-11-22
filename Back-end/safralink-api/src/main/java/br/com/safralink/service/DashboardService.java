package br.com.safralink.service;

import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.domain.Transacao;
import br.com.safralink.util.Tempo;
import br.com.safralink.web.dto.OutrosDtos.CompraPorDia;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.repository.RequisicaoVendaRepository;
import br.com.safralink.repository.TransacaoRepository;
import br.com.safralink.web.dto.CatalogoDtos.SaldoResponse;
import br.com.safralink.web.dto.OutrosDtos.CompraPorProduto;
import br.com.safralink.web.dto.OutrosDtos.DashboardCerealistaResponse;
import br.com.safralink.web.dto.OutrosDtos.DashboardProdutorResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ProdutoService produtoService;
    private final SaldoService saldoService;
    private final RequisicaoService requisicaoService;
    private final RequisicaoVendaRepository requisicaoRepository;
    private final TransacaoRepository transacaoRepository;
    private final ProdutorRepository produtorRepository;

    public DashboardService(ProdutoService produtoService, SaldoService saldoService,
                            RequisicaoService requisicaoService, RequisicaoVendaRepository requisicaoRepository,
                            TransacaoRepository transacaoRepository, ProdutorRepository produtorRepository) {
        this.produtoService = produtoService;
        this.saldoService = saldoService;
        this.requisicaoService = requisicaoService;
        this.requisicaoRepository = requisicaoRepository;
        this.transacaoRepository = transacaoRepository;
        this.produtorRepository = produtorRepository;
    }

    @Transactional(readOnly = true)
    public DashboardProdutorResponse doProdutor(Long produtorId) {
        List<SaldoResponse> saldos = saldoService.listarDoProdutor(produtorId);
        BigDecimal totalSacas = saldos.stream().map(SaldoResponse::quantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valor = saldos.stream().map(SaldoResponse::valorEstimado).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardProdutorResponse(
                produtoService.listar(),
                saldos,
                totalSacas,
                valor,
                requisicaoRepository.countByProdutorIdAndStatus(produtorId, StatusRequisicao.PENDENTE),
                requisicaoRepository.findTop5ByProdutorIdOrderByCriadoEmDesc(produtorId).stream()
                        .map(requisicaoService::resposta).toList());
    }

    private List<CompraPorDia> comprasDosUltimosDias(int dias) {
        LocalDate hoje = Tempo.hoje();
        LocalDate inicio = hoje.minusDays(dias - 1L);
        Map<LocalDate, BigDecimal[]> acumulado = new HashMap<>();
        for (Transacao t : transacaoRepository.findByCriadoEmGreaterThanEqual(Tempo.inicioDoDia(inicio))) {
            LocalDate dia = t.getCriadoEm().atZone(Tempo.ZONA).toLocalDate();
            BigDecimal[] soma = acumulado.computeIfAbsent(dia, d -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            soma[0] = soma[0].add(t.getValorBruto());
            soma[1] = soma[1].add(t.getRequisicao().getQuantidade());
        }
        List<CompraPorDia> serie = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate dia = inicio.plusDays(i);
            BigDecimal[] soma = acumulado.getOrDefault(dia, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            serie.add(new CompraPorDia(dia, soma[0], soma[1]));
        }
        return serie;
    }

    @Transactional(readOnly = true)
    public DashboardCerealistaResponse daCerealista() {
        List<CompraPorProduto> porProduto = transacaoRepository.comprasPorProduto().stream()
                .map(l -> new CompraPorProduto((String) l[0], (BigDecimal) l[1], (BigDecimal) l[2])).toList();
        return new DashboardCerealistaResponse(
                transacaoRepository.totalSacas(),
                transacaoRepository.totalBruto(),
                transacaoRepository.totalDescontos(),
                produtorRepository.countByAtivoTrue(),
                requisicaoRepository.countByStatus(StatusRequisicao.PENDENTE),
                porProduto,
                comprasDosUltimosDias(30),
                requisicaoRepository.findTop5ByStatusOrderByCriadoEmDesc(StatusRequisicao.PENDENTE).stream()
                        .map(requisicaoService::resposta).toList());
    }
}
