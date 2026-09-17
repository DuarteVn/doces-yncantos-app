package com.montseubolo.app.api.dto;

import com.montseubolo.app.model.TipoUsuario;

public class LoginResponse {

    private String token;
    private String id;
    private String nome;
    private TipoUsuario perfil;

    public String getToken() {
        return token;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoUsuario getPerfil() {
        return perfil;
    }
}
