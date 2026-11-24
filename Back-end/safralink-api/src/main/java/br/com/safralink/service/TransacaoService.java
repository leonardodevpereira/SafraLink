package br.com.safralink.service;

import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.Desconto;
import br.com.safralink.domain.Endereco;
import br.com.safralink.domain.DescontoAplicado;
import br.com.safralink.domain.RequisicaoVenda;
import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.domain.Transacao;
import br.com.safralink.repository.DescontoRepository;
import br.com.safralink.repository.TransacaoRepository;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.util.Tempo;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import br.com.safralink.web.dto.RequisicaoDtos.DescontoAplicadoResponse;
import br.com.safralink.web.dto.RequisicaoDtos.TransacaoResponse;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransacaoService {
    private static final Sort ORDEM = Sort.by(Sort.Direction.DESC, "criadoEm", "id");

    private final TransacaoRepository repository;
    private final DescontoRepository descontoRepository;

    public TransacaoService(TransacaoRepository repository, DescontoRepository descontoRepository) {
        this.repository = repository;
        this.descontoRepository = descontoRepository;
    }

    @Transactional
    public Transacao gerar(RequisicaoVenda requisicao) {
        BigDecimal bruto = requisicao.valorBruto();
        Transacao transacao = new Transacao();
        transacao.setRequisicao(requisicao);
        transacao.setValorBruto(bruto);

        BigDecimal total = BigDecimal.ZERO;
        for (Desconto desconto : descontoRepository.findByAtivoTrueOrderByIdAsc()) {
            BigDecimal valor = DescontoService.calcular(desconto, bruto, requisicao.getQuantidade());
            if (valor.signum() > 0) {
                DescontoAplicado aplicado = new DescontoAplicado();
                aplicado.setDesconto(desconto);
                aplicado.setNome(desconto.getNome());
                aplicado.setValorAplicado(valor);
                transacao.adicionarDesconto(aplicado);
                total = total.add(valor);
            }
        }
        transacao.setTotalDescontos(total);
        transacao.setValorLiquido(bruto.subtract(total).max(BigDecimal.ZERO));
        return repository.save(transacao);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransacaoResponse> listar(UsuarioPrincipal usuario, Long produtorId, Long produtoId,
                                                  StatusRequisicao status, LocalDate de, LocalDate ate, int pagina,
                                                  int tamanho) {
        var page = repository.findAll(filtro(usuario, produtorId, produtoId, status, de, ate),
                PageRequest.of(Math.max(pagina, 0), AuditoriaService.limitar(tamanho), ORDEM));
        return PageResponse.de(page.map(TransacaoService::resposta));
    }

    @Transactional(readOnly = true)
    public TransacaoResponse buscar(UsuarioPrincipal usuario, Long id) {
        Transacao t = repository.findById(id)
                .filter(x -> usuario.isCerealista()
                        || x.getRequisicao().getProdutor().getId().equals(usuario.getId()))
                .orElseThrow(() -> ApiException.naoEncontrado("Transação não encontrada."));
        return resposta(t);
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listarParaRelatorio(UsuarioPrincipal usuario, LocalDate de, LocalDate ate) {
        return repository.findAll(filtro(usuario, null, null, null, de, ate), ORDEM).stream()
                .map(TransacaoService::resposta).toList();
    }

    private Specification<Transacao> filtro(UsuarioPrincipal usuario, Long produtorId, Long produtoId,
                                            StatusRequisicao status, LocalDate de, LocalDate ate) {
        return (root, query, cb) -> {
            Join<Transacao, RequisicaoVenda> r = root.join("requisicao");
            List<Predicate> ps = new ArrayList<>();
            Long escopo = usuario.isCerealista() ? produtorId : usuario.getId();
            if (escopo != null) {
                ps.add(cb.equal(r.get("produtor").get("id"), escopo));
            }
            if (produtoId != null) {
                ps.add(cb.equal(r.get("produto").get("id"), produtoId));
            }
            if (status != null) {
                ps.add(cb.equal(r.get("status"), status));
            }
            if (de != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), Tempo.inicioDoDia(de)));
            }
            if (ate != null) {
                ps.add(cb.lessThan(root.get("criadoEm"), Tempo.fimDoDiaExclusivo(ate)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    public static TransacaoResponse resposta(Transacao t) {
        RequisicaoVenda r = t.getRequisicao();
        List<DescontoAplicadoResponse> descontos = t.getDescontos().stream()
                .map(d -> new DescontoAplicadoResponse(d.getNome(), d.getValorAplicado())).toList();

        Cerealista compradora = r.getAprovadoPor();
        String compradoraNome = compradora == null ? null : compradora.getRazaoSocial();
        String compradoraDocumento = compradora == null ? null : compradora.getCnpj();
        String compradoraEndereco = null;
        String compradoraCidade = null;
        if (compradora != null && compradora.getEndereco() != null) {
            Endereco e = compradora.getEndereco();
            compradoraCidade = e.getCidade();
            compradoraEndereco = Stream.of(e.getLogradouro(), e.getNumero(), e.getBairro(),
                            e.getCidade() != null && e.getUf() != null ? e.getCidade() + "/" + e.getUf() : e.getCidade())
                    .filter(parte -> parte != null && !parte.isBlank())
                    .collect(Collectors.joining(", "));
            if (compradoraEndereco.isBlank()) {
                compradoraEndereco = null;
            }
        }
        return new TransacaoResponse(t.getId(), r.getId(), r.getProdutor().getId(), r.getProdutor().getNome(),
                r.getProdutor().getCpf(), r.getProduto().getId(), r.getProduto().getNome(), r.getQuantidade(),
                r.getPrecoSaca(), t.getValorBruto(), t.getTotalDescontos(), t.getValorLiquido(), r.getStatus(),
                descontos, t.getCriadoEm(), t.getDataProcessamento(), compradoraNome, compradoraDocumento,
                compradoraEndereco, compradoraCidade);
    }
}
