package com.montseubolo.api.controller;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montseubolo.api.dto.UsuarioCadastroRequest;
import com.montseubolo.api.dto.UsuarioResponse;
import com.montseubolo.api.exception.GlobalExceptionHandler;
import com.montseubolo.api.exception.RegraDeNegocioException;
import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.security.JwtService;
import com.montseubolo.api.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/auth/cadastro - Deve retornar 201 Created quando os dados forem válidos")
    void deveRetornar201QuandoCadastroValido() throws Exception {
        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "Yane Confeiteira",
                "yane@docesyncantos.com",
                "11987654321",
                "senha123",
                null
        );

        UsuarioResponse response = new UsuarioResponse(
                "uuid-123",
                "Yane Confeiteira",
                "yane@docesyncantos.com",
                "11987654321",
                Set.of(TipoUsuario.CLIENTE)
        );

        when(usuarioService.cadastrar(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("uuid-123"))
                .andExpect(jsonPath("$.nome").value("Yane Confeiteira"))
                .andExpect(jsonPath("$.email").value("yane@docesyncantos.com"))
                .andExpect(jsonPath("$.telefone").value("11987654321"))
                .andExpect(jsonPath("$.perfis[0]").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/auth/cadastro - Deve retornar 400 Bad Request quando campos obrigatórios estiverem ausentes")
    void deveRetornar400QuandoCamposInvalidos() throws Exception {
        UsuarioCadastroRequest requestInvalido = new UsuarioCadastroRequest(
                "",
                "email-invalido",
                "123", // menor que 11 dígitos
                "123", // menor que 6 caracteres
                null
        );

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("POST /api/auth/cadastro - Deve retornar 409 Conflict quando o e-mail já estiver cadastrado")
    void deveRetornar409QuandoEmailDuplicado() throws Exception {
        UsuarioCadastroRequest request = new UsuarioCadastroRequest(
                "Yane Confeiteira",
                "duplicado@docesyncantos.com",
                "11987654321",
                "senha123",
                null
        );

        when(usuarioService.cadastrar(any()))
                .thenThrow(new RegraDeNegocioException(HttpStatus.CONFLICT, "Já existe um usuário cadastrado com este e-mail"));

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Já existe um usuário cadastrado com este e-mail"));
    }
}
