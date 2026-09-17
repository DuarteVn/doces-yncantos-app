package com.montseubolo.api.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    // Uma conta pode ter mais de um perfil (ex.: a mesma pessoa sendo
    // Confeiteira e Admin), e escolhe qual usar a cada login.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "usuario_perfis",
            joinColumns = @JoinColumn(name = "usuario_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "perfil"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false)
    private Set<TipoUsuario> perfis = new HashSet<>();

    public Usuario() {
    }

    public Usuario(String nome, String email, String senha, Set<TipoUsuario> perfis) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfis = perfis;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Set<TipoUsuario> getPerfis() {
        return perfis;
    }

    public void setPerfis(Set<TipoUsuario> perfis) {
        this.perfis = perfis;
    }
}
