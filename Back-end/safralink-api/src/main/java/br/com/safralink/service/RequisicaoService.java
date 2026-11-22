package br.com.safralink.service;

import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoAtualizacaoRequest;
import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.ContraProposta;
import br.com.safralink.domain.Perfil;
import br.com.safralink.domain.Produto;
import br.com.safralink.domain.Produtor;
import br.com.safralink.domain.QuantidadeDisponivel;
import br.com.safralink.domain.RequisicaoVenda;
import br.com.safralink.domain.Saldo;
import br.com.safralink.domain.StatusContraProposta;
import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.domain.TipoNotificacao;
import br.com.safralink.domain.Transacao;
import br.com.safralink.domain.Usuario;
import br.com.safralink.repository.CerealistaRepository;
import br.com.safralink.repository.ContraPropostaRepository;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.repository.QuantidadeDisponivelRepository;
import br.com.safralink.repository.RequisicaoVendaRepository;
import br.com.safralink.repository.SaldoRepository;
import br.com.safralink.repository.TransacaoRepository;
import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import br.com.safralink.web.dto.RequisicaoDtos.ContraPropostaRequest;
import br.com.safralink.web.dto.RequisicaoDtos.ContraPropostaResponse;
import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoRequest;
import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoResponse;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequisicaoService {
    private final RequisicaoVendaRepository repository;
    private final ContraPropostaRepository contraPropostaRepository;
    private final ProdutorRepository produtorRepository;
    private final CerealistaRepository cerealistaRepository;
    private final SaldoRepository saldoRepository;
    private final QuantidadeDisponivelRepository quantidadeRepository;
    private final TransacaoRepository transacaoRepository;
    private final ProdutoService produtoService;
    private final SaldoService saldoService;
    private final TransacaoService transacaoService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;
    private final NotificacaoService notificacoes;

    public RequisicaoService(RequisicaoVendaRepository repository, ContraPropostaRepository contraPropostaRepository,
                             ProdutorRepository produtorRepository, CerealistaRepository cerealistaRepository,
                             SaldoRepository saldoRepository, QuantidadeDisponivelRepository quantidadeRepository,
                             TransacaoRepository transacaoRepository, ProdutoService produtoService,
                             SaldoService saldoService, TransacaoService transacaoService,
                             UsuarioService usuarioService, AuditoriaService auditoria,
                             NotificacaoService notificacoes) {
        this.repository = repository;
        this.contraPropostaRepository = contraPropostaRepository;
        this.produtorRepository = produtorRepository;
        this.cerealistaRepository = cerealistaRepository;
        this.saldoRepository = saldoRepository;
        this.quantidadeRepository = quantidadeRepository;
        this.transacaoRepository = transacaoRepository;
        this.produtoService = produtoService;
        this.saldoService = saldoService;
        this.transacaoService = transacaoService;
        this.usuarioService = usuarioService;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
    }

    @Transactional(readOnly = true)
    public PageResponse<RequisicaoResponse> listar(UsuarioPrincipal usuario, StatusRequisicao status, Long produtorId,
                                                   int pagina, int tamanho) {
        Specification<RequisicaoVenda> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Long escopo = usuario.isCerealista() ? produtorId : usuario.getId();
            if (escopo != null) {
                ps.add(cb.equal(root.get("produtor").get("id"), escopo));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        var page = repository.findAll(spec, PageRequest.of(Math.max(pagina, 0), AuditoriaService.limitar(tamanho),
                Sort.by(Sort.Direction.DESC, "criadoEm", "id")));
        return PageResponse.de(page.map(this::resposta));
    }

    @Transactional(readOnly = true)
    public RequisicaoResponse buscar(UsuarioPrincipal usuario, Long id) {
        return resposta(carregarComAcesso(usuario, id));
    }

    @Transactional
    public RequisicaoResponse criar(UsuarioPrincipal usuario, RequisicaoRequest req) {
        Produtor produtor = produtorRepository.findById(usuario.getId())
                .orElseThrow(() -> ApiException.proibido("Apenas produtores solicitam vendas."));
        Produto produto = produtoService.buscarAtivo(req.produtoId());
        BigDecimal preco = req.precoSaca() != null ? req.precoSaca()
                : produtoService.precoAtual(produto.getId()).orElseThrow(() ->
                        ApiException.regraDeNegocio("Ainda não há preço definido para este produto."));
        garantirSaldo(produtor.getId(), produto.getId(), req.quantidade());

        RequisicaoVenda r = new RequisicaoVenda();
        r.setProdutor(produtor);
        r.setProduto(produto);
        r.setQuantidade(req.quantidade());
        r.setPrecoSaca(preco);
        r.setObservacao(Mapeador.limpar(req.observacao()));
        r = repository.save(r);

        auditoria.registrar(produtor, AcaoAuditoria.REQUISICAO_CRIADA, "requisicoes_venda", r.getId(),
                resumo(r));
        notificacoes.notificarCerealistas(TipoNotificacao.REQUISICAO_CRIADA,
                produtor.getNome() + " solicitou a venda de " + resumo(r));
        return resposta(r);
    }

    @Transactional
    public RequisicaoResponse reenviar(UsuarioPrincipal usuario, Long id) {
        RequisicaoVenda r = bloquearComAcesso(usuario, id);
        if (usuario.isCerealista()) {
            throw ApiException.proibido("Apenas o produtor reenvia a própria requisição.");
        }
        exigirStatus(r, StatusRequisicao.RECUSADA, "Só requisições recusadas podem ser reenviadas.");
        garantirSaldo(r.getProdutor().getId(), r.getProduto().getId(), r.getQuantidade());

        r.setStatus(StatusRequisicao.PENDENTE);
        r.setMotivoRecusa(null);
        r.setAprovadoPor(null);
        auditoria.registrar(r.getProdutor(), AcaoAuditoria.REQUISICAO_REENVIADA, "requisicoes_venda", id, resumo(r));
        notificacoes.notificarCerealistas(TipoNotificacao.REQUISICAO_CRIADA,
                r.getProdutor().getNome() + " reenviou a requisição de " + resumo(r));
        return resposta(r);
    }

    @Transactional
    public RequisicaoResponse atualizar(UsuarioPrincipal usuario, Long id, RequisicaoAtualizacaoRequest req) {
        RequisicaoVenda r = bloquearComAcesso(usuario, id);
        if (usuario.isCerealista()) {
            throw ApiException.proibido("Apenas o produtor edita a própria requisição.");
        }
        exigirStatus(r, StatusRequisicao.PENDENTE, "Só requisições pendentes podem ser editadas.");
        BigDecimal disponivel = saldoService.disponivelParaVenda(r.getProdutor().getId(), r.getProduto().getId())
                .add(r.getQuantidade());
        if (req.quantidade().compareTo(disponivel) > 0) {
            throw ApiException.regraDeNegocio("Quantidade acima do saldo disponível para venda (" + disponivel + " sc).");
        }
        r.setQuantidade(req.quantidade());
        if (req.precoSaca() != null) {
            r.setPrecoSaca(req.precoSaca());
        }
        r.setObservacao(Mapeador.limpar(req.observacao()));
        cancelarContraPropostasPendentes(id);
        auditoria.registrar(r.getProdutor(), AcaoAuditoria.REQUISICAO_ALTERADA, "requisicoes_venda", id, resumo(r));
        notificacoes.notificarCerealistas(TipoNotificacao.REQUISICAO_CRIADA,
                r.getProdutor().getNome() + " alterou a requisição para " + resumo(r));
        return resposta(r);
    }

    @Transactional
    public void excluir(UsuarioPrincipal usuario, Long id) {
        RequisicaoVenda r = bloquearComAcesso(usuario, id);
        if (usuario.isCerealista()) {
            throw ApiException.proibido("Apenas o produtor exclui a própria requisição.");
        }
        if (r.getStatus() != StatusRequisicao.PENDENTE && r.getStatus() != StatusRequisicao.RECUSADA) {
            throw ApiException.conflito("Requisições aprovadas ou processadas não podem ser excluídas.");
        }
        String resumo = resumo(r);
        Produtor produtor = r.getProdutor();
        repository.delete(r);
        auditoria.registrar(produtor, AcaoAuditoria.REQUISICAO_EXCLUIDA, "requisicoes_venda", id, resumo);
    }

    @Transactional
    public RequisicaoResponse aprovar(UsuarioPrincipal usuario, Long id) {
        Cerealista cerealista = exigirCerealista(usuario);
        RequisicaoVenda r = bloquear(id);
        exigirStatus(r, StatusRequisicao.PENDENTE, "Só requisições pendentes podem ser aprovadas.");

        Long produtorId = r.getProdutor().getId();
        Long produtoId = r.getProduto().getId();
        QuantidadeDisponivel disponibilidade = quantidadeRepository.buscarParaAtualizar(produtoId)
                .orElseThrow(() -> ApiException.regraDeNegocio(
                        "A cerealista ainda não definiu a quantidade disponível para este produto."));
        if (r.getQuantidade().compareTo(disponibilidade.getQuantidade()) > 0) {
            throw ApiException.regraDeNegocio("Quantidade acima da disponibilidade de compra ("
                    + disponibilidade.getQuantidade() + " sc). Ajuste a quantidade disponível ou recuse.");
        }
        Saldo saldo = saldoRepository.buscarParaAtualizar(produtorId, produtoId)
                .orElseThrow(() -> ApiException.regraDeNegocio("O produtor não tem saldo neste produto."));
        if (saldo.getQuantidade().compareTo(r.getQuantidade()) < 0) {
            throw ApiException.regraDeNegocio("Saldo do produtor insuficiente (" + saldo.getQuantidade() + " sc).");
        }

        saldo.debitar(r.getQuantidade());
        disponibilidade.setQuantidade(disponibilidade.getQuantidade().subtract(r.getQuantidade()));
        r.setStatus(StatusRequisicao.APROVADA);
        r.setAprovadoPor(cerealista);
        r.setMotivoRecusa(null);
        cancelarContraPropostasPendentes(id);
        Transacao transacao = transacaoService.gerar(r);

        auditoria.registrar(cerealista, AcaoAuditoria.APROVACAO, "requisicoes_venda", id,
                resumo(r) + " | líquido " + PrecoService.moeda(transacao.getValorLiquido()));
        notificacoes.notificar(r.getProdutor(), TipoNotificacao.REQUISICAO_APROVADA,
                "Sua venda de " + resumo(r) + " foi aprovada. Valor líquido: "
                        + PrecoService.moeda(transacao.getValorLiquido()));
        return resposta(r);
    }

    @Transactional
    public RequisicaoResponse recusar(UsuarioPrincipal usuario, Long id, String motivo) {
        Cerealista cerealista = exigirCerealista(usuario);
        RequisicaoVenda r = bloquear(id);
        exigirStatus(r, StatusRequisicao.PENDENTE, "Só requisições pendentes podem ser recusadas.");
        r.setStatus(StatusRequisicao.RECUSADA);
        r.setMotivoRecusa(motivo.trim());
        r.setAprovadoPor(cerealista);
        cancelarContraPropostasPendentes(id);

        auditoria.registrar(cerealista, AcaoAuditoria.RECUSA, "requisicoes_venda", id,
                resumo(r) + " | motivo: " + motivo.trim());
        notificacoes.notificar(r.getProdutor(), TipoNotificacao.REQUISICAO_RECUSADA,
                "Sua venda de " + resumo(r) + " foi recusada: " + motivo.trim());
        return resposta(r);
    }

    @Transactional
    public RequisicaoResponse processar(UsuarioPrincipal usuario, Long id) {
        Cerealista cerealista = exigirCerealista(usuario);
        RequisicaoVenda r = bloquear(id);
        exigirStatus(r, StatusRequisicao.APROVADA, "Só requisições aprovadas podem ser processadas.");
        Transacao transacao = transacaoRepository.findByRequisicaoId(id)
                .orElseThrow(() -> ApiException.conflito("Requisição aprovada sem transação associada."));
        transacao.setDataProcessamento(Instant.now());
        r.setStatus(StatusRequisicao.PROCESSADA);

        auditoria.registrar(cerealista, AcaoAuditoria.PROCESSAMENTO, "requisicoes_venda", id, resumo(r));
        notificacoes.notificar(r.getProdutor(), TipoNotificacao.REQUISICAO_PROCESSADA,
                "Sua venda de " + resumo(r) + " foi processada.");
        return resposta(r);
    }

    @Transactional
    public RequisicaoResponse enviarContraProposta(UsuarioPrincipal usuario, Long requisicaoId,
                                                   ContraPropostaRequest req) {
        RequisicaoVenda r = bloquearComAcesso(usuario, requisicaoId);
        exigirStatus(r, StatusRequisicao.PENDENTE, "Só é possível negociar requisições pendentes.");
        Usuario autor = usuarioService.buscar(usuario.getId());

        cancelarContraPropostasPendentes(requisicaoId);
        ContraProposta cp = new ContraProposta();
        cp.setRequisicao(r);
        cp.setAutor(autor);
        cp.setPrecoSaca(req.precoSaca());
        contraPropostaRepository.save(cp);

        auditoria.registrar(autor, AcaoAuditoria.CONTRA_PROPOSTA_ENVIADA, "requisicoes_venda", requisicaoId,
                "Preço proposto " + PrecoService.moeda(req.precoSaca()) + "/sc");
        notificarOutraParte(usuario, r, TipoNotificacao.CONTRA_PROPOSTA,
                autor.getNome() + " propôs " + PrecoService.moeda(req.precoSaca()) + "/sc para " + resumo(r));
        return resposta(r);
    }

    @Transactional
    public RequisicaoResponse responderContraProposta(UsuarioPrincipal usuario, Long contraPropostaId,
                                                      boolean aceitar) {
        ContraProposta cp = contraPropostaRepository.findById(contraPropostaId)
                .orElseThrow(() -> ApiException.naoEncontrado("Contra-proposta não encontrada."));
        RequisicaoVenda r = bloquearComAcesso(usuario, cp.getRequisicao().getId());
        exigirStatus(r, StatusRequisicao.PENDENTE, "Só é possível negociar requisições pendentes.");
        if (cp.getStatus() != StatusContraProposta.PENDENTE) {
            throw ApiException.conflito("Esta contra-proposta já foi respondida.");
        }
        if (cp.getAutor().getId().equals(usuario.getId())) {
            throw ApiException.proibido("Você não pode responder à sua própria contra-proposta.");
        }
        Usuario quemResponde = usuarioService.buscar(usuario.getId());
        cp.setStatus(aceitar ? StatusContraProposta.ACEITA : StatusContraProposta.RECUSADA);
        cp.setRespondidaEm(Instant.now());
        if (aceitar) {
            r.setPrecoSaca(cp.getPrecoSaca());
        }
        auditoria.registrar(quemResponde,
                aceitar ? AcaoAuditoria.CONTRA_PROPOSTA_ACEITA : AcaoAuditoria.CONTRA_PROPOSTA_RECUSADA,
                "requisicoes_venda", r.getId(), "Preço " + PrecoService.moeda(cp.getPrecoSaca()) + "/sc");
        notificacoes.notificar(cp.getAutor(), TipoNotificacao.CONTRA_PROPOSTA,
                quemResponde.getNome() + (aceitar ? " aceitou" : " recusou") + " sua proposta de "
                        + PrecoService.moeda(cp.getPrecoSaca()) + "/sc para " + resumo(r));
        return resposta(r);
    }

    public RequisicaoResponse resposta(RequisicaoVenda r) {
        List<ContraPropostaResponse> contraPropostas = contraPropostaRepository
                .findByRequisicaoIdOrderByCriadoEmAscIdAsc(r.getId()).stream().map(this::resposta).toList();
        var transacao = transacaoRepository.findByRequisicaoId(r.getId()).map(TransacaoService::resposta).orElse(null);
        return new RequisicaoResponse(r.getId(), r.getProdutor().getId(), r.getProdutor().getNome(),
                r.getProduto().getId(), r.getProduto().getNome(), r.getQuantidade(), r.getPrecoSaca(), r.valorBruto(),
                r.getStatus(), r.getMotivoRecusa(), r.getObservacao(),
                r.getAprovadoPor() == null ? null : r.getAprovadoPor().getNome(), r.getCriadoEm(),
                r.getAtualizadoEm(), contraPropostas, transacao);
    }

    private ContraPropostaResponse resposta(ContraProposta c) {
        return new ContraPropostaResponse(c.getId(), c.getAutor().getId(), c.getAutor().getNome(),
                c.getAutor().getPerfil(), c.getPrecoSaca(), c.getStatus(), c.getCriadoEm(), c.getRespondidaEm());
    }

    private void garantirSaldo(Long produtorId, Long produtoId, BigDecimal quantidade) {
        BigDecimal disponivel = saldoService.disponivelParaVenda(produtorId, produtoId);
        if (quantidade.compareTo(disponivel) > 0) {
            throw ApiException.regraDeNegocio("Quantidade acima do saldo disponível para venda (" + disponivel
                    + " sc, já descontadas as requisições pendentes).");
        }
    }

    private void cancelarContraPropostasPendentes(Long requisicaoId) {
        contraPropostaRepository.findByRequisicaoIdAndStatus(requisicaoId, StatusContraProposta.PENDENTE)
                .forEach(c -> {
                    c.setStatus(StatusContraProposta.CANCELADA);
                    c.setRespondidaEm(Instant.now());
                });
    }

    private void notificarOutraParte(UsuarioPrincipal autor, RequisicaoVenda r, TipoNotificacao tipo, String msg) {
        if (autor.getPerfil() == Perfil.CEREALISTA) {
            notificacoes.notificar(r.getProdutor(), tipo, msg);
        } else {
            notificacoes.notificarCerealistas(tipo, msg);
        }
    }

    private Cerealista exigirCerealista(UsuarioPrincipal usuario) {
        return cerealistaRepository.findById(usuario.getId())
                .orElseThrow(() -> ApiException.proibido("Apenas a cerealista pode executar esta operação."));
    }

    private RequisicaoVenda carregarComAcesso(UsuarioPrincipal usuario, Long id) {
        RequisicaoVenda r = repository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Requisição não encontrada."));
        verificarAcesso(usuario, r);
        return r;
    }

    private RequisicaoVenda bloquearComAcesso(UsuarioPrincipal usuario, Long id) {
        RequisicaoVenda r = bloquear(id);
        verificarAcesso(usuario, r);
        return r;
    }

    private RequisicaoVenda bloquear(Long id) {
        return repository.buscarParaAtualizar(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Requisição não encontrada."));
    }

    private void verificarAcesso(UsuarioPrincipal usuario, RequisicaoVenda r) {
        if (!usuario.isCerealista() && !r.getProdutor().getId().equals(usuario.getId())) {
            throw ApiException.naoEncontrado("Requisição não encontrada.");
        }
    }

    private void exigirStatus(RequisicaoVenda r, StatusRequisicao esperado, String mensagem) {
        if (r.getStatus() != esperado) {
            throw ApiException.conflito(mensagem + " Situação atual: " + r.getStatus() + ".");
        }
    }

    private String resumo(RequisicaoVenda r) {
        return r.getQuantidade() + " sc de " + r.getProduto().getNome() + " a " + PrecoService.moeda(r.getPrecoSaca())
                + "/sc";
    }
}
