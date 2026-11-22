package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Auditoria;
import br.com.safralink.domain.Usuario;
import br.com.safralink.repository.AuditoriaRepository;
import br.com.safralink.util.Tempo;
import br.com.safralink.web.dto.OutrosDtos.AuditoriaResponse;
import br.com.safralink.web.dto.OutrosDtos.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(Usuario autor, AcaoAuditoria acao, String entidade, Long entidadeId, String detalhes) {
        Auditoria a = new Auditoria();
        if (autor != null) {
            a.setUsuarioId(autor.getId());
            a.setUsuarioNome(autor.getNome());
            a.setUsuarioEmail(autor.getEmail());
        }
        a.setAcao(acao);
        a.setEntidade(entidade);
        a.setEntidadeId(entidadeId);
        a.setDetalhes(cortar(detalhes));
        repository.save(a);
    }

    @Transactional
    public void registrarSemUsuario(String email, AcaoAuditoria acao, String detalhes) {
        Auditoria a = new Auditoria();
        a.setUsuarioEmail(email == null ? null : cortar(email, 180));
        a.setAcao(acao);
        a.setDetalhes(cortar(detalhes));
        repository.save(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaResponse> listar(AcaoAuditoria acao, LocalDate de, LocalDate ate, int pagina,
                                                  int tamanho) {
        Specification<Auditoria> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (acao != null) {
                ps.add(cb.equal(root.get("acao"), acao));
            }
            if (de != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), Tempo.inicioDoDia(de)));
            }
            if (ate != null) {
                ps.add(cb.lessThan(root.get("criadoEm"), Tempo.fimDoDiaExclusivo(ate)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        var page = repository.findAll(spec, PageRequest.of(Math.max(pagina, 0), limitar(tamanho),
                        Sort.by(Sort.Direction.DESC, "criadoEm", "id")))
                .map(a -> new AuditoriaResponse(a.getId(), a.getUsuarioNome(), a.getUsuarioEmail(), a.getAcao(),
                        a.getEntidade(), a.getEntidadeId(), a.getDetalhes(), a.getCriadoEm()));
        return PageResponse.de(page);
    }

    static int limitar(int tamanho) {
        return Math.min(Math.max(tamanho, 1), 100);
    }

    private static String cortar(String valor) {
        return cortar(valor, 500);
    }

    private static String cortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
