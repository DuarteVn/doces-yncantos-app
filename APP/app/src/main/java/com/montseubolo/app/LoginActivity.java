package com.montseubolo.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.montseubolo.app.api.ApiClient;
import com.montseubolo.app.api.AuthApiService;
import com.montseubolo.app.api.dto.LoginRequest;
import com.montseubolo.app.api.dto.LoginResponse;
import com.montseubolo.app.model.TipoUsuario;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela de login do app Doces Yncantos.
 *
 * O botão "Entrar" autentica o usuário na API (POST /api/auth/login) usando
 * o e-mail, a senha e o perfil (Cliente/Confeiteira/Admin) selecionados na
 * tela, e navega para a tela inicial correspondente ao perfil autenticado.
 */
public class LoginActivity extends AppCompatActivity {

    // Views dos 3 botões de tipo de usuário (Cliente, Confeiteira, Admin)
    private TextView btnCliente;
    private TextView btnConfeiteira;
    private TextView btnAdmin;

    // Campos de texto
    private EditText edtEmail;
    private EditText edtSenha;
    private ImageView imgAlternarSenha;

    // Botão de entrar e link de criar conta
    private TextView btnEntrar;
    private TextView txtCriarConta;
    private LinearLayout linhaCriarConta;

    // Guarda qual tipo de usuário está selecionado no momento
    private TipoUsuario tipoUsuarioSelecionado = TipoUsuario.CLIENTE;

    // Tag usada nos Log.d para facilitar filtrar no Logcat
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ligarViews();
        configurarBotoesDeTipo();
        configurarHabilitacaoDoBotaoEntrar();
        configurarBotaoEntrar();
        configurarLinkCriarConta();
        configurarAlternarVisibilidadeSenha();
        atualizarVisibilidadeCriarConta();
    }

    // Liga cada variável Java ao componente correspondente do layout XML
    private void ligarViews() {
        btnCliente = findViewById(R.id.btnCliente);
        btnConfeiteira = findViewById(R.id.btnConfeiteira);
        btnAdmin = findViewById(R.id.btnAdmin);

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        imgAlternarSenha = findViewById(R.id.imgAlternarSenha);

        btnEntrar = findViewById(R.id.btnEntrar);
        txtCriarConta = findViewById(R.id.txtCriarConta);
        linhaCriarConta = findViewById(R.id.linhaCriarConta);
    }

    /**
     * Configura o clique dos 3 botões de tipo de usuário.
     * A regra é simples: só um pode estar "selecionado" por vez.
     * Quando um é clicado, ele recebe o fundo "selecionado" (rosa) e os
     * outros dois voltam para o fundo "normal" (branco).
     */
    private void configurarBotoesDeTipo() {
        // Lista com os 3 botões para facilitar resetar o visual de todos de uma vez
        List<TextView> botoesDeTipo = Arrays.asList(btnCliente, btnConfeiteira, btnAdmin);

        btnCliente.setOnClickListener(v -> selecionarTipoUsuario(botoesDeTipo, btnCliente, TipoUsuario.CLIENTE));
        btnConfeiteira.setOnClickListener(v -> selecionarTipoUsuario(botoesDeTipo, btnConfeiteira, TipoUsuario.CONFEITEIRA));
        btnAdmin.setOnClickListener(v -> selecionarTipoUsuario(botoesDeTipo, btnAdmin, TipoUsuario.ADMIN));
    }

    // Marca "botaoClicado" como selecionado e os demais da lista como normais
    private void selecionarTipoUsuario(List<TextView> todosOsBotoes, TextView botaoClicado, TipoUsuario tipo) {
        for (TextView botao : todosOsBotoes) {
            boolean estaSelecionado = (botao == botaoClicado);
            botao.setBackgroundResource(
                    estaSelecionado ? R.drawable.bg_toggle_selecionado : R.drawable.bg_toggle_normal
            );
            botao.setTypeface(null, estaSelecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        tipoUsuarioSelecionado = tipo;
        Log.d(TAG, "Tipo de usuário selecionado: " + tipoUsuarioSelecionado);
        atualizarVisibilidadeCriarConta();
    }

    // "Criar conta" só faz sentido para quem ainda não tem cadastro de Cliente;
    // Confeiteira e Admin são perfis criados por dentro da loja, não pelo app.
    private void atualizarVisibilidadeCriarConta() {
        linhaCriarConta.setVisibility(tipoUsuarioSelecionado == TipoUsuario.CLIENTE ? View.VISIBLE : View.GONE);
    }

    // Alterna a senha entre oculta e visível ao tocar no ícone do olho
    private void configurarAlternarVisibilidadeSenha() {
        Typeface fonteOriginal = edtSenha.getTypeface();

        imgAlternarSenha.setOnClickListener(v -> {
            boolean senhaVisivel = edtSenha.getInputType()
                    == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

            if (senhaVisivel) {
                edtSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgAlternarSenha.setImageResource(R.drawable.ic_olho_aberto);
            } else {
                edtSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgAlternarSenha.setImageResource(R.drawable.ic_olho_fechado);
            }

            // setInputType pode resetar a fonte e mover o cursor para o início
            edtSenha.setTypeface(fonteOriginal);
            edtSenha.setSelection(edtSenha.getText().length());
        });
    }

    /**
     * O botão "Entrar" começa desabilitado e só é liberado quando os campos
     * de e-mail E senha estiverem preenchidos. Um único TextWatcher nos dois
     * campos reavalia essa condição a cada letra digitada.
     */
    private void configurarHabilitacaoDoBotaoEntrar() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarEstadoBotaoEntrar();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtEmail.addTextChangedListener(watcher);
        edtSenha.addTextChangedListener(watcher);

        atualizarEstadoBotaoEntrar();
    }

    // Habilita/desabilita o botão "Entrar" e troca seu fundo conforme o estado
    private void atualizarEstadoBotaoEntrar() {
        boolean camposPreenchidos = !TextUtils.isEmpty(edtEmail.getText().toString().trim())
                && !TextUtils.isEmpty(edtSenha.getText().toString().trim());

        btnEntrar.setEnabled(camposPreenchidos);
        btnEntrar.setBackgroundResource(
                camposPreenchidos ? R.drawable.bg_botao_entrar : R.drawable.bg_botao_entrar_desabilitado
        );
    }

    // Configura o clique do botão "Entrar"
    private void configurarBotaoEntrar() {
        btnEntrar.setOnClickListener(v -> realizarLogin());
    }

    // Chama a API para autenticar o usuário com e-mail, senha e perfil selecionados
    private void realizarLogin() {
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();

        btnEntrar.setEnabled(false);

        AuthApiService authApiService = ApiClient.getInstance().create(AuthApiService.class);
        authApiService.login(new LoginRequest(email, senha, tipoUsuarioSelecionado)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Login efetuado: " + response.body().getNome() + " (" + response.body().getPerfil() + ")");
                    abrirTelaInicial(response.body());
                    return;
                }

                atualizarEstadoBotaoEntrar();

                if (response.code() == 401) {
                    Toast.makeText(LoginActivity.this, R.string.login_msg_credenciais_invalidas, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Erro inesperado no login: HTTP " + response.code());
                    Toast.makeText(LoginActivity.this, R.string.login_msg_erro_conexao, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                atualizarEstadoBotaoEntrar();
                Log.e(TAG, "Falha ao chamar a API de login", t);
                Toast.makeText(LoginActivity.this, R.string.login_msg_erro_conexao, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Abre a tela inicial correspondente ao perfil retornado pela API
    private void abrirTelaInicial(LoginResponse resposta) {
        Class<? extends HomeActivity> telaDestino;

        switch (resposta.getPerfil()) {
            case CONFEITEIRA:
                telaDestino = ConfeiteiraHomeActivity.class;
                break;
            case ADMIN:
                telaDestino = AdminHomeActivity.class;
                break;
            case CLIENTE:
            default:
                telaDestino = ClienteHomeActivity.class;
                break;
        }

        Intent intent = new Intent(this, telaDestino);
        intent.putExtra(HomeActivity.EXTRA_NOME_USUARIO, resposta.getNome());
        startActivity(intent);
    }

    // Configura o clique do link "Criar conta"
    private void configurarLinkCriarConta() {
        txtCriarConta.setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroActivity.class);
            startActivity(intent);
        });
    }
}
