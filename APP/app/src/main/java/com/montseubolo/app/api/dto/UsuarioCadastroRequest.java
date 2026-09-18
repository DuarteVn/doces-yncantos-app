package com.montseubolo.app.api.dto;

public class UsuarioCadastroRequest {

    private final String nome;
    private final String email;
    private final String telefone;
    private final String senha;

    public UsuarioCadastroRequest(String nome, String email, String telefone, String senha) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.senha = senha;
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

    public String getSenha() {
        return senha;
    }
}
