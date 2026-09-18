package com.montseubolo.app.api;

import com.montseubolo.app.api.dto.LoginRequest;
import com.montseubolo.app.api.dto.LoginResponse;
import com.montseubolo.app.api.dto.UsuarioCadastroRequest;
import com.montseubolo.app.api.dto.UsuarioResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/cadastro")
    Call<UsuarioResponse> cadastrar(@Body UsuarioCadastroRequest request);
}
