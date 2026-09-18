package com.montseubolo.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.montseubolo.app.api.ApiClient;
import com.montseubolo.app.api.AuthApiService;
import com.montseubolo.app.api.dto.UsuarioCadastroRequest;
import com.montseubolo.app.api.dto.UsuarioResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela de Cadastro de novos clientes.
 *
 * Valida os campos localmente, garantindo antecedência e consistência
 * (ex.: senhas idênticas, e-mail válido, telefone com DDD), mantendo
 * o botão "Criar conta" desabilitado até preenchimento correto.
 */
public class CadastroActivity extends AppCompatActivity {

    private static final String TAG = "CadastroActivity";

    private EditText edtNome;
    private EditText edtEmail;
    private EditText edtTelefone;
    private EditText edtSenha;
    private EditText edtConfirmarSenha;

    private ImageView imgAlternarSenha;
    private ImageView imgAlternarConfirmarSenha;

    private TextView txtMensagemErro;
    private TextView btnCriarConta;
    private TextView txtEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        ligarViews();
        configurarAlternarVisibilidadeSenhas();
        configurarValidacaoFormulario();
        configurarBotaoCriarConta();
        configurarLinkVoltarAoLogin();
    }

    private void ligarViews() {
        edtNome = findViewById(R.id.edtNome);
        edtEmail = findViewById(R.id.edtEmail);
        edtTelefone = findViewById(R.id.edtTelefone);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);

        imgAlternarSenha = findViewById(R.id.imgAlternarSenha);
        imgAlternarConfirmarSenha = findViewById(R.id.imgAlternarConfirmarSenha);

        txtMensagemErro = findViewById(R.id.txtMensagemErro);
        btnCriarConta = findViewById(R.id.btnCriarConta);
        txtEntrar = findViewById(R.id.txtEntrar);
    }

    private void configurarAlternarVisibilidadeSenhas() {
        configurarOlhoSenha(edtSenha, imgAlternarSenha);
        configurarOlhoSenha(edtConfirmarSenha, imgAlternarConfirmarSenha);
    }

    private void configurarOlhoSenha(EditText campo, ImageView icone) {
        Typeface fonteOriginal = campo.getTypeface();

        icone.setOnClickListener(v -> {
            boolean visivel = campo.getInputType()
                    == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

            if (visivel) {
                campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                icone.setImageResource(R.drawable.ic_olho_aberto);
            } else {
                campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                icone.setImageResource(R.drawable.ic_olho_fechado);
            }

            campo.setTypeface(fonteOriginal);
            campo.setSelection(campo.getText().length());
        });
    }

    private void configurarValidacaoFormulario() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarEstadoBotao();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtNome.addTextChangedListener(watcher);
        edtEmail.addTextChangedListener(watcher);
        edtTelefone.addTextChangedListener(watcher);
        edtSenha.addTextChangedListener(watcher);
        edtConfirmarSenha.addTextChangedListener(watcher);

        atualizarEstadoBotao();
    }

    private void atualizarEstadoBotao() {
        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String telefone = edtTelefone.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();
        String confirmarSenha = edtConfirmarSenha.getText().toString().trim();

        String apenasNumerosTelefone = telefone.replaceAll("[^0-9]", "");

        boolean nomeValido = !TextUtils.isEmpty(nome);
        boolean emailTemArroba = email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1;
        boolean emailValido = !TextUtils.isEmpty(email) && emailTemArroba && Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean telefoneValido = apenasNumerosTelefone.length() >= 11;
        boolean senhaValida = senha.length() >= 6;
        boolean senhasCoincidem = !TextUtils.isEmpty(senha) && senha.equals(confirmarSenha);

        // Feedback visual imediato na tela, igual ao aviso de senhas não coincidentes
        if (!TextUtils.isEmpty(email) && !emailValido) {
            txtMensagemErro.setText(R.string.cadastro_erro_email_invalido);
            txtMensagemErro.setVisibility(View.VISIBLE);
        } else if (!TextUtils.isEmpty(telefone) && !telefoneValido) {
            txtMensagemErro.setText(R.string.cadastro_erro_telefone_invalido);
            txtMensagemErro.setVisibility(View.VISIBLE);
        } else if (!TextUtils.isEmpty(senha) && !TextUtils.isEmpty(confirmarSenha) && !senhasCoincidem) {
            txtMensagemErro.setText(R.string.cadastro_erro_senhas_diferentes);
            txtMensagemErro.setVisibility(View.VISIBLE);
        } else {
            txtMensagemErro.setVisibility(View.GONE);
        }

        boolean formularioValido = nomeValido && emailValido && telefoneValido && senhaValida && senhasCoincidem;

        btnCriarConta.setEnabled(formularioValido);
        btnCriarConta.setBackgroundResource(
                formularioValido ? R.drawable.bg_botao_entrar : R.drawable.bg_botao_entrar_desabilitado
        );
    }

    private void configurarBotaoCriarConta() {
        btnCriarConta.setOnClickListener(v -> submeterCadastro());
    }

    private void submeterCadastro() {
        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String telefone = edtTelefone.getText().toString().trim().replaceAll("[^0-9]", "");
        String senha = edtSenha.getText().toString().trim();

        btnCriarConta.setEnabled(false);
        txtMensagemErro.setVisibility(View.GONE);

        AuthApiService api = ApiClient.getInstance().create(AuthApiService.class);
        UsuarioCadastroRequest request = new UsuarioCadastroRequest(nome, email, telefone, senha);

        api.cadastrar(request).enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CadastroActivity.this, R.string.cadastro_msg_sucesso, Toast.LENGTH_SHORT).show();

                    // Redireciona diretamente para a tela inicial do cliente (Cenário 1)
                    Intent intent = new Intent(CadastroActivity.this, ClienteHomeActivity.class);
                    intent.putExtra(HomeActivity.EXTRA_NOME_USUARIO, response.body().getNome());
                    startActivity(intent);
                    finish();
                    return;
                }

                atualizarEstadoBotao();

                if (response.code() == 409) {
                    String mensagemErro = extrairMensagemErro(response, getString(R.string.cadastro_erro_email_existente));
                    txtMensagemErro.setText(mensagemErro);
                    txtMensagemErro.setVisibility(View.VISIBLE);
                    Toast.makeText(CadastroActivity.this, mensagemErro, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Erro no cadastro: HTTP " + response.code());
                    Toast.makeText(CadastroActivity.this, R.string.login_msg_erro_conexao, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UsuarioResponse> call, Throwable t) {
                atualizarEstadoBotao();
                Log.e(TAG, "Falha na chamada de cadastro", t);
                Toast.makeText(CadastroActivity.this, R.string.login_msg_erro_conexao, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extrairMensagemErro(Response<?> response, String mensagemPadrao) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                JSONObject obj = new JSONObject(json);
                if (obj.has("mensagem")) {
                    return obj.getString("mensagem");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear corpo de erro", e);
        }
        return mensagemPadrao;
    }

    private void configurarLinkVoltarAoLogin() {
        txtEntrar.setOnClickListener(v -> finish());
    }
}
