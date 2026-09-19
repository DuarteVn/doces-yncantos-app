package com.montseubolo.api.service;


import java.util.List;

import com.montseubolo.api.dto.UsuarioAtualizacaoRequest;
import com.montseubolo.api.dto.UsuarioCadastroRequest;
import com.montseubolo.api.dto.UsuarioResponse;
import com.montseubolo.api.exception.RegraDeNegocioException;
import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.model.Usuario;
import com.montseubolo.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RegraDeNegocioException(HttpStatus.CONFLICT, "Já existe um usuário cadastrado com este e-mail");
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                request.perfil(),
                request.telefone()
        );

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Override
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    @Override
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.de(buscarEntidadePorId(id));
    }

    @Override
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
        Usuario usuario = buscarEntidadePorId(id);
        usuario.setNome(request.nome());
        usuario.setPerfil(request.perfil());
        usuario.setTelefone(request.telefone());
        usuario.setAtivo(request.ativo());
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Override
    public void excluir(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RegraDeNegocioException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public Usuario autenticar(String email, String senha, TipoUsuario perfil) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException(HttpStatus.UNAUTHORIZED, "E-mail, senha ou perfil inválidos"));

        boolean senhaValida = passwordEncoder.matches(senha, usuario.getSenhaHash());
        boolean possuiPerfil = usuario.getPerfil() == perfil;
        boolean ativo = usuario.isAtivo();

        if (!senhaValida || !possuiPerfil || !ativo) {
            throw new RegraDeNegocioException(HttpStatus.UNAUTHORIZED, "E-mail, senha ou perfil inválidos ou inativos");
        }

        return usuario;
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
