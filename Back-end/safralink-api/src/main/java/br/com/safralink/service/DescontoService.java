package br.com.safralink.service;

import br.com.safralink.repository.DescontoAplicadoRepository;
import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Desconto;
import br.com.safralink.domain.FormaDesconto;
import br.com.safralink.repository.DescontoRepository;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.RequisicaoDtos.DescontoRequest;
import br.com.safralink.web.dto.RequisicaoDtos.DescontoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescontoService {
    private final DescontoRepository repository;
    private final DescontoAplicadoRepository aplicadoRepository;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;

    public DescontoService(DescontoRepository repository, DescontoAplicadoRepository aplicadoRepository,
                           UsuarioService usuarioService, AuditoriaService auditoria) {
        this.repository = repository;
        this.aplicadoRepository = aplicadoRepository;
        this.usuarioService = usuarioService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<DescontoResponse> listar() {
        return repository.findAllByOrderByIdAsc().stream().map(DescontoService::resposta).toList();
    }

    @Transactional
    public DescontoResponse criar(Long cerealistaId, DescontoRequest req) {
        Desconto d = new Desconto();
        aplicar(d, req);
        d = repository.save(d);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.DESCONTO_CRIADO, "descontos", d.getId(),
                d.getNome());
        return resposta(d);
    }

    @Transactional
    public DescontoResponse atualizar(Long cerealistaId, Long id, DescontoRequest req) {
        Desconto d = repository.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Desconto não encontrado."));
        aplicar(d, req);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.DESCONTO_ALTERADO, "descontos", id,
                d.getNome() + (d.isAtivo() ? " (ativo)" : " (inativo)"));
        return resposta(d);
    }

    public static BigDecimal calcular(Desconto d, BigDecimal valorBruto, BigDecimal quantidadeSacas) {
        BigDecimal valor = d.getForma() == FormaDesconto.PERCENTUAL
                ? valorBruto.multiply(d.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : quantidadeSacas.multiply(d.getValor()).setScale(2, RoundingMode.HALF_UP);
        return valor.max(BigDecimal.ZERO);
    }

    private void aplicar(Desconto d, DescontoRequest req) {
        if (req.forma() == FormaDesconto.PERCENTUAL && req.valor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.requisicaoInvalida("O percentual não pode passar de 100%.");
        }
        d.setNome(req.nome().trim());
        d.setCategoria(req.categoria());
        d.setForma(req.forma());
        d.setValor(req.valor());
        d.setAtivo(req.ativo() == null || req.ativo());
    }

    static DescontoResponse resposta(Desconto d) {
        return new DescontoResponse(d.getId(), d.getNome(), d.getCategoria(), d.getForma(), d.getValor(), d.isAtivo());
    }

    @Transactional(readOnly = true)
    public DescontoResponse buscar(Long id) {
        return resposta(repository.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Desconto não encontrado.")));
    }

    @Transactional
    public void excluir(Long cerealistaId, Long id) {
        Desconto d = repository.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Desconto não encontrado."));
        if (aplicadoRepository.existsByDescontoId(id)) {
            throw ApiException.conflito("Este desconto já foi aplicado em transações. Inative-o em vez de excluí-lo.");
        }
        repository.delete(d);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.DESCONTO_EXCLUIDO, "descontos", id,
                d.getNome());
    }
}
