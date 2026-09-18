package com.montseubolo.api.dto;

import java.util.Set;

import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.model.Usuario;

public record UsuarioResponse(String id, String nome, String email, String telefone, Set<TipoUsuario> perfis) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTelefone(), usuario.getPerfis());
    }
}
