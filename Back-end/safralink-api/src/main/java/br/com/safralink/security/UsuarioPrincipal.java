package br.com.safralink.security;

import br.com.safralink.domain.Perfil;
import br.com.safralink.domain.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioPrincipal implements UserDetails {
    private final Long id;
    private final String email;
    private final Perfil perfil;
    private final String senhaHash;
    private final boolean ativo;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.perfil = usuario.getPerfil();
        this.senhaHash = usuario.getSenhaHash();
        this.ativo = usuario.isAtivo();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isCerealista() {
        return perfil == Perfil.CEREALISTA;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
