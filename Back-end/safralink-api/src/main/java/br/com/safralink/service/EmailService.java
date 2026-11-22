package br.com.safralink.service;

import br.com.safralink.domain.Usuario;

public interface EmailService {
    void enviarRecuperacaoSenha(Usuario destino, String link);
}
