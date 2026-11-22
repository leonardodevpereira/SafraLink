package br.com.safralink.service;

import br.com.safralink.domain.Notificacao;
import br.com.safralink.domain.TipoNotificacao;
import br.com.safralink.domain.Usuario;
import br.com.safralink.repository.CerealistaRepository;
import br.com.safralink.repository.NotificacaoRepository;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.repository.UsuarioRepository;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.OutrosDtos.NotificacaoResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {
    private final NotificacaoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CerealistaRepository cerealistaRepository;
    private final ProdutorRepository produtorRepository;

    public NotificacaoService(NotificacaoRepository repository, UsuarioRepository usuarioRepository,
                              CerealistaRepository cerealistaRepository, ProdutorRepository produtorRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.cerealistaRepository = cerealistaRepository;
        this.produtorRepository = produtorRepository;
    }

    @Transactional
    public void notificar(Usuario destino, TipoNotificacao tipo, String mensagem) {
        repository.save(nova(destino, tipo, mensagem));
    }

    @Transactional
    public void notificarCerealistas(TipoNotificacao tipo, String mensagem) {
        repository.saveAll(cerealistaRepository.findAll().stream()
                .filter(Usuario::isAtivo).map(c -> nova(c, tipo, mensagem)).toList());
    }

    @Transactional
    public void notificarProdutores(TipoNotificacao tipo, String mensagem) {
        repository.saveAll(produtorRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(p -> nova(p, tipo, mensagem)).toList());
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> recentes(Long usuarioId) {
        return repository.findTop50ByUsuarioIdOrderByCriadoEmDescIdDesc(usuarioId).stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> naoLidas(Long usuarioId) {
        return repository.findByUsuarioIdAndLidaFalseOrderByCriadoEmAscIdAsc(usuarioId).stream()
                .map(this::resposta).toList();
    }

    @Transactional
    public void marcarComoLida(Long usuarioId, Long id) {
        Notificacao n = repository.findById(id).filter(x -> x.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> ApiException.naoEncontrado("Notificação não encontrada."));
        n.setLida(true);
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        repository.marcarTodasComoLidas(usuarioId);
    }

    @Transactional
    public void limparTodas(Long usuarioId) {
        repository.apagarTodas(usuarioId);
    }

    private Notificacao nova(Usuario destino, TipoNotificacao tipo, String mensagem) {
        Notificacao n = new Notificacao();
        n.setUsuario(destino);
        n.setTipo(tipo);
        n.setMensagem(mensagem.length() > 255 ? mensagem.substring(0, 255) : mensagem);
        return n;
    }

    private NotificacaoResponse resposta(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTipo(), n.getMensagem(), n.isLida(), n.getCriadoEm());
    }
}
