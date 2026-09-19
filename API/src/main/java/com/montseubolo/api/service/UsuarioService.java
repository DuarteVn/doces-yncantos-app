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

    UsuarioResponse buscarPorId(Long id);

    UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request);

    void excluir(Long id);

    Usuario autenticar(String email, String senha, TipoUsuario tipo);
}
