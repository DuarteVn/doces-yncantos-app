package com.montseubolo.api.dto;

import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, TipoUsuario perfil, String telefone, boolean ativo) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), usuario.getTelefone(), usuario.isAtivo());
    }
}
