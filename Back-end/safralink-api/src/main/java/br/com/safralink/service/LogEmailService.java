package br.com.safralink.service;

import br.com.safralink.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(LogEmailService.class);

    @Override
    public void enviarRecuperacaoSenha(Usuario destino, String link) {
        log.info("[E-MAIL SIMULADO] Recuperação de senha para {}: {} (válido por 1 hora)", destino.getEmail(), link);
    }
}
