package com.montseubolo.app;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base das telas iniciais de cada perfil (Cliente/Confeiteira/Admin).
 * Cada subclasse só precisa informar o título da sua área.
 */
public abstract class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_NOME_USUARIO = "extra_nome_usuario";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        String nome = getIntent().getStringExtra(EXTRA_NOME_USUARIO);

        TextView txtTitulo = findViewById(R.id.txtTituloHome);
        TextView txtMensagem = findViewById(R.id.txtMensagemHome);
        ImageView imgVoltar = findViewById(R.id.imgVoltarHome);

        txtTitulo.setText(getTituloTela());
        txtMensagem.setText(getString(R.string.home_boas_vindas, nome));
        imgVoltar.setOnClickListener(v -> finish());
    }

    protected abstract String getTituloTela();
}
