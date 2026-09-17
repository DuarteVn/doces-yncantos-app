package com.montseubolo.api.service;

import java.util.List;

import com.montseubolo.api.dto.UsuarioAtualizacaoRequest;
import com.montseubolo.api.dto.UsuarioCadastroRequest;
import com.montseubolo.api.dto.UsuarioResponse;
import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.model.Usuario;

public interface UsuarioService {

    UsuarioResponse cadastrar(UsuarioCadastroRequest request);

    List<UsuarioResponse> listarTodos();

    UsuarioResponse buscarPorId(String id);

    UsuarioResponse atualizar(String id, UsuarioAtualizacaoRequest request);

    void excluir(String id);

    Usuario autenticar(String email, String senha, TipoUsuario tipo);
}
