package com.montseubolo.api.service;

import java.util.Set;

import com.montseubolo.api.dto.UsuarioCadastroRequest;
import com.montseubolo.api.dto.UsuarioResponse;
import com.montseubolo.api.exception.RegraDeNegocioException;
import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.model.Usuario;
import com.montseubolo.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso, aplicando hash na senha e atribuindo perfil CLIENTE por padrão")
    void deveCadastrarClienteComSucesso() {
        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "Maria Doces",
                "maria@email.com",
                "11987654321",
                "senha123",
                null
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("hash_bcrypt_seguro");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse response = usuarioService.cadastrar(request);

        assertNotNull(response);
        assertEquals("Maria Doces", response.nome());
        assertEquals("maria@email.com", response.email());
        assertEquals("11987654321", response.telefone());
        assertEquals(Set.of(TipoUsuario.CLIENTE), response.perfis());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertEquals("hash_bcrypt_seguro", salvo.getSenha());
        assertEquals("11987654321", salvo.getTelefone());
        assertTrue(salvo.getPerfis().contains(TipoUsuario.CLIENTE));
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException com status 409 quando o e-mail já existir")
    void deveLancarExcecaoQuandoEmailJaExistir() {
        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "Maria Doces",
                "maria@email.com",
                "11987654321",
                "senha123",
                null
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> usuarioService.cadastrar(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Já existe um usuário cadastrado com este e-mail", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
