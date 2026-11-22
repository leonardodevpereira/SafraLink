package br.com.safralink.security;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.service.AuditoriaService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class FalhaDeLoginAuditoria {
    private final AuditoriaService auditoria;

    public FalhaDeLoginAuditoria(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @EventListener
    public void aoFalhar(AuthenticationFailureBadCredentialsEvent evento) {
        auditoria.registrarSemUsuario(String.valueOf(evento.getAuthentication().getName()),
                AcaoAuditoria.LOGIN_FALHA, "Tentativa de autenticação recusada");
    }
}
