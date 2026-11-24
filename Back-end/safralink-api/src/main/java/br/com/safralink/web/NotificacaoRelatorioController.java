package br.com.safralink.web;

import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.service.NotificacaoService;
import br.com.safralink.service.RelatorioService;
import br.com.safralink.web.dto.OutrosDtos.NotificacaoResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NotificacaoRelatorioController {
    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

    private final NotificacaoService notificacoes;
    private final RelatorioService relatorios;

    public NotificacaoRelatorioController(NotificacaoService notificacoes, RelatorioService relatorios) {
        this.notificacoes = notificacoes;
        this.relatorios = relatorios;
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoResponse> recentes(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return notificacoes.recentes(usuario.getId());
    }

    @GetMapping("/notificacoes/nao-lidas")
    public List<NotificacaoResponse> naoLidas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return notificacoes.naoLidas(usuario.getId());
    }

    @GetMapping("/notificacoes/{id}")
    public NotificacaoResponse buscarNotificacao(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                 @PathVariable Long id) {
        return notificacoes.buscar(usuario.getId(), id);
    }

    @DeleteMapping("/notificacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirNotificacao(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        notificacoes.excluir(usuario.getId(), id);
    }

    @PostMapping("/notificacoes/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLida(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        notificacoes.marcarComoLida(usuario.getId(), id);
    }

    @PostMapping("/notificacoes/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLidas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        notificacoes.marcarTodasComoLidas(usuario.getId());
    }

    @DeleteMapping("/notificacoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void limparNotificacoes(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        notificacoes.limparTodas(usuario.getId());
    }

    @GetMapping("/relatorios/transacoes.csv")
    public ResponseEntity<byte[]> transacoes(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return csv("safralink-transacoes.csv", relatorios.transacoes(usuario, de, ate));
    }

    @GetMapping("/relatorios/produtores.csv")
    @PreAuthorize("hasRole('CEREALISTA')")
    public ResponseEntity<byte[]> produtores() {
        return csv("safralink-produtores.csv", relatorios.produtores());
    }

    private static ResponseEntity<byte[]> csv(String arquivo, byte[] conteudo) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo).build().toString())
                .body(conteudo);
    }
}
