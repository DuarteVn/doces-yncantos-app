package com.montseubolo.app.api.dto;

import com.montseubolo.app.model.TipoUsuario;
import java.util.Set;

public class UsuarioResponse {

    private String id;
    private String nome;
    private String email;
    private String telefone;
    private Set<TipoUsuario> perfis;

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public Set<TipoUsuario> getPerfis() {
        return perfis;
    }
}
