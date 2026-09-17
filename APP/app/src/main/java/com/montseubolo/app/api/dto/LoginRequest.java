package com.montseubolo.app.api.dto;

import com.montseubolo.app.model.TipoUsuario;

public class LoginRequest {

    private final String email;
    private final String senha;
    private final TipoUsuario perfil;

    public LoginRequest(String email, String senha, TipoUsuario perfil) {
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
    }
}
