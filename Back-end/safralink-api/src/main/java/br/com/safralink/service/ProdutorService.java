package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Produtor;
import br.com.safralink.domain.StatusRequisicao;
import br.com.safralink.repository.RequisicaoVendaRepository;
import br.com.safralink.repository.UsuarioRepository;
import br.com.safralink.web.dto.AuthDtos.RegistroRequest;
import br.com.safralink.web.dto.UsuarioDtos.PerfilRequest;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.repository.SaldoRepository;
import br.com.safralink.repository.TransacaoRepository;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.CatalogoDtos.SaldoResponse;
import br.com.safralink.web.dto.OutrosDtos.ProdutorResumoResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutorService {
    private final ProdutorRepository produtorRepository;
    private final SaldoRepository saldoRepository;
    private final TransacaoRepository transacaoRepository;
    private final SaldoService saldoService;
    private final UsuarioRepository usuarioRepository;
    private final RequisicaoVendaRepository requisicaoRepository;
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final AuditoriaService auditoria;

    public ProdutorService(ProdutorRepository produtorRepository, SaldoRepository saldoRepository,
                           TransacaoRepository transacaoRepository, SaldoService saldoService,
                           UsuarioRepository usuarioRepository, RequisicaoVendaRepository requisicaoRepository,
                           UsuarioService usuarioService, AuthService authService, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.usuarioService = usuarioService;
        this.authService = authService;
        this.auditoria = auditoria;
        this.produtorRepository = produtorRepository;
        this.saldoRepository = saldoRepository;
        this.transacaoRepository = transacaoRepository;
        this.saldoService = saldoService;
    }

    @Transactional(readOnly = true)
    public List<ProdutorResumoResponse> listar(String busca) {
        Map<Long, BigDecimal> saldos = new HashMap<>();
        saldoRepository.totaisPorProdutor().forEach(l -> saldos.put((Long) l[0], (BigDecimal) l[1]));
        Map<Long, Instant> ultimas = new HashMap<>();
        transacaoRepository.ultimaTransacaoPorProdutor().forEach(l -> ultimas.put((Long) l[0], (Instant) l[1]));

        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        return produtorRepository.findAll(Sort.by("nome")).stream()
                .filter(p -> termo.isEmpty() || contem(p, termo))
                .map(p -> resumo(p, saldos.getOrDefault(p.getId(), BigDecimal.ZERO), ultimas.get(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutorResumoResponse buscar(Long id) {
        Produtor p = carregar(id);
        Instant ultima = null;
        for (Object[] l : transacaoRepository.ultimaTransacaoPorProdutor()) {
            if (id.equals(l[0])) {
                ultima = (Instant) l[1];
            }
        }
        return resumo(p, saldoRepository.totalDoProdutor(id), ultima);
    }

    @Transactional
    public UsuarioResponse criar(RegistroRequest req) {
        return authService.registrar(req);
    }

    @Transactional
    public ProdutorResumoResponse atualizar(Long cerealistaId, Long id, PerfilRequest req) {
        Produtor p = carregar(id);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflito("Este e-mail já está em uso por outra conta.");
        }
        p.setNome(req.nome().trim());
        p.setEmail(email);
        p.setTelefone(Mapeador.limpar(req.telefone()));
        Mapeador.aplicarEndereco(p.getEndereco(), req.endereco());
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.PERFIL_ATUALIZADO, "usuarios", id,
                "Dados do produtor " + p.getNome() + " atualizados pela cerealista");
        return buscar(id);
    }

    @Transactional
    public void excluir(Long cerealistaId, Long id) {
        Produtor p = carregar(id);
        if (saldoRepository.totalDoProdutor(id).signum() > 0) {
            throw ApiException.conflito("O produtor ainda tem saldo em sacas e não pode ser excluído.");
        }
        if (requisicaoRepository.existsByProdutorIdAndStatus(id, StatusRequisicao.PENDENTE)) {
            throw ApiException.conflito("O produtor tem requisições pendentes e não pode ser excluído.");
        }
        p.setAtivo(false);
        auditoria.registrar(usuarioService.buscar(cerealistaId), AcaoAuditoria.PRODUTOR_INATIVADO, "usuarios", id,
                p.getNome());
    }

    private Produtor carregar(Long id) {
        return produtorRepository.findById(id).filter(Produtor::isAtivo)
                .orElseThrow(() -> ApiException.naoEncontrado("Produtor não encontrado."));
    }

    private static ProdutorResumoResponse resumo(Produtor p, BigDecimal saldoTotal, Instant ultima) {
        return new ProdutorResumoResponse(p.getId(), p.getNome(), p.getCpf(), p.getEmail(), p.getTelefone(),
                p.getEndereco() == null ? null : p.getEndereco().getCidade(), saldoTotal, ultima, p.isAtivo());
    }

    @Transactional(readOnly = true)
    public List<SaldoResponse> saldosDoProdutor(Long produtorId) {
        if (!produtorRepository.existsById(produtorId)) {
            throw ApiException.naoEncontrado("Produtor não encontrado.");
        }
        return saldoService.listarDoProdutor(produtorId);
    }

    private static boolean contem(Produtor p, String termo) {
        return p.getNome().toLowerCase(Locale.ROOT).contains(termo)
                || p.getEmail().toLowerCase(Locale.ROOT).contains(termo)
                || p.getCpf().contains(termo.replaceAll("\\D", "").isEmpty() ? "\u0000" : termo.replaceAll("\\D", ""));
    }
}
