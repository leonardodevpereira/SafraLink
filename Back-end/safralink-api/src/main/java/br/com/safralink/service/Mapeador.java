package br.com.safralink.service;

import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.Endereco;
import br.com.safralink.domain.Usuario;
import br.com.safralink.web.dto.UsuarioDtos.EnderecoDto;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import org.hibernate.Hibernate;

public final class Mapeador {
    private Mapeador() {}

    public static EnderecoDto endereco(Endereco e) {
        if (e == null) {
            return null;
        }
        return new EnderecoDto(e.getLogradouro(), e.getNumero(), e.getBairro(), e.getCidade(), e.getUf(), e.getCep());
    }

    public static void aplicarEndereco(Endereco destino, EnderecoDto origem) {
        if (origem == null) {
            return;
        }
        destino.setLogradouro(limpar(origem.logradouro()));
        destino.setNumero(limpar(origem.numero()));
        destino.setBairro(limpar(origem.bairro()));
        destino.setCidade(limpar(origem.cidade()));
        destino.setUf(origem.uf() == null || origem.uf().isBlank() ? null : origem.uf().trim().toUpperCase());
        destino.setCep(limpar(origem.cep()));
    }

    public static UsuarioResponse usuario(Usuario usuario) {
        Usuario u = (Usuario) Hibernate.unproxy(usuario);
        String razao = u instanceof Cerealista c ? c.getRazaoSocial() : null;
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil(),
                u.getDocumento(), razao, endereco(u.getEndereco()), u.getCriadoEm());
    }

    public static String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
